package com.brige.blutooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Rth on 2015/4/30.
 */
public class BlutoothServer extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private final Handler handler;
    private final BluetoothAdapter mAdapter;
    private final String TAG = getClass().getSimpleName();

    private boolean isListening = false;
    private BluetoothServerConnectListener listener;

    public BlutoothServer(Handler han,String name,UUID uuid,BluetoothServerConnectListener listener) {
        this.listener = listener;
        handler = han;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket tmp = null;
        try {
            tmp = mAdapter.listenUsingRfcommWithServiceRecord(name,
                    uuid);
        } catch (IOException e) {
            Log.e(TAG, "listen() failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        Log.e(TAG,"begin server listen");
        BluetoothSocket socket = null;
        isListening = true;
        while (isListening) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG,  "accept() failed", e);
                listener.serverConnectFailed();
                break;
            }
            isListening = false;
            if (socket != null) {
                if(listener.serverConnect(socket,socket.getRemoteDevice())) {
                    //there is a connect before this connect,close this connect
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close unwanted socket", e);
                    }
                }
            }
        }
        Log.i(TAG, "END mAcceptThread ");
    }

    public void cancel() {
        Log.e(TAG, "cancel " + this);
        try {
            mmServerSocket.close();
            isListening = false;
        } catch (IOException e) {
            Log.e(TAG, "close() of server failed", e);
        }
    }

    public interface BluetoothServerConnectListener {
        boolean serverConnect(BluetoothSocket socket,BluetoothDevice device);
        void serverConnectFailed();
    }
}
