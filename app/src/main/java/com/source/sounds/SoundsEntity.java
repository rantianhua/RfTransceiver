package com.source.sounds;


import com.my_interface.SendMessageListener;
import com.rftransceiver.datasets.AudioData;
import com.rftransceiver.datasets.MyDataQueue;
import com.rftransceiver.util.Constants;
import com.source.DataPacketOptions;


public class SoundsEntity implements Runnable
{
	private volatile  boolean isSendering = false;  //mark is sendering sounds
    private boolean isRunning = false;  //mark the thread is running，only control by this thread
	private MyDataQueue dataQueue;  //sounds data cache

    private DataPacketOptions options;

    private SendMessageListener sendListener = null;

    byte[] temp;

    public SoundsEntity()
	 {
         dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Sounds_Send);
	 }
	
	public void startSending() 
	{
        isRunning = true;
        isSendering = true;
        try {
            new Thread(this).start();
        } catch (Exception e) {
            e.printStackTrace();
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
		this.isSendering = false;  
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
	        while (isRunning)
	        {
                if(isSendering) { //now the cache length is dynamic
                    if(dataQueue.getSize() > 7) {
                        for(int i = 0; i < 7;i++) {
                            AudioData restData = (AudioData)dataQueue.get();
                            for(int j = 0;j<restData.getSize();j++) {
                                temp[index++] = restData.getencodeData()[j];
                                if (index == options.getLength()-1) {
                                    //temp have been full,can to be sent
                                    sendListener.sendSound(temp,false);
                                    initTemp();
                                    //reset to recount
                                    index = options.getOffset();
                                }
                            }
                        }
                    }else {
                        try {
                            Thread.sleep(10);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }else { //the user have stop,now the cache's length is changeless
                    int restCountsInDataQueue = dataQueue.getSize();
                    int restCountsIntemp = (index-options.getOffset()) / Constants.Small_Sounds_Packet_Length;
                    if(restCountsInDataQueue == 0) {
                        isRunning = false;
                    }
                    for(int i = 0;i < restCountsInDataQueue;i++) {
                        AudioData restData = (AudioData)dataQueue.get();
                        for(int j = 0;j<restData.getSize();j++) {
                            temp[index++] = restData.getencodeData()[j];
                            if (index == options.getLength()-1) {
                                //temp have been full,can to be sent
                                if((restCountsInDataQueue+restCountsIntemp) % soundsPackets == 0 && i == restCountsInDataQueue-1) {
                                    temp[options.getRealLenIndex()] = (byte) (index-options.getOffset());
                                    sendListener.sendSound(temp,true);
                                    isRunning = false;  //shutdown this thread
                                    sum = 0;    //ready to count next send
                                }else {
                                    sendListener.sendSound(temp,false);
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
                        sendListener.sendSound(temp,true);
                        isRunning = false;  //shutdown this thread
                        sum = 0;    //ready to count next send
                        index = options.getOffset();
                    }
                }
	        }
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
    }
}
