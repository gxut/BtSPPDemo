package com.android.btsppdemo.client;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.android.btsppdemo.R;

public class DeviceListActivity extends AppCompatActivity {
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private List<String> mDeviceMacAddressList;

    private View mNewDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        initView();
        initData();
    }

    private void initView() {
        mNewDevices = findViewById(R.id.title_new_devices);
        final ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mDeviceMacAddressList = new ArrayList<>();

        // Find and set up the ListView for paired devices
        final ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        if (pairedListView != null) {
            pairedListView.setAdapter(pairedDevicesArrayAdapter);
            pairedListView.setOnItemClickListener(mDeviceClickListener);
        }

        // Find and set up the ListView for newly discovered devices
        final ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        if (newDevicesListView != null) {
            newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
            newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        }

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        // Register for broadcasts when discovery has finished
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        final Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            final View pairedDevicesView = findViewById(R.id.title_paired_devices);
            if (pairedDevicesView != null) {
                pairedDevicesView.setVisibility(View.VISIBLE);
            }
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());  //显示配对设备，名字，MAC地址
            }
        } else {
            String noDevices = getResources().getText(R.string.no_devices_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    private void initData() {
        // Set the CANCELED in case the user backs out.
        setResult(Activity.RESULT_CANCELED);
    }

    public void onClick(View v) {
        doDiscovery();
        v.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        mNewDevices.setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (mDeviceMacAddressList.contains(device.getAddress())) {
                    return;
                } else {
                    //判断类型，如果是标准蓝牙或者是双模，则显示
                    if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                        mDeviceMacAddressList.add(device.getAddress());
                    }

                }
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.no_devices_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}


