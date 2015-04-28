package com.blutooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
* Created by Rth on 2015/4/25.
*/
public class BluetoothFactory {

    private static final String TAG = "BluetoothSever";

    //蓝牙服务端的name和UUID
    private static final String NAME = TAG;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ServerThread serverThread;  //服务端监听线程
    private ClientThread clientThread;    //客户端连接线程
    private WorkThread workThread;   //连接后的工作线程
    private int mState;

    //标志蓝牙连接的状态
    public static final int STATE_NONE = 0;       // 无状态
    public static final int STATE_LISTENING = 1;     // 监听连接中
    public static final int STATE_CONNECTING = 2; // 正在连接中
    public static final int STATE_CONNECTED = 3;  //已连接一个远程设备
    public static final int  STATE_STOP_LISTEN= 4;  //服务端停止监听

    public static int n = 1;    //计数器

    public BluetoothFactory(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;    //初始化服务端状态
        mHandler = handler;
    }

    //这只当前的连接状态
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        //让UI更新蓝牙状态
        mHandler.obtainMessage(Constants.MSG_WHAT_STATE, state, -1).sendToTarget();
    }

    /**
     * 返回当前蓝牙状态
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 开启蓝牙服务端，开始监听连接
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // 关闭蓝牙客户端
        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        //关闭工作线程
        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        //设置当前状态为正在监听
        setState(STATE_LISTENING);

        //开启蓝牙服务端
        if (serverThread == null) {
            serverThread = new ServerThread();
            serverThread.start();
        }
    }

    /**
     * 用已有的蓝牙设备建立远程连接
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        if (mState == STATE_CONNECTING) {
            if (clientThread != null) {
                clientThread.cancel();
                clientThread = null;
            }
        }

        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        // 开启客户端连接线程
        clientThread = new ClientThread(device);
        clientThread.start();

        setState(STATE_CONNECTING);
    }

    /**
     管理连接蓝牙后的socket
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {

        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }

        // 关闭已有的工作线程
        if (workThread != null) {
            workThread.cancel();
            workThread = null;
        }

        if(serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        //开启管理连接的工作线程
        workThread = new WorkThread(socket);
        workThread.start();

        // 将连接的蓝牙设备名称返回给UI
        Message msg = mHandler.obtainMessage(Constants.MSG_WHAT_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
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
        setState(STATE_NONE);
    }

    /**
     *向工作线程中写入数据发送
     */
    public void write(byte[] out) {
        WorkThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = workThread;
        }
        r.write(out);
    }

    /**
     *连接失败后通知UI
     */
    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "无法建立连接，请确保有开启的蓝牙服务端");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // 重新开启服务以备建立连接
        BluetoothFactory.this.start();
    }

    /**
     * 通知UI连接丢失
     */
    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "连接已丢失");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // 重启连接
        BluetoothFactory.this.start();
    }

    /**
     * 蓝牙连接的服务端
     */
    private class ServerThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public ServerThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME,
                        MY_UUID);
            } catch (IOException e) {
                Log.e(TAG,  "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.e(TAG,"begin server listen");
            BluetoothSocket socket = null;
            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG,  "accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothFactory.this) {
                        switch (mState) {
                            case STATE_LISTENING:
                            case STATE_CONNECTING:
                                //连接正常,开启工作线程
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                //准备连接或已经有了连接，则关闭新的连接
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
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
                setState(STATE_STOP_LISTEN);
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    /**
     *客户端连接
     */
    private class ClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                        MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() from a device  failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN a socket connect:");
            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() a device socket " , e2);
                }
                connectionFailed();
                return;
            }

            //重置客户端连接
            synchronized (BluetoothFactory.this) {
                clientThread = null;
            }
            // 管理连接
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect（device)  socket failed", e);
            }
        }
    }

    /**
     * 工作线程
     */
    private class WorkThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public WorkThread(BluetoothSocket socket) {
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
            Log.i(TAG, "begin work thread");
            byte[] buffer = new byte[1068];
            byte[] temp= new byte[64];  //每接收64个进行处理
            int indexTemp = 0;  //计数器
            byte[] tempSounds = new byte[10];   //语音缓存，10字节为一单位，方便直接解码
            int indexSounds = 0;    //语音信息计数器
            int mode  = -1; //mode 表示此次接收的数据类型，0表示语音，1表示文本，-1是初始值
            int counts = -1;    //表示64字节整包的数量
            int bytes;
            while (true) {
                // 保持对InputStream的监听，直到有异常抛出
                try {
                    bytes = mmInStream.read(buffer);
                    for (int i = 0; i < bytes; i++) {
                        temp[indexTemp++] = buffer[i];
                        if (indexTemp == 64) {
                            //已读满64位
                            if (mode == -1) {
                                //先确认数据类型
                                for (byte b : temp) {
                                    if (b == (byte) 0x03) {
                                        mode = 0;  //语音
                                        Log.e("start receive", "sounds");
                                    } else {
                                        mode = 1;   //文本
                                    }
                                }
                            } else {
                                if (counts < 0) {
                                    //读取数据长度
                                    counts = (temp[0] * 10 + temp[1]) / 64;
                                    Log.e("receive pakage number is ", counts + "");
                                } else {
                                    //此处读取数据段
                                    if (counts == 0) {
                                        //表示这个是结束包，先进行判断
                                        boolean end = false;
                                        for (byte b : temp) {
                                            if (b == (byte) 0x0f) {
                                                end = true;
                                            } else {
                                                end = false;
                                                break;
                                            }
                                            if (end) {
                                                //复位
                                                counts = -1;
                                                mode = -1;
                                                //回传
                                                Log.e("reveive complete", "now can feedback ");
                                                //通知UI接收完成
                                                mHandler.obtainMessage(Constants.MESSAGE_READ,
                                                        tempSounds.length, 2, null).sendToTarget();
                                            }
                                        }
                                    } else {
                                        //接收到数据段
                                        if (mode == 0) {
                                            //语音数据
                                            int dataLength = 64;
                                            if (counts == 1) {
                                                //最后一个包，取出有效数据的长度
                                                dataLength = temp[63];
                                                Log.e("data length of last pakage is ", dataLength + "");
                                            }
                                            for (int k = 0; k < dataLength; k++) {
                                                tempSounds[indexSounds++] = temp[i];
                                                if (indexSounds == 10 || (k == dataLength - 1 && counts == 1)) {
                                                    //复位
                                                    indexSounds = 0;
                                                    //向UI发送
                                                    mHandler.obtainMessage(Constants.MESSAGE_READ,
                                                            tempSounds.length, 0, tempSounds).sendToTarget();
                                                }
                                            }
                                        } else {
                                            //文本数据
                                        }
                                        //已读取一个数据段
                                        counts--;
                                    }
                                }
                            }
                            //计数器复位
                            indexTemp = 0;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        int count = 0;
        StringBuilder sb = new StringBuilder();
        public void write(byte[] sendBuff) {
            try {
                mmOutStream.write(sendBuff);
                for(byte b : sendBuff) {
                    sb.append(String.format("%#x ",b));
                }
                Log.e("write ",++count + "个包: " + sb.toString());
                if(sendBuff[65] == (byte) 0x07) {
                    count = 0;
                }
                sb.delete(0,sb.length());
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, sendBuff.length, -1,sendBuff)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
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
