package com.source.text;

import android.util.Log;

import com.rftransceiver.datasets.MyDataQueue;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.DataPacketOptions;
import com.source.SendMessageListener;

import java.math.BigDecimal;

/**
 * Created by Rth on 2015/4/29.
 * this class handle the text content
 * if user input a content ,this class need to unpack the content
 * a packets (66bytes per packet)
 */
public class TextEntity implements Runnable{

    /**
     * the options to decide the packet
     */
    private DataPacketOptions options;

    /**
     * @param sendListener callback in MainActivity
     */
    private SendMessageListener sendListener = null;

    /**
     *
     *the cache to save packet
     */
    private MyDataQueue dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Text_Send);

    /**
     *
     * the counter to count the number of temp,64 bytes per packet
     */
    private int index = 0;

    /**
     * a cache to save text data
     */
    private byte[] temp;

    /**
     * every packet's real data's length
     */
    private int realDataLen;

    /**
     * used for imageData
     */
    private float imgDataLen;
    private float sendDataLen;

    public TextEntity() {

    }

    public void unpacking(String content,DataPacketOptions.TextType type) {
        if(type == DataPacketOptions.TextType.Image) {
            unpacking(content);
            return;
        }
        byte[] text = content.getBytes();
        int len = text.length;
        int remainder = len % (options.getLength()-1-options.getOffset());
        int count = len / (options.getLength()-1-options.getOffset());
        initTemp(type);
        for(int i = 0; i < len;i++) {
            temp[index++] = text[i];
            if(index == options.getLength()-1) {
                index = options.getOffset();
                //cache full
                if(remainder == 0 && dataQueue.getSize() == count-1) {
                    Log.e("unpacking", "the last packet");
                    temp[options.getRealLenIndex()] = (byte)(index-options.getOffset());
                }
                dataQueue.add(temp);
                initTemp(type);
            }
        }
        if(index > options.getOffset()) {
            //the last packet that length less than 60
            temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
            index = options.getOffset();
            dataQueue.add(temp);
        }
        //start to send data
        PoolThreadUtil.getInstance().addTask(this);
    }

    /**
     * unpack image data
     * @param content
     */
    private volatile boolean addImageData = true;
    private void unpacking(String content) {
        initTemp(DataPacketOptions.TextType.Image);
        imgDataLen = 0;
        byte[] sourceData = content.getBytes();
        imgDataLen = sourceData.length;
        int remainder = (int)(imgDataLen % realDataLen);
        int count = (int) imgDataLen / realDataLen;
        PoolThreadUtil.getInstance().addTask(new SendImageData());
        for(int i = 0;i <= count;i++) {
            if(i != count) {
                System.arraycopy(sourceData,i*realDataLen,temp,options.getOffset(),realDataLen);
                if(remainder == 0) {
                    //the last packet
                    temp[options.getRealLenIndex()] = (byte)realDataLen;
                }
                dataQueue.add(temp);
                initTemp(DataPacketOptions.TextType.Image);
            }else {
                if(remainder == 0)  break;
                //the last packet
                System.arraycopy(sourceData,i*realDataLen,temp,options.getOffset(),remainder);
                temp[options.getRealLenIndex()] = (byte)remainder;
                dataQueue.add(temp);
            }
        }
        setAddImageData(false);
    }

    private synchronized void setAddImageData(boolean add) {
        addImageData = add;
    }

    private synchronized boolean getAddImageData() {
        return addImageData;
    }

    /**
     * send images datas
     */
    class SendImageData implements Runnable {
        @Override
        public void run() {
            setAddImageData(true);
            while (getAddImageData()) {
                byte[] imageData = (byte[])dataQueue.get();
                if(imageData != null) {
                    if(sendListener == null) return;
                    if(imageData[options.getRealLenIndex()] == options.getRealLen()) {
                        sendDataLen += realDataLen;
                    }else {
                        sendDataLen += imageData[options.getRealLenIndex()];
                    }
                    sendListener.sendPacketedData(imageData,false,getSendImgPercent());
                    try {
                        Thread.sleep(100);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            int size = dataQueue.getSize();
            for(int i = 0;i < size;i++) {
                byte[] data = (byte[])dataQueue.get();
                if(sendListener == null || data == null) return;
                if(data[options.getRealLenIndex()] == options.getRealLen()) {
                    sendDataLen += realDataLen;
                }else {
                    sendDataLen += data[options.getRealLenIndex()];
                }
                sendListener.sendPacketedData(data,false,getSendImgPercent());
                try {
                    Thread.sleep(100);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int  getSendImgPercent() {
        float scale = sendDataLen / imgDataLen;
        scale *= 100;
        int per = (int)scale;
        if(per == 100) sendDataLen = 0;
        return per;
    }

    private void initTemp(DataPacketOptions.TextType type){
        temp = null;
        temp = new byte[options.getLength()];
        temp[0] = options.getHead();
        temp[options.getLength()-1] = options.getTail();
        temp[options.getTypeFlagIndex()] = options.getTypeFlag(type);
        temp[options.getRealLenIndex()] = options.getRealLen();
    }

    @Override
    public void run() {
        while (dataQueue.getSize() > 0) {
            boolean end = dataQueue.getSize() == 1;
            if(sendListener == null) return;
            sendListener.sendPacketedData((byte[])dataQueue.get(),end);
            try {
                Thread.sleep(40);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setSendListener(SendMessageListener sendListener) {
        this.sendListener = null;
        this.sendListener = sendListener;
    }

    public void setOptions(DataPacketOptions options) {
        this.options = null;
        this.options = options;
        if(options != null) {
            index = options.getOffset();
            realDataLen = (options.getLength()-1-options.getOffset());
        }
    }
}
