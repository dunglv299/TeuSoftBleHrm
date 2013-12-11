package com.teusoft.ble;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.android.bluetooth.le.profile.hrm.BluetoothHrmClient;
import com.htc.android.bluetooth.le.profile.hrm.BluetoothHrmService;

public class HTCActivity extends BaseActivity {

	private String TAG = "HTCActivity";
	private boolean appRegistered = false;
	private boolean mIsConnected = false;
	private BluetoothHrmClient mHeartRateMonitor = null;
	private BluetoothDevice mDevice = null;
	private TextView mConnectionStatus;
	private TextView mHeartrate;
	TextView mCurrent;
	TextView mTarget;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.htc_main);

		mConnectionStatus = (TextView) findViewById(R.id.con_status);
		mHeartrate = (TextView) findViewById(R.id.heartrate);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothHrmClient.HRM_REGISTERED);
		intentFilter.addAction(BluetoothHrmClient.HRM_CONNECTED);
		intentFilter.addAction(BluetoothHrmClient.HRM_DISCONNECTED);
		intentFilter.addAction(BluetoothHrmService.HRM_MEASUREMENT);
		intentFilter.addAction(BluetoothHrmService.HRM_BODY_SENSOR_LOCATION);
		registerReceiver(regReceiver, intentFilter);
		mHeartRateMonitor = new BluetoothHrmClient(this);

		mCurrent = (TextView) findViewById(R.id.current_tv);
		mTarget = (TextView) findViewById(R.id.target_tv);

	}

	private final BroadcastReceiver regReceiver = new BroadcastReceiver() {
		public void onReceive(final Context context, final Intent intent) {

			if (intent.getAction().equals(BluetoothHrmClient.HRM_REGISTERED)) {
				Log.d(TAG, "Received app Register Intent");
				appRegistered = true;
			} else if (intent.getAction().equals(
					BluetoothHrmClient.HRM_CONNECTED)) {
				mConnectionStatus.setTextColor(Color.BLUE);
				mConnectionStatus.setText("Connected.");
				mIsConnected = true;
			} else if (intent.getAction().equals(
					BluetoothHrmService.HRM_MEASUREMENT)) {
				int hrmMeasurementValue = intent.getIntExtra(
						BluetoothHrmService.EXTRA_HRMVALUE, Integer.MIN_VALUE);
				int energyExpendedValue = intent.getIntExtra(
						BluetoothHrmService.EXTRA_ENERGY_EXPENDED,
						Integer.MIN_VALUE);
				int[] rrIntervalValue = intent
						.getIntArrayExtra(BluetoothHrmService.EXTRA_RR_INTERVAL);
				int sensorContactStatus = intent.getIntExtra(
						BluetoothHrmService.EXTRA_HRMSENSORCONTACTSTATUS,
						Integer.MIN_VALUE);

				Log.e(TAG, " HRMVALUE = " + hrmMeasurementValue
						+ "  ENERGY_EXPENDED= " + energyExpendedValue
						+ "  RR_INTERVAL= " + rrIntervalValue[0]
						+ "  HRMSENSORCONTACTSTATUS = " + sensorContactStatus);

				if (rrIntervalValue != null) {
					StringBuffer stringBuffer = new StringBuffer();
					for (int i = 0; i < rrIntervalValue.length; i++) {
						if (rrIntervalValue[i] != -1) { // invalid RR interval
														// value
							stringBuffer
									.append((rrIntervalValue[i] * 1000) / 1024);
							stringBuffer.append("  ");
						} else {
							stringBuffer.append("  ");
							break;
						}
					}
				}
				updateHeartPulse(String.valueOf(hrmMeasurementValue));
			} else if (intent.getAction().equals(
					BluetoothHrmClient.HRM_DISCONNECTED)) {
				mConnectionStatus.setTextColor(Color.RED);
				mConnectionStatus.setText("Disconnected.");
				mIsConnected = false;
			}
		}
	};

	private void updateHeartPulse(final String percentage) {
		runOnUiThread(new Runnable() {
			public void run() {
				mHeartrate.setTextSize(80);
				// setDataTemperature(Integer.getInteger(percentage));
				// if (percentage.length() < 3) {
				// mHeartrate.setText(" " + percentage);
				// } else {
				// mHeartrate.setText(percentage);
				// }
			}
		});

		// Timer timer = new Timer();
		// timer.schedule(new TimerTask() {
		// public void run() {
		// updateHeartRateWithDefault(percentage);
		// }
		// }, 1000);
		updateHeartRateWithDefault(percentage);

	}

	private void updateHeartRateWithDefault(final String percentage) {
		runOnUiThread(new Runnable() {
			public void run() {
				int value = Integer.parseInt(percentage);
				setDataTemperature(value);
				mHeartrate.setTextSize(80);
				if (percentage.length() < 3) {
					mHeartrate.setText(" " + percentage);
				} else {
					mHeartrate.setText(percentage);
				}
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode != Activity.RESULT_OK) {
				finish();
			} else {
				mBtAdapter.startDiscovery();
			}
		} else if (requestCode == REQUEST_SELECT_DEVICE) {
			if (resultCode == Activity.RESULT_OK && data != null
					&& appRegistered == true) {
				String deviceAddress = data
						.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
						deviceAddress);
				mHeartRateMonitor.connectBackground(mDevice);
			} else {
				Toast.makeText(this, "failed to select the device - try again",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	public void onDestroy() {
		try {
			unregisterReceiver(regReceiver);
		} catch (Exception ignore) {
		}

		if (mHeartRateMonitor != null && mDevice != null) {
			mHeartRateMonitor.cancelBackgroundConnection(mDevice);
			try {
				mHeartRateMonitor.deregister();
			} catch (InterruptedException ignored) {
			}

		}
		if (mHeartRateMonitor != null) {
			mHeartRateMonitor.finish();
		}
		super.onDestroy();
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

	@Override
	public void onBackPressed() {
		// if (mState == STATE_READY) {
		// Intent startMain = new Intent(Intent.ACTION_MAIN);
		// startMain.addCategory(Intent.CATEGORY_HOME);
		// startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// startActivity(startMain);
		// } else {
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
						}).setNegativeButton(R.string.popup_no, null).show();
		// }
	}

}
