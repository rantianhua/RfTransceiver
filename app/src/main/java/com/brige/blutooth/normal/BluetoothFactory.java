package com.brige.blutooth.normal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.rftransceiver.util.Constants;
import com.source.parse.ParseFactory;

import java.util.UUID;

/**
* Created by Rth on 2015/4/25.
*/
public class BluetoothFactory implements BlutoothServer.BluetoothServerConnectListener
    ,BluetoothClient.BluetoothClientListener{

    private static final String TAG = "BluetoothSever";

    //the uuid and bluetoothserver name
    private static final String NAME = TAG;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private BlutoothServer serverThread;
    private BluetoothClient clientThread;
    private WorkThread workThread;
    private BluetoothState mState;

    private ParseFactory parseFactory;


    public enum BluetoothState {
        STATE_NONE, // no connection
        STATE_LISTENING,     // listening
        STATE_CONNECTING, // connecting
        STATE_CONNECTED,    //have connected a device or server
    }

    public BluetoothFactory(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = BluetoothState.STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(BluetoothState state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(Constants.MSG_WHAT_STATE, state.ordinal(), -1).sendToTarget();
    }

    /**
     * return the state of current bluetooth
     */
    public synchronized BluetoothState getState() {
        return mState;
    }

    /**
     * start server to listening
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // close all the other threads
        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        setState(BluetoothState.STATE_LISTENING);

        if (serverThread == null) {
            serverThread = new BlutoothServer(mHandler,NAME,MY_UUID,this);
            serverThread.start();
        }
    }

    /**
     * 用已有的蓝牙设备建立远程连接
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        if(serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        if (mState == BluetoothState.STATE_CONNECTING) {
            if (clientThread != null) {
                clientThread.cancel();
                clientThread = null;
            }
        }

        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        clientThread = new BluetoothClient(device,MY_UUID,this);
        clientThread.start();

        setState(BluetoothState.STATE_CONNECTING);
    }

    //manage the connected socket
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {

        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        if(serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        workThread = new WorkThread(socket,mHandler);
        workThread.setParseFactory(parseFactory);
        workThread.start();

        Message msg = mHandler.obtainMessage(Constants.MSG_WHAT_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(BluetoothState.STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        setState(BluetoothState.STATE_NONE);
    }

    /**
     * send data to socket
     */
    public void write(byte[] out,boolean end) {
        WorkThread r;
        synchronized (this) {
            if (mState != BluetoothState.STATE_CONNECTED) return;
            r = workThread;
        }
        r.write(out, end);
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "无法建立连接，请确保有开启的蓝牙服务端");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "连接已丢失");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // 重启连接
        BluetoothFactory.this.start();
    }

    @Override
    public boolean serverConnect(BluetoothSocket socket, BluetoothDevice device) {
        synchronized (BluetoothFactory.this) {
            switch (mState) {
                case STATE_LISTENING:
                case STATE_CONNECTING:
                    //connect normal,open workThread
                    connected(socket,device);
                    return false;
                case STATE_NONE:
                case STATE_CONNECTED:
                    return true;
            }
        }
        return false;
    }

    @Override
    public void serverConnectFailed() {
        connectionFailed();
    }

    @Override
    public void clientConnect(BluetoothSocket socket, BluetoothDevice device) {
        //重置客户端连接
        synchronized (BluetoothFactory.this) {
            clientThread = null;
        }
        connected(socket, device);
    }

    @Override
    public void clientConnectFailed() {
        connectionFailed();
    }

    public void setParseFactory(ParseFactory factory) {
        this.parseFactory = null;
        this.parseFactory = factory;
    }

}
