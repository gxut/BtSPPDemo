package com.android.btsppdemo.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
import static java.lang.Boolean.TRUE;

/**
 * Service of bluetooth server.
 */
public class ServerService extends ConnectedService {
    private static final String TAG = "ServerService";
    private static final String NAME = "BluetoothChat";
    private UUID mUUID;
    private final BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;
    private Context mContext;

    private boolean acceptThreadRunning = false;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public ServerService(Context context, Handler handler) {
        super(handler);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mUUID = UUID.fromString(context.getResources().getString(R.string.default_uuid));
        Log.e(TAG, "server uuid = " + mUUID);
        mContext = context;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        if (mConnectedThread != null) {
            Log.e(TAG, "Cancel connected thread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            Log.e(TAG, "Start accept thread");
            acceptThreadRunning = TRUE;
            mAcceptThread.start();
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
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
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
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
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                final boolean secure = PreferenceSettings.getSecurity(mContext);
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, mUUID);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, mUUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            Log.e(TAG, "AcceptThread");
            BluetoothSocket socket = null;
            while (acceptThreadRunning) {
                if (mState == STATE_CONNECTED) {
                    try {
                        Thread.sleep(500, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                if (mState == STATE_DISCONNECTING) {
                    try {
                        mmServerSocket.close();
                        mmServerSocket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    BluetoothServerSocket tmp = null;
                    try {
                        final boolean secure = PreferenceSettings.getSecurity(mContext);
                        if (secure) {
                            tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, mUUID);
                        } else {
                            tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, mUUID);
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "listen() failed", e);
                    }
                    mmServerSocket = tmp;
                    mState = STATE_LISTEN;
                }
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                Log.e(TAG, "before check socket");
                if (socket != null) {
                    Log.e(TAG, "before Sync");
                    synchronized (ServerService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                                Log.e(TAG, "listen");
                            case STATE_CONNECTING:
                                Log.e(TAG, "connecting");
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                                Log.e(TAG, "state none");
                            case STATE_CONNECTED:
                                Log.e(TAG, "connected");
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                            default:
                                Log.e(TAG, "default");
                                break;
                        }
                    }
                }
            }
            Log.e(TAG, "exit accept thread");
        }

        public void cancel() {
            try {
                acceptThreadRunning = false;
                mmServerSocket.close();
                mmServerSocket = null;
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
}
