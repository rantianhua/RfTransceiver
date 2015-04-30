package com.brige.blutooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.rftransceiver.util.Constants;

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

    /**
     *
     * @param socket
     * @param han
     */

    /**
     * buff to cache what we read in the socket
     */
    private byte[] buffer = new byte[1068];
    /**
     * a receive cache ,64 bytes per packet
     */
    private byte[] temp= new byte[64];
    /**
     * the temp's counter
     */
    private int indexTemp = 0;
    /**
     * the cache of sounds ,10 bytes per packet
     */
    private byte[] tempSounds = new byte[10];
    /**
     * tempSounds's counter
     */
    private int indexSounds = 0;
    /**
     * mode represent the type of the data we receive ,-1 indicates that i do not get the type of the data
     * 0 indicates that we will receive sounds data and 1 is text data
     */
    private int mode  = -1;
    /**
     * record the number of the packets
     */
    private int counts = -1;
    /**
     * this is the feedback packet,head is 0x01 and tail is 0x05
     */
    private byte[] feedBack = new byte[66];

    private int sum = 0;
    private StringBuilder sb;

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

        feedBack[0] = (byte) 0x01;
        feedBack[65] = (byte) 0x05;
        sb = new StringBuilder();
        sb.append("A ");
    }

    public void run() {
        /**
         * how long we read in the socket
         */
        int bytes;
        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                for (int i = 0; i < bytes; i++) {
                    sb.append(String.format("%#x ",buffer[i]));
                    temp[indexTemp++] = buffer[i];
                    if (indexTemp == 64) {
                        if(parseReceiveData()) {
                            continue;
                        }
                        //reset counter
                        indexTemp = 0;
                        counts--;
                        Log.e("receive " + (++ sum) + "th 个包",sb.toString());
                        sb.delete(0,sb.length());
                        sb.append("A ");
                        checkFeedback();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                break;
            }
        }
    }

    private void checkFeedback() {
        //check weather we need to feedback
        if(counts == 0) {
            //now we need to feedback
            Log.e("receive complete", "now can feedback ");
            write(feedBack);
            counts = -1;
        }
    }

    //the return value tell the for{} weather continue or not
    private boolean parseReceiveData() {
        if (counts == -1)
        {
            //first we need to know the counts
            int high = temp[0] < 0 ? 256 + temp[0] : temp[0];
            int low = temp[1] < 0 ? 256 + temp[1] : temp[1];
            counts = (high * 16 + low) / 64;
            Log.e("receive packet's number is ", counts + "");
            indexTemp = 0;
            Log.e("the length packet is ",sb.toString());
            sb.delete(0,sb.length());
            sb.append("A ");
            return true;
        }
        if (mode == -1) {
            //next we need to know the mode
            for (byte b : temp)
            {
                if (b == (byte) 0x03) {
                    mode = 0;
                } else {
                    mode = 1;
                }
            }
            counts--;
            indexTemp = 0;
            checkFeedback();
            sb.delete(0,sb.length());
            sb.append("A ");
            return true;
        }
        //now we can read normal data
        if(mode == 0) {
            int length = temp[0];
            if(length != 126) {
                //this is the last packet ,we need to reset all the mark or flag,ready to receive next data
                mode = -1;
                Log.e("receive the last packet","the length is " + length );
                sum = 0;
            }else {
                length = length / 2;
            }
            for(int j = 1;j <= length;j++) {
                tempSounds[indexSounds++] = temp[j];
                if(indexSounds == 10) {
                    //sounds cache have been full
                    handler.obtainMessage(Constants.MESSAGE_READ,
                            0,10,tempSounds).sendToTarget();
                    tempSounds = new byte[10];
                    indexSounds = 0;
                }
            }
        }else {

        }
        return false;
    }

    int count = 0;
    StringBuilder send = new StringBuilder();
    public void write(byte[] sendBuff) {
        try {
            mmOutStream.write(sendBuff);
            for(byte b : sendBuff) {
                send.append(String.format("%#x ",b));
            }
            Log.e("write ",++count + "个包: " + send.toString());
            send.delete(0, send.length());
            handler.obtainMessage(Constants.MESSAGE_WRITE, sendBuff.length, -1,sendBuff)
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

