package com.brige.blutooth.normal;

/**
 * Created by Rth on 2015/4/30.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 *客户端连接
 */
public class BluetoothClient extends Thread {

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String TAG = getClass().getSimpleName();

    private BluetoothClientListener listener;

    public BluetoothClient(BluetoothDevice device,UUID uuid,BluetoothClientListener listener) {
        this.listener = listener;
        mmDevice = device;
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(
                    uuid);
        } catch (IOException e) {
            Log.e(TAG, "create() from a device  failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        Log.i(TAG, "BEGIN a socket connect:");
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        try {
            mmSocket.connect();
        } catch (IOException e) {
            try {
                mmSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() a device socket " , e2);
            }
            listener.clientConnectFailed();
            return;
        }
        listener.clientConnect(mmSocket,mmDevice);
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect（device)  socket failed", e);
        }
    }

    public interface BluetoothClientListener{
        void clientConnect(BluetoothSocket socket,BluetoothDevice device);
        void clientConnectFailed();
    }
}
