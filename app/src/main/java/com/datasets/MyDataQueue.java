package com.datasets;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Rth on 2015/4/24.
 */
public class MyDataQueue {

    private static MyDataQueue dataQueue1 = null,dataQueue2 = null,dataQueue3;

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

        /**
         * 标志接收文本
         */
        Text_receiver,

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
            case Text_receiver:
                if(dataQueue3 == null) {
                    dataQueue3 = new MyDataQueue();
                }
                return dataQueue3;
        }
        return null;
    }

    public static void recycle(DataType type) {
        switch (type) {
            case Sound_Receiver:
                if(dataQueue1 != null) {
                    dataQueue1 =null;
                }
            case Sound_Decoder:
                if(dataQueue2 != null) {
                    dataQueue2 = null;
                }
            case Text_receiver:
                if(dataQueue3 != null) {
                    dataQueue3 = null;
                }
        }
    }

    public synchronized boolean  add(Object o) {
        return queue.offer(o);
    }

    public synchronized Object get() {
        return queue.poll();
    }

    public synchronized int getSize() {
        return queue.size();
    }
}
