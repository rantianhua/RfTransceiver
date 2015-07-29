package com.rftransceiver.datasets;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Rth on 2015/4/24.
 */
public class MyDataQueue {

    private static MyDataQueue dataQueue1,dataQueue2,dataQueue3,dataQueue4;

    private volatile Queue<Object> queue = null;


    public enum DataType{
        /**
         * the cache to save data received
         */
        Sound_Receiver,

        /**
         * the cache to save data to be decoded
         */
        Sound_Decoder,

        /**
         * the cache to save text data will be send
         */
        Text_Send,

        /**
         * the cache to save sounds data will be send
         */
        Sounds_Send,
    }

    private MyDataQueue(){
        queue = new LinkedList<>();
    }


    public  static MyDataQueue getInstance(DataType type) {
        switch (type) {
            case Sound_Receiver:
                if(dataQueue1 == null) {
                    dataQueue1 = new MyDataQueue();
                }
                return dataQueue1;
            case Sound_Decoder:
                if(dataQueue2 == null) {
                    dataQueue2 = new MyDataQueue();
                }
                return dataQueue2;
            case Text_Send:
                if(dataQueue3 == null) {
                    dataQueue3 = new MyDataQueue();
                }
                return dataQueue3;
            case Sounds_Send:
                if(dataQueue4 == null) {
                    dataQueue4 = new MyDataQueue();
                }
                return dataQueue4;
        }
        return null;
    }

    public static void recycle(DataType type) {
        switch (type) {
            case Sound_Receiver:
                if(dataQueue1 != null) {
                    dataQueue1 =null;
                }
                break;
            case Sound_Decoder:
                if(dataQueue2 != null) {
                    dataQueue2 = null;
                }
                break;
            case Text_Send:
                if(dataQueue3!= null) {
                    dataQueue3 = null;
                }
                break;
            case Sounds_Send:
                if(dataQueue4 != null) {
                    dataQueue4 =null;
                }
                break;
        }
    }

    public synchronized void add(Object o) {
        queue.offer(o);
    }

    public synchronized Object get() {
        return queue.poll();
    }

    public synchronized int getSize() {
        return queue.size();
    }

    public synchronized void clear() {
        queue.clear();
    }

}
