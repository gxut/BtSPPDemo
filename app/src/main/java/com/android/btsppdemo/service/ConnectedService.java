package com.android.btsppdemo.service;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedService {
    private static final String TAG = "ConnectedService";
    protected Handler mHandler;
    protected int mState;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_DISCONNECTING = 4;

    public ConnectedService(Handler handler) {
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    protected synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        mHandler.obtainMessage(ConstVar.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Log.e(TAG, "connectionLost");
        setState(STATE_DISCONNECTING);

        Message msg = mHandler.obtainMessage(ConstVar.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ConstVar.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    protected class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean mThreadStoping;
        private boolean mThreadStoped;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.e(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while (!mThreadStoping) {
                try {
                    if (mmInStream.available() != 0) {
                        Log.e(TAG, "Disable data check: " + mmInStream.available());
                        bytes = mmInStream.read(buffer);
                        mHandler.obtainMessage(ConstVar.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    try {
                        mmSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    connectionLost();
                    break;
                }
            }

            mThreadStoped = true;
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                Log.e(TAG, "initiative writed:" + buffer.length);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void sendData(byte[] data) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            write(data);
        }

        public void cancel() {
            try {
                mThreadStoping = true;
                while (mThreadStoped != true) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mmInStream != null)
                    mmInStream.close();
                if (mmOutStream != null)
                    mmOutStream.close();
                if (mmSocket != null)
                    mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
