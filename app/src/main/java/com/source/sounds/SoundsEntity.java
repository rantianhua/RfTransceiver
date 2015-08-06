package com.source.sounds;


import android.util.Log;

import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.datasets.AudioData;
import com.rftransceiver.datasets.MyDataQueue;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.DataPacketOptions;
import com.source.SendMessageListener;


public class SoundsEntity implements Runnable
{
	private volatile  boolean isSendering = false;  //标志用户按住录音按钮
    private boolean isRunning = false;  //标志录音线程正在运行
	private MyDataQueue dataQueue;  //缓存录音信息

    private DataPacketOptions options;  //数据包参数

    private SendMessageListener sendListener = null;    //发送的回调函数

    byte[] temp;

    private int realSoundsLen;  //数据包中真实数据长度

    public SoundsEntity()
	 {
         dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Sounds_Send);
	 }
	
	public void startSending() 
	{
        if(isRunning()) return;
        setRunning(true);
        setSendering(true);
        try {
            MainActivity.soundsRecords.clear();
            PoolThreadUtil.getInstance().addTask(this);
        } catch (Exception e) {
            setSendering(false);
            setRunning(false);
        }
    }

    /**
     * 将编码数据加入缓存队列中
     * @param data 编码数据
     * @param size 数据长度
     */
	public void addData(byte[] data, int size)
	{
            byte[] tempData = new byte[size];
            System.arraycopy(data,0,tempData,0,size);
            MainActivity.soundsRecords.add(tempData);   //
		    AudioData encodedData = new AudioData();
	        encodedData.setSize(size);
            encodedData.setencodeData(tempData);
	        dataQueue.add(encodedData);
            data = null;
            tempData = null;
	}

	//停止发送录音
	public void stopSending() 
	{
        setSendering(false);
	}
	
	public void run()
	 {
            initTemp();
            int index = options.getOffset(); //数据包索引
            int soundsPackets = (options.getLength()-options.getOffset()-1) / Constants.Small_Sounds_Packet_Length;
	        while (isRunning())
	        {
                if(isSendering()) {
                    //表明现在dataQueue中数据个数是动态的
                    if(dataQueue.getSize() > 5) {
                        //保证在用户停止录音后，dataQueue中还有数据，这样能知道发送的最后一个包
                        for(int i = 0; i < 4;i++) {
                            AudioData restData = (AudioData)dataQueue.get();
                            for(int j = 0;j<restData.getSize();j++) {
                                temp[index++] = restData.getencodeData()[j];
                                if (index == options.getLength()-1) {
                                    //拼好一个包，可以发送
                                    sendSoundsData(temp, false, realSoundsLen);
                                    initTemp();
                                    //复位索引，准备拼接下一个包
                                    index = options.getOffset();
                                }
                            }
                        }
                    }
                }else {
                    //用户已停止录音，现在dataQueue中的数据是固定的
                    int restCountsInDataQueue = dataQueue.getSize();
                    int restCountsIntemp = (index-options.getOffset()) / Constants.Small_Sounds_Packet_Length;
                    if(restCountsInDataQueue == 0) {
                        setRunning(false);
                    }
                    for(int i = 0;i < restCountsInDataQueue;i++) {
                        AudioData restData = (AudioData)dataQueue.get();
                        for(int j = 0;j<restData.getSize();j++) {
                            temp[index++] = restData.getencodeData()[j];
                            if (index == options.getLength()-1) {
                                //检查是不是最后一个包
                                if((restCountsInDataQueue+restCountsIntemp) % soundsPackets == 0 && i == restCountsInDataQueue-1) {
                                    temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
                                    sendSoundsData(temp, true, temp[options.getRealLenIndex()]);
                                    setRunning(false);
                                }else {
                                    sendSoundsData(temp, false, realSoundsLen);
                                    initTemp();
                                }
                                //复位索引
                                index = options.getOffset();
                            }
                        }
                    }
                    if(index > options.getOffset()) {
                        temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
                        sendSoundsData(temp, true, temp[options.getRealLenIndex()]);
                        setRunning(false);
                        index = options.getOffset();
                    }
                }
	        }
	 }

    /**
     * send packed packets to ble
     * @param data
     * @param end
     * @param length
     */
    private void sendSoundsData(byte[] data,boolean end,int length) {
        if (sendListener == null) {
            setRunning(false);
            return;
        }
        sendListener.sendPacketedData(data,end,length);
    }

    public synchronized boolean isSendering() {
        return isSendering;
    }

    public synchronized void setSendering(boolean isSendering) {
        this.isSendering = isSendering;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /**
     * 初始化包缓存器，
     * temp[options.getRealLenIndex()]这位数据用来标识是不是发送的最后一个包，
     * 如果值为option.getRealLen，则不是结束包，
     * 若是结束包，则值为最后一个包中真实语音数据的长度，该值小于option.getRealLen
     *
     */
    private void initTemp() {
        temp = null;
        temp = new byte[options.getLength()];
        temp[0]  = options.getHead();
        temp[options.getLength()-1] = options.getTail();
        temp[options.getTypeFlagIndex()] = options.getTypeFlag();
        temp[options.getRealLenIndex()] = options.getRealLen();
    }

    public void setSendListener(SendMessageListener sendListener) {
        this.sendListener = null;
        this.sendListener = sendListener;
    }

    public void setOptions(DataPacketOptions options) {
        this.options =null;
        this.options = options;
        if(options != null) {
            realSoundsLen = options.getRealLen()-options.getOffset()-1;
        }
    }

}
