package com.rftransceiver.datasets;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Rth on 2015/4/24.
 */
public class MyDataQueue {

    private static MyDataQueue dataQueue1 = null,dataQueue2 = null;

    private volatile Queue<Object> queue = null;

    public enum DataType{
        /**
         * 标志接收语音信息
         */
        Sound_Receiver,

        /**
         * 标志语音解码
         */
        Sound_Decoder,
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

}
