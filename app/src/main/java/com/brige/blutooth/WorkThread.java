package com.brige.blutooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.source.parse.ParseFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Rth on 2015/4/30.
 */

public class WorkThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler handler;
    private final String TAG = getClass().getSimpleName();
    private volatile boolean isReading = false;
    private StringBuilder sb = new StringBuilder();
    private ParseFactory parseFactory;


    public WorkThread(BluetoothSocket socket,Handler han) {
        mmSocket = socket;
        handler = han;
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
        /**
         * how long we read in the socket
         */
        int bytes;

        isReading = true;

        byte[] buffer = new byte[1068];

        while (isReading) {
            try {
                bytes = mmInStream.read(buffer);
                try {
                    parseFactory.roughParse(buffer,bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("WorkTHread","parse work wrong");
                }
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                break;
            }
        }
    }

    public void write(byte[] sendBuff,boolean end) {
        try {
            if(sendBuff == null) {
                return;
            }
            if(mmOutStream == null) return;
            mmOutStream.write(sendBuff);
            for(byte b : sendBuff) {
                sb.append(String.format("%#x ",b));
            }
            Log.e("have write",sb.toString());
            sb.delete(0,sb.length());
        } catch (IOException e) {
            Log.e("write ","write exception",e);
        }
    }

    public void cancel() {
        try {
            isReading = false;
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }

    public void setParseFactory(ParseFactory factory) {
        this.parseFactory = null;
        this.parseFactory = factory;
    }
}

