package com.hardcopy.blechat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;

import com.hardcopy.blechat.fragments.ExampleFragment;
import com.hardcopy.blechat.fragments.FragmentAdapter;
import com.hardcopy.blechat.fragments.IFragmentListener;
import com.hardcopy.blechat.service.BTCTemplateService;
import com.hardcopy.blechat.utils.AppSettings;
import com.hardcopy.blechat.utils.Constants;
import com.hardcopy.blechat.utils.Logs;
import com.hardcopy.blechat.utils.RecycleUtils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements IFragmentListener {
	private int csvNumber = 1;
	private StringBuffer csvStorage;
	private String msgCollection = "";

	// Debugging
	private static final String TAG = "BLEChatActivity";

	// Context, System
	private Context mContext;
	private BTCTemplateService mService;
	private ActivityHandler mActivityHandler;

	// UI stuff
	private FragmentManager mFragmentManager;
	private FragmentAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;

	private ImageView mImageBT = null;
	private TextView mTextStatus = null;

	// Refresh timer
	private Timer mRefreshTimer = null;


	/*****************************************************
	 *    Overrided methods
	 ******************************************************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//----- System, Context
		mContext = this;   //.getApplicationContext();
		mActivityHandler = new ActivityHandler();
		AppSettings.initializeAppSettings(mContext);

		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the primary sections of the app.
		mFragmentManager = getSupportFragmentManager();
		mSectionsPagerAdapter = new FragmentAdapter(mFragmentManager, mContext, this, mActivityHandler);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// Setup views
		mImageBT = (ImageView) findViewById(R.id.status_title);
		mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
		mTextStatus = (TextView) findViewById(R.id.status_text);
		mTextStatus.setText(getResources().getString(R.string.bt_state_init));

		csvStorage = new StringBuffer();

		// Do data initialization after service started and binded
		doStartService();
	}


	@Override
	public synchronized void onStart() {
		super.onStart();
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		// Stop the timer
		if (mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer = null;
		}
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		finalizeActivity();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		// onDestroy is not always called when applications are finished by Android system.
		finalizeActivity();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_scan:
				// Launch the DeviceListActivity to see devices and do scan
				doScan();
				return true;
			case R.id.action_discoverable:
				// Ensure this device is discoverable by others
				ensureDiscoverable();
				return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();      // TODO: Disable this line to run below code
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void OnFragmentCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
		switch (msgType) {
			case IFragmentListener.CALLBACK_RUN_IN_BACKGROUND:
				if (mService != null)
					mService.startServiceMonitoring();
				break;
			case IFragmentListener.CALLBACK_SEND_MESSAGE:
				if (mService != null && arg2 != null)
					mService.sendMessageToRemote(arg2);

			default:
				break;
		}
	}


	/*****************************************************
	 *   Private methods
	 ******************************************************/

	/**
	 * Service connection
	 */
	private ServiceConnection mServiceConn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, "Activity - Service connected");

			mService = ((BTCTemplateService.ServiceBinder) binder).getService();

			// Activity couldn't work with mService until connections are made
			// So initialize parameters and settings here. Do not initialize while running onCreate()
			initialize();
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	/**
	 * Start service if it's not running
	 */
	private void doStartService() {
		Log.d(TAG, "# Activity - doStartService()");
		startService(new Intent(this, BTCTemplateService.class));
		bindService(new Intent(this, BTCTemplateService.class), mServiceConn, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Stop the service
	 */
	private void doStopService() {
		Log.d(TAG, "# Activity - doStopService()");
		mService.finalizeService();
		stopService(new Intent(this, BTCTemplateService.class));
	}

	/**
	 * Initialization / Finalization
	 */
	private void initialize() {
		Logs.d(TAG, "# Activity - initialize()");

		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.bt_ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}

		mService.setupService(mActivityHandler);

		// If BT is not on, request that it be enabled.
		// RetroWatchService.setupBT() will then be called during onActivityResult
		if (!mService.isBluetoothEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
		}

		// Load activity reports and display
		if (mRefreshTimer != null) {
			mRefreshTimer.cancel();
		}

		// Use below timer if you want scheduled job
		//mRefreshTimer = new Timer();
		//mRefreshTimer.schedule(new RefreshTimerTask(), 5*1000);
	}

	private void finalizeActivity() {
		Logs.d(TAG, "# Activity - finalizeActivity()");

		if (!AppSettings.getBgService()) {
			doStopService();
		} else {
		}

		// Clean used resources
		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
		System.gc();
	}

	/**
	 * Launch the DeviceListActivity to see devices and do scan
	 */
	private void doScan() {
		Intent intent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
	}

	/**
	 * Ensure this device is discoverable by others
	 */
	private void ensureDiscoverable() {
		if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(intent);
		}
	}


	/*****************************************************
	 *   Public classes
	 ******************************************************/

	/**
	 * Receives result from external activity
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logs.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {
			case Constants.REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					// Get the device MAC address
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					// Attempt to connect to the device
					if (address != null && mService != null)
						mService.connectDevice(address);
				}
				break;

			case Constants.REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a BT session
					mService.setupBLE();
				} else {
					// User did not enable Bluetooth or an error occured
					Logs.e(TAG, "BT is not enabled");
					Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				}
				break;
		}   // End of switch(requestCode)
	}


	/*****************************************************
	 *   Handler, Callback, Sub-classes
	 ******************************************************/

	public class ActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				// Receives BT state messages from service
				// and updates BT state UI
				case Constants.MESSAGE_BT_STATE_INITIALIZED:
					mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + getResources().getString(R.string.bt_state_init));
					mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
					break;
				case Constants.MESSAGE_BT_STATE_LISTENING:
					mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + getResources().getString(R.string.bt_state_wait));
					mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_invisible));
					break;
				case Constants.MESSAGE_BT_STATE_CONNECTING:
					mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + getResources().getString(R.string.bt_state_connect));
					mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_away));
					break;
				case Constants.MESSAGE_BT_STATE_CONNECTED:
					if (mService != null) {
						String deviceName = mService.getDeviceName();
						if (deviceName != null)
							mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + getResources().getString(R.string.bt_state_connected) + " " + deviceName);
						else
							mTextStatus.setText(getResources().getString(R.string.bt_title) + ": " + getResources().getString(R.string.bt_state_connected) + " no name");
						mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_online));
					}
					break;
				case Constants.MESSAGE_BT_STATE_ERROR:
					mTextStatus.setText(getResources().getString(R.string.bt_state_error));
					mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_busy));
					break;

				// BT Command status
				case Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED:
					mTextStatus.setText(getResources().getString(R.string.bt_cmd_sending_error));
					mImageBT.setImageDrawable(getResources().getDrawable(android.R.drawable.presence_busy));
					break;

				///////////////////////////////////////////////
				// When there's incoming packets on bluetooth
				// do the UI works like below
				///////////////////////////////////////////////
				case Constants.MESSAGE_READ_CHAT_DATA:
					if (msg.obj != null) {
						msgCollection += (String) msg.obj;
						if (msgCollection.contains("\r\n")) {
							msgCollection = msgCollection.replace("\r\n", "");

							ExampleFragment frg = (ExampleFragment) mSectionsPagerAdapter.getItem(FragmentAdapter.FRAGMENT_POS_EXAMPLE);

							if (msgCollection.equals("Setting") || msgCollection.equals("DONE"))
								frg.showMessage(msgCollection);

							else if (msgCollection.equals("reset")) {
								saveFile(csvStorage.toString(), csvNumber++);
								csvStorage.setLength(0);
							} else {
								csvStorage.append(msgCollection);
							}
							msgCollection = "";
						}
					}
					break;

				default:
					break;
			}

			super.handleMessage(msg);
		}
	}   // End of class ActivityHandler

	private void saveFile(String message, int num) {
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		String filePath = "wearable";
		File file = new File(path, filePath);

		String title = "TimeStamp,ax,ay,az,gx,gy,gz,mx,my,mz\n";

		file.mkdirs();

		String tempFile = "temp" + String.valueOf(num) + ".csv";
		String fileName = file.getPath().toString() + "/" + tempFile;

		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			fos.write(title.getBytes());
			fos.write(message.getBytes());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Toast.makeText(this, fileName + "파일 저장 완료", Toast.LENGTH_SHORT).show();
	}
}