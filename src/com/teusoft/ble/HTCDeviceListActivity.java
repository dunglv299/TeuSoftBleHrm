package com.teusoft.ble;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.htc.android.bluetooth.le.gatt.BleAdapter;

public class HTCDeviceListActivity extends Activity {
	private DeviceAdapter deviceAdapter;
	List<BluetoothDevice> deviceList;
	private BluetoothAdapter mBtAdapter;
	private AdapterView.OnItemClickListener mDeviceClickListener;
	private TextView mEmptyList;
	private final BroadcastReceiver mReceiver;

	public HTCDeviceListActivity() {
		this.mDeviceClickListener = new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				mBtAdapter.cancelDiscovery();
				Bundle localBundle = new Bundle();
				String address = ((BluetoothDevice) deviceList.get(paramInt))
						.getAddress();
				localBundle.putString("android.bluetooth.device.extra.DEVICE",
						address);
				Intent localIntent = new Intent();
				localIntent.putExtras(localBundle);
				setResult(Activity.RESULT_OK, localIntent);
				finish();
			}
		};
		this.mReceiver = new BroadcastReceiver() {
			public void onReceive(Context paramContext, Intent paramIntent) {
				String action = paramIntent.getAction();
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice localBluetoothDevice = (BluetoothDevice) paramIntent
							.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
					if (BleAdapter.getDeviceType(localBluetoothDevice) == BleAdapter.DEVICE_TYPE_BLE) {
						addDevice(localBluetoothDevice);
					}
				}
				if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					setProgressBarIndeterminateVisibility(false);
					setTitle(R.string.select_device);
					if (deviceList.size() == 0) {
						mEmptyList.setText(R.string.no_ble_devices);
					}
				}
			}
		};
	}

	private void addDevice(BluetoothDevice paramBluetoothDevice) {
		boolean found = false;
		Iterator<BluetoothDevice> localIterator = deviceList.iterator();
		while (localIterator.hasNext()) {
			if (!((BluetoothDevice) localIterator.next()).getAddress().equals(
					paramBluetoothDevice.getAddress())) {
				continue;
			}
			found = true;
		}
		if (!found) {
			this.mEmptyList.setVisibility(View.GONE);
			this.deviceList.add(paramBluetoothDevice);
			this.deviceAdapter.notifyDataSetChanged();
		}
	}

	private void populateList() {
		deviceList = new ArrayList<BluetoothDevice>();
		deviceAdapter = new DeviceAdapter(this, deviceList);
		ListView localListView = (ListView) findViewById(R.id.new_devices);
		localListView.setAdapter(deviceAdapter);
		localListView.setOnItemClickListener(mDeviceClickListener);
		Iterator<BluetoothDevice> localIterator = this.mBtAdapter
				.getBondedDevices().iterator();
		while (localIterator.hasNext()) {
			BluetoothDevice localBluetoothDevice = (BluetoothDevice) localIterator
					.next();
			addDevice(localBluetoothDevice);
		}
		this.mBtAdapter.startDiscovery();
	}

	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		TextView localTextView = (TextView) findViewById(R.id.empty);
		this.mEmptyList = localTextView;
		Button localButton = (Button) findViewById(R.id.btn_cancel);
		localButton.setOnClickListener(new OnClickListener() {
			public void onClick(View paramView) {
				finish();
			}
		});
	}

	protected void onDestroy() {
		super.onDestroy();
		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}
	}

	public void onStart() {
		super.onStart();
		IntentFilter localIntentFilter = new IntentFilter(
				"android.bluetooth.device.action.FOUND");
		localIntentFilter
				.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
		BroadcastReceiver localBroadcastReceiver = mReceiver;
		registerReceiver(localBroadcastReceiver, localIntentFilter);
		populateList();
	}

	public void onStop() {
		super.onStop();
		BroadcastReceiver localBroadcastReceiver = mReceiver;
		unregisterReceiver(localBroadcastReceiver);
	}

	class DeviceAdapter extends BaseAdapter {
		Context context;
		List<BluetoothDevice> devices;
		LayoutInflater inflater;

		public DeviceAdapter(HTCDeviceListActivity deviceListActivity,
				List<BluetoothDevice> devices) {
			context = deviceListActivity;
			inflater = LayoutInflater.from(deviceListActivity);
			this.devices = devices;
		}

		public int getCount() {
			return devices.size();
		}

		public Object getItem(int paramInt) {
			return devices.get(paramInt);
		}

		public long getItemId(int paramInt) {
			return paramInt;
		}

		public View getView(int paramInt, View paramView,
				ViewGroup paramViewGroup) {
			ViewGroup localViewGroup = null;
			if (paramView != null) {
				localViewGroup = (ViewGroup) paramView;
			} else {
				localViewGroup = (ViewGroup) this.inflater.inflate(
						R.layout.device_element, null);
			}
			BluetoothDevice localBluetoothDevice = (BluetoothDevice) devices
					.get(paramInt);
			TextView textViewAddress = (TextView) localViewGroup
					.findViewById(R.id.address);
			String address = localBluetoothDevice.getAddress();
			textViewAddress.setText(address);
			TextView textViewName = (TextView) localViewGroup
					.findViewById(R.id.name);
			String name = localBluetoothDevice.getName();
			textViewName.setText(name);
			return localViewGroup;
		}
	}
}