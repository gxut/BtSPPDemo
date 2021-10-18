package com.android.btsppdemo.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.btsppdemo.service.ClientService;
import com.android.btsppdemo.service.ConstVar;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.android.btsppdemo.R;

public class BtClientActivity extends AppCompatActivity {
    private static final String TAG = "BtClientActivity";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final String DEFAULT_DATA = "ABCDEFG";
    private BluetoothAdapter mBluetoothAdapter;
    private ClientService mService;
    private String mConnectedDeviceName;
    private TextView mConnectState;
    private TextView mConnectedDevice;
    private EditText mDataToSend;
    private TextView mDataReceived;
    private ScrollView mTestLogScroll;
    private TextView mTestLog;
    private MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_client);
        initView();
        initData();
    }

    private void initView() {
        mConnectState = findViewById(R.id.connect_state);
        mConnectedDevice = findViewById(R.id.connected_device);
        mDataToSend = findViewById(R.id.data_to_send);
        mDataReceived = findViewById(R.id.data_received);
        mTestLogScroll = findViewById(R.id.test_log_scroll);
        mTestLog = findViewById(R.id.test_log);
    }

    private void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
        }
        mHandler = new MyHandler(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (mService == null) {
            mService = new ClientService(this, mHandler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mService != null) {
            if (mService.getState() == ClientService.STATE_NONE) {
                mService.start();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mService != null) {
            mService.stop();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.stop();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.connect:
                final Intent intent = new Intent(v.getContext(), DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                break;
            case R.id.send_clear:
                sendClear();
                break;
            case R.id.send_default:
                sendDefault();
                break;
            case R.id.send:
                sendData();
                break;
            case R.id.recv_clear:
                receiveClear();
                break;
            case R.id.test_log_clear:
                testLogClear();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    final String mMacAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    addTestLog("Connecting with " + mMacAddress);
                    try {
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mMacAddress);
                        mService.connect(device);  //连接对端设备
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                    break;
                }
        }
    }

    private void sendClear() {
        mDataToSend.setText("");
    }

    private void sendDefault() {
        mDataToSend.setText(DEFAULT_DATA);
    }

    private void receiveClear() {
        mDataReceived.setText("");
    }

    private void testLogClear() {
        mTestLog.setText("");
    }

    private void addTestLog(String str) {
        mTestLog.append(str + "\n");
        mTestLogScroll.post(new Runnable() {
            @Override
            public void run() {
                mTestLogScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void sendData() {
        if (mService.getState() != ClientService.STATE_CONNECTED) {
            addTestLog("Not connected");
            return;
        }
        if (mDataToSend.getText().toString().isEmpty()) {
            mDataToSend.setText(DEFAULT_DATA);
        }
        byte[] data = mDataToSend.getText().toString().getBytes();

        MyThread thread = new MyThread(data);
        thread.start();
    }

    private class MyThread extends Thread {
        private byte[] mData;

        public MyThread(byte[] data) {
            mData = Arrays.copyOfRange(data, 0, data.length);
        }

        @Override
        public void run() {
            super.run();
            mService.sendData(mData);
        }
    }

    private static class MyHandler extends Handler {
        WeakReference<Activity> mActivity;

        MyHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final BtClientActivity a = (BtClientActivity) mActivity.get();
            switch (msg.what) {
                case ConstVar.MESSAGE_STATE_CHANGE:
                    Log.e(TAG, "change state");
                    a.showConnectionState(msg.arg1);
                    break;
                case ConstVar.MESSAGE_READ:
                    final int bytes = msg.arg1;
                    final byte[] buf = (byte[]) msg.obj;
                    final String str = new String(buf, 0, bytes);
                    a.mDataReceived.setText(str);
                    break;
                case ConstVar.MESSAGE_DEVICE_NAME:
                    a.mConnectedDeviceName = msg.getData().getString(ConstVar.DEVICE_NAME);
                    Toast.makeText(a.getApplicationContext(), a.getResources().getString(R.string.connect_to) + a.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case ConstVar.MESSAGE_TOAST:
                    Toast.makeText(a.getApplicationContext(), msg.getData().getString(ConstVar.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void showConnectionState(int status) {
        switch (status) {
            case ClientService.STATE_CONNECTED:
                mConnectState.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                mConnectState.setText(R.string.connected);
                mConnectedDevice.setText(mConnectedDeviceName);
                break;
            case ClientService.STATE_CONNECTING:
                mConnectState.setText(R.string.connecting);
                mConnectedDevice.setText("");
                break;

            case ClientService.STATE_DISCONNECTING:
            case ClientService.STATE_LISTEN:
            case ClientService.STATE_NONE:
                Log.e(TAG, "state change to " + status);
                mConnectState.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                mConnectState.setText(R.string.disconnected);
                mConnectedDevice.setText("");
                break;
            default:
                break;
        }
    }
}
