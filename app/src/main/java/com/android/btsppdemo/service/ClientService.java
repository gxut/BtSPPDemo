package com.android.btsppdemo.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.btsppdemo.R;

import com.android.btsppdemo.PreferenceSettings;

import java.io.IOException;
import java.util.UUID;

/**
 * Service of bluetooth client.
 */
public class ClientService extends ConnectedService {
    private static final String TAG = "ClientService";
    private UUID mUUID;
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Context mContext;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public ClientService(Context context, Handler handler) {
        super(handler);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String uuid = sharedPref.getString(context.getString(R.string.uuid_client), context.getResources().getString(R.string.default_uuid));
        mUUID = UUID.fromString(uuid);
        Log.e(TAG, "client uuid = " + uuid);
        mContext = context;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * 启动ConnectThread线程连接对端设备
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        final boolean secure = PreferenceSettings.getSecurity(mContext);
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(ConstVar.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ConstVar.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void sendData(byte[] data) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.sendData(data);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        Log.e(TAG, "connectionFailed");
        setState(STATE_LISTEN);

        Message msg = mHandler.obtainMessage(ConstVar.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ConstVar.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     * <p>
     * 主动连接其他设备，连接之前向通过createRfcommSocketToServiceRecord或createInsecureRfcommSocketToServiceRecord创建一个bluetoothsocket
     * 在通过该socket进行连接
     */
    protected class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                if (secure) {
                    try {
                        tmp = device.createRfcommSocketToServiceRecord(mUUID);
                    } catch (Exception e2) {
                        tmp = device.createRfcommSocketToServiceRecord(mUUID);
                    }
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(mUUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            mAdapter.cancelDiscovery();

            try {
                Log.e(TAG, "mmSocket connect");
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "mmSocket connect IOException");

                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                ClientService.this.start();
                return;
            }

            synchronized (ClientService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


}
