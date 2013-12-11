package com.teusoft.ble;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.teusoft.ble.constant.Constant;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// String s = "Debug-infos:";
		// s += "\n OS Version: " + System.getProperty("os.version") + "("
		// + android.os.Build.VERSION.INCREMENTAL + ")";
		// s += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
		// s += "\n Device: " + android.os.Build.DEVICE;
		// s += "\n Model (and Product): " + android.os.Build.MODEL + " ("
		// + android.os.Build.PRODUCT + ")";
		String manufacturer = Build.MANUFACTURER.toLowerCase();
		Log.e("manufacturer", manufacturer);
		if (manufacturer.contains(Constant.HTC_MANUFACTURER)) {
			Intent intent = new Intent(this, HTCActivity.class);
			intent.putExtra("manufacturer", manufacturer);
			startActivity(intent);
		} else if (manufacturer.contains(Constant.SAMSUNG_MANUFACTURER)) {
			Intent intent = new Intent(this, SamsungActivity.class);
			intent.putExtra("manufacturer", manufacturer);
			startActivity(intent);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		finish();
	}
}
