package com.source.text;

import com.my_interface.SendMessageListener;
import com.rftransceiver.datasets.MyDataQueue;
import com.source.DataPacketOptions;

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

    public TextEntity() {

    }

    public void unpacking(String content) {
        initTemp();
        byte[] text = content.getBytes();
        int len = text.length;
        for(int i = 0; i < len;i++) {
            temp[index++] = text[i];
            if(index == options.getLength()-1) {
                index = options.getOffset();
                //cache full
                dataQueue.add(temp);
                initTemp();
            }
        }
        if(index > options.getOffset()) {
            //the last packet that length less than 64
            temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
            index = options.getOffset();
            dataQueue.add(temp);
        }
        //start to send data
        new Thread(this).start();
    }

    private void initTemp(){
        temp = null;
        temp = new byte[options.getLength()];
        temp[0] = options.getHead();
        temp[options.getLength()-1] = options.getTail();
        temp[options.getTypeFlagIndex()] = options.getTypeFlag();
        temp[options.getRealLenIndex()] = options.getRealLen();
    }

    @Override
    public void run() {
        while (dataQueue.getSize() > 0) {
            boolean end = dataQueue.getSize() == 1;
            sendListener.sendPacketedData((byte[])dataQueue.get(),end);
//            try {
//                Thread.sleep(350);
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
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
        }
    }
}
