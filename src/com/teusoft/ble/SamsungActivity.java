/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teusoft.ble;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SamsungActivity extends BaseActivity implements OnClickListener {

	private static final int STATE_READY = 10;
	public static final String TAG = "HRPCollector";
	private static final int HRP_PROFILE_CONNECTED = 20;
	private static final int HRP_PROFILE_DISCONNECTED = 21;
	private static final int STATE_OFF = 10;
	private static final int WAITING_TIME = 1500;

	public int mState = HRP_PROFILE_DISCONNECTED;

	private HRPService mService = null;
	private BluetoothDevice mDevice = null;
	Button mConnect;
	private TextView mCurrent;
	private TextView mTarget;
	private TextView mConnectionStatus;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.samsung_main);
		init();
		mConnectionStatus = (TextView) findViewById(R.id.con_status);
		mConnect = (Button) findViewById(R.id.btn_connect_or_disconnect);
		mConnect.setVisibility(View.INVISIBLE);
		mConnect.setOnClickListener(this);
		mCurrent = (TextView) findViewById(R.id.current_tv);
		mTarget = (TextView) findViewById(R.id.target_tv);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_connect_or_disconnect:
			if (mState == STATE_READY || (mState == HRP_PROFILE_CONNECTED)) {
				mService.disableNotification(mDevice);// disable HR
														// notifications,
														// if
														// enabled
				mService.disconnect(mDevice);
			} else if (mState == HRP_PROFILE_DISCONNECTED) {
				mService.connect(mDevice, false);
			}
			break;
		default:
			break;
		}
	}

	private BroadcastReceiver deviceStateListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			final Intent mIntent = intent;
			if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				BluetoothDevice device = mIntent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int devState = mIntent.getIntExtra(
						BluetoothDevice.EXTRA_BOND_STATE, -1);
				Log.d(TAG, "BluetoothDevice.ACTION_BOND_STATE_CHANGED");
				setUiState();
				if (device.equals(mDevice)
						&& devState == BluetoothDevice.BOND_NONE) {
					runOnUiThread(new Runnable() {
						public void run() {
							mDevice = null;
							setUiState();
						}
					});
				}
			}
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = mIntent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED" + "state is"
						+ state);
				runOnUiThread(new Runnable() {
					public void run() {
						if (state == STATE_OFF) {
							mDevice = null;
							mState = HRP_PROFILE_DISCONNECTED;
							setUiStateForBTOff();
						}
					}
				});
			}
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder rawBinder) {
			mService = ((HRPService.LocalBinder) rawBinder).getService();
			Log.d(TAG, "onServiceConnected mService= " + mService);
			mService.setActivityHandler(mHandler);
		}

		public void onServiceDisconnected(ComponentName classname) {
			mService = null;
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HRPService.HRP_CONNECT_MSG:
				Log.d(TAG, "mHandler.HRP_CONNECT_MSG");
				runOnUiThread(new Runnable() {
					public void run() {
						mState = HRP_PROFILE_CONNECTED;
						setUiState();
					}
				});
				break;

			case HRPService.HRP_DISCONNECT_MSG:
				Log.d(TAG, "mHandler.HRP_DISCONNECT_MSG");
				runOnUiThread(new Runnable() {
					public void run() {
						mState = HRP_PROFILE_DISCONNECTED;
						setUiState();
					}
				});
				break;

			case HRPService.HRP_READY_MSG:
				Log.d(TAG, "mHandler.HRP_READY_MSG");
				runOnUiThread(new Runnable() {
					public void run() {
						mState = STATE_READY;
						setUiState();
					}
				});

			case HRPService.HRP_VALUE_MSG:
				Log.d(TAG, "mHandler.HRP_VALUE_MSG");
				Bundle data1 = msg.getData();
				final int hrmval = data1.getInt(HRPService.HRM_VALUE, 0);
				runOnUiThread(new Runnable() {
					public void run() {
						if (hrmval >= 0) {
							setDataTemperature(hrmval);
							Log.e("HRMVAL", hrmval + "");
						}
					}

				});

			default:
				super.handleMessage(msg);
			}
		}
	};

	private void init() {
		Log.d(TAG, "init() mService= " + mService);
		Intent bindIntent = new Intent(this, HRPService.class);
		startService(bindIntent);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(deviceStateListener, filter);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart() mService= " + mService);
	}

	@Override
	public void onDestroy() {
		unbindService(mServiceConnection);
		unregisterReceiver(deviceStateListener);
		stopService(new Intent(this, HRPService.class));
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

		case REQUEST_SELECT_DEVICE:
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data
						.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
						deviceAddress);
				Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
						+ "mserviceValue" + mService);
				updateUi();
				mService.connect(mDevice, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Bluetooth has turned on ",
						Toast.LENGTH_SHORT).show();

			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, "Problem in BT Turning ON ",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		default:
			Log.e(TAG, "wrong requst Code");
			break;
		}
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		if (!mBtAdapter.isEnabled()) {
			Log.i(TAG, "onResume - BT not enabled yet");
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		updateUi();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private void updateUi() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setUiState();
			}
		});
	}

	private void setUiState() {
		Log.e(TAG, "... setUiState.mState" + mState);
		// mData.setEnabled(mState == STATE_READY);
		if (mState == STATE_READY) {
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mService.enableHRNotification(mDevice);
				}
			}, WAITING_TIME);
			// mService.enableHRNotification(mDevice);
		}
		switch (mState) {
		case HRP_PROFILE_CONNECTED:
			mConnectionStatus.setTextColor(Color.BLUE);
			mConnectionStatus.setText(R.string.connected);
			Log.i(TAG, "STATE_CONNECTED::device name" + mDevice.getName());
			mConnect.setVisibility(View.VISIBLE);
			mConnect.setText(R.string.disconnected);
			mConnect.setEnabled(false);
			break;
		case HRP_PROFILE_DISCONNECTED:
			mConnectionStatus.setTextColor(Color.RED);
			mConnectionStatus.setText(R.string.disconnected);
			Log.i(TAG, "disconnected");
			mConnect.setText(R.string.connect);
			mConnect.setEnabled(true);
			mCurrent.setText("");
			mTarget.setText("");
			break;
		case STATE_READY:
			mConnect.setText(R.string.disconnect);
			mConnect.setEnabled(mDevice != null);
			break;
		default:
			Log.e(TAG, "wrong mState");
			break;
		}
	}

	private void setUiStateForBTOff() {
		mConnect.setEnabled(false);
	}

	@Override
	public void onBackPressed() {
		if (mState == STATE_READY) {
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		} else {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.popup_title)
					.setMessage(R.string.popup_message)
					.setPositiveButton(R.string.popup_yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).setNegativeButton(R.string.popup_no, null)
					.show();
		}
	}

	public void setDataTemperature(int value) {

		if (value < 1000) {
			mCurrent.setText(value + "");
		} else if (value >= 1000 && value < 2000) {
			mTarget.setText(value - 1000 + "");
		} else if (value >= 2000 && value < 3000) {
			mCurrent.setText(value - 2000 + "");
		} else if (value >= 3000 && value != 4000) {
			mTarget.setText(value - 3000 + "");
		}
	}

}
