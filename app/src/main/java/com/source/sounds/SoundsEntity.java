package com.source.sounds;


import com.rftransceiver.datasets.AudioData;
import com.rftransceiver.datasets.MyDataQueue;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.DataPacketOptions;
import com.source.SendMessageListener;


public class SoundsEntity implements Runnable
{
	private volatile  boolean isSendering = false;  //mark is sendering sounds
    private boolean isRunning = false;  //mark the thread is running，
	private MyDataQueue dataQueue;  //sounds data cache

    private DataPacketOptions options;

    private SendMessageListener sendListener = null;

    byte[] temp;

    private int realSoundsLen;

    public SoundsEntity()
	 {
         dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Sounds_Send);
	 }
	
	public void startSending() 
	{
        setRunning(true);
        setSendering(true);
        try {
            PoolThreadUtil.getInstance().addTask(this);
        } catch (Exception e) {
            setSendering(false);
            setRunning(false);
        }
    }

	public void addData(byte[] data, int size)
	{
            byte[] tempData = new byte[size];
            System.arraycopy(data,0,tempData,0,size);
		    AudioData encodedData = new AudioData();
	        encodedData.setSize(size);
            encodedData.setencodeData(tempData);
	        dataQueue.add(encodedData);
            data = null;
            tempData = null;
	}

	//stop send
	public void stopSending() 
	{
        setSendering(false);
	}
	
	public void run()
	 {
            initTemp();
            int index = options.getOffset(); //the packet's counter
            int sum = 0;   //count the number of packets
         /**
          * every encode sounds packet's length is ten ,soundsPacket record a sounds sending packet have how much encode sounds packets
          */
            int soundsPackets = (options.getLength()-options.getOffset()-1) / Constants.Small_Sounds_Packet_Length;
	        while (isRunning())
	        {
                if(isSendering()) { //now the cache length is dynamic
                    if(dataQueue.getSize() > 5) {
                        for(int i = 0; i < 4;i++) {
                            AudioData restData = (AudioData)dataQueue.get();
                            for(int j = 0;j<restData.getSize();j++) {
                                temp[index++] = restData.getencodeData()[j];
                                if (index == options.getLength()-1) {
                                    //temp have been full,can to be sent
                                    sendSoundsData(temp, false, realSoundsLen);
                                    initTemp();
                                    //reset to recount
                                    index = options.getOffset();
                                }
                            }
                        }
                    }else {
//                        try {
//                            Thread.sleep(10);
//                        }catch (Exception e) {
//                            e.printStackTrace();
//                        }
                    }
                }else { //the user have stop,now the cache's length is changeless
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
                                //temp have been full,can to be sent
                                if((restCountsInDataQueue+restCountsIntemp) % soundsPackets == 0 && i == restCountsInDataQueue-1) {
                                    temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
                                    sendSoundsData(temp, true, temp[options.getRealLenIndex()]);
                                    setRunning(false);  //shutdown this thread
                                    sum = 0;    //ready to count next send
                                }else {
                                    sendSoundsData(temp, false, realSoundsLen);
                                    initTemp();
                                }
                                //reset to recount
                                index = options.getOffset();
                            }
                        }
                    }
                    if(index > options.getOffset()) {
                        //now temp is the last packet
                        temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
                        sendSoundsData(temp,true,temp[options.getRealLenIndex()]);
                        setRunning(false);  //shutdown this thread
                        sum = 0;    //ready to count next send
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
