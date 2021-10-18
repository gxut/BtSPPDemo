package com.android.btsppdemo.server;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.btsppdemo.R;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.btsppdemo.service.ConstVar;
import com.android.btsppdemo.service.ServerService;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class BtServerActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String DEFAULT_DATA = "ABCDEFG";
    private static final String TAG = "BtServerActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private ServerService mService;
    private String mConnectedDeviceName;
    private TextView mConnectState;
    private TextView mConnectedDevice;
    private EditText mDataToSend;
    private TextView mDataReceived;
    private ScrollView mTestLogScroll;
    private TextView mTestLog;
    private Button mSetVisibleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_server);
        initView();
        initData();
    }

    private void initView() {
        mSetVisibleBtn = findViewById(R.id.set_bt_visible);
        mSetVisibleBtn.setOnClickListener(this);
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
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
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
            mService = new ServerService(this, mHandler);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mService != null) {
            if (mService.getState() == ServerService.STATE_NONE) {
                mService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.stop();
        }
    }

    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.set_bt_visible:
                setBtVisible();
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
        if (mService.getState() != ServerService.STATE_CONNECTED) {
            addTestLog("Not connected");
            return;
        }

        if (mDataToSend.getText().toString().isEmpty()) {
            mDataToSend.setText(DEFAULT_DATA);
        }
        byte[] data = mDataToSend.getText().toString().getBytes();

        // In order to show UI, create a thread to send large amount of data
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

    private void showConnectionState(int status) {
        android.util.Log.e(TAG, "status=" + status);
        switch (status) {
            case ServerService.STATE_LISTEN:
            case ServerService.STATE_DISCONNECTING://add 2019.3.30
            case ServerService.STATE_NONE:
                mConnectState.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                mConnectState.setText(R.string.disconnected);
                mConnectedDevice.setText("");
                break;
            case ServerService.STATE_CONNECTED:
                mConnectState.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                mConnectState.setText(R.string.connected);
                mConnectedDevice.setText(mConnectedDeviceName);
                break;
            case ServerService.STATE_CONNECTING:
                mConnectState.setText(R.string.connecting);
                mConnectedDevice.setText("");
                break;
            default:
                break;
        }
    }

    private MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        WeakReference<Activity> mActivity;

        MyHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final BtServerActivity a = (BtServerActivity) mActivity.get();
            switch (msg.what) {
                case ConstVar.MESSAGE_STATE_CHANGE:
                    a.showConnectionState(msg.arg1);
                    break;
                case ConstVar.MESSAGE_READ:
                    final int bytes = msg.arg1;
                    final byte[] buf = (byte[]) msg.obj;
                    // show data received.
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

    private void setBtVisible() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 500);
            startActivity(discoverableIntent);
        } else {
            Toast.makeText(this, "Already discoverable", Toast.LENGTH_SHORT).show();
        }
    }
}