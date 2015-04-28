package com.rftransceiver;


import android.util.Log;

import com.datasets.MyDataQueue;


public class AudioSender  implements Runnable
{
	private volatile  boolean isSendering = false;  //mark is sendering sounds
    private boolean isRunning = false;  //mark the thread is running，only control by this thread
	private MyDataQueue dataQueue;  //sounds data cache

    public static TestSoundWithBluetooth sendListener = null;

    public static volatile boolean end = false;

    private StringBuilder sb = new StringBuilder();

    private byte[] note = new byte[66]; //the note packet of sounds

    private boolean havaNote = false;   //mark weather send note packet or not
	
	 public AudioSender() 
	 {
         for(int i = 0; i < 66; i++) {
             note[i] = (byte) 0x03;
         }
         dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Sound_Decoder);
	 }
	
	public void startSending() 
	{
        isRunning = true;
        isSendering = true;
		new Thread(this).start();
	}

	public void addData(byte[] data, int size)
	{
		    AudioData encodedData = new AudioData();
	        encodedData.setSize(size);  
            encodedData.setencodeData(data);
	        dataQueue.add(encodedData);
	}

	
	//stop send
	public void stopSending() 
	{
		this.isSendering = false;  
	}
	
	public void run()
	 {
            byte[] temp = new byte[66];
            temp[0] = (byte) 0x01;
            temp[65] = (byte) 0x04;
            int index = 1; //the packets counter ,64 byte per packet
            int sum = 0;   //count the number of packets
	        while (isRunning)
	        {
                if(isSendering) { //now the cache length is dynamic
                    AudioData sendData = (AudioData)dataQueue.get();
                    if(sendData == null) {
                        try {
                            Thread.sleep(10);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else {
                        for(int i = 0;i<sendData.getSize();i++) {
                            temp[index++] = sendData.getencodeData()[i];
                            if (index == 65) {
                                //temp have full data,can to be sent
                                packetToSend(temp,index,++sum);
                                //reset to recount
                                index = 1;
                                temp = new byte[66];    //have a new object to cache
                                temp[0] = (byte) 0x01;
                                temp[65] = (byte) 0x04;
                            }
                        }
                    }
                }else { //the user have stop,now the cache's length is changeless
                    int restCounts = dataQueue.getSize();
                    if(restCounts == 0) {
                        //there is no data
                        if(index > 1) {
                            //this is the last packet
                            temp[65] = (byte) 0x07;     //the last packet's tail is 0x07
                            temp[64] = (byte) index;
                            packetToSend(temp,index,++sum);
                            //reset to recount
                            index = 1;
                        }
                        isRunning = false;  //shutdown this thread
                        sum = 0;    //ready to count next send
                        index = 1;
                    }else {
                        for(int i = 0;i < restCounts;i++) {
                            AudioData restData = (AudioData)dataQueue.get();
                            for(int j = 0;j<restData.getSize();j++) {
                                temp[index++] = restData.getencodeData()[j];
                                if (index == 65) {
                                    //temp have full data,can to be sent
                                    packetToSend(temp,index,++sum);
                                    //reset to recount
                                    index = 1;
                                    temp = new byte[66];    //have a new object to cache
                                    temp[0] = (byte) 0x01;
                                    temp[65] = (byte) 0x04;
                                }
                            }
                        }
                        if(index > 1) {
                            //now temp is the last packet
                            temp[65] = (byte) 0x07;     //the last packet's tail is 0x07
                            temp[64] = (byte) index;
                            packetToSend(temp,index,++sum);
                            isRunning = false;  //shutdown this thread
                            sum = 0;    //ready to count next send
                            index = 1;
                        }
                    }
                }
	        }
	 }

    private void packetToSend(byte[] sendMessage,int length,int sum) {
        if (sendListener != null) {
            if(!havaNote) {
                sendListener.sendSound(note, note.length);
                havaNote = true;
            }
            for (byte b : sendMessage) {
                sb.append(String.format("%#x ", b));
            }
            if(length != 65) {
                havaNote = false;
                Log.e("send the last " + sum + "th packet is ", sb.toString());
            }else {
                Log.e("send the " + sum + "th packet is ", sb.toString());
            }
            sb.delete(0, sb.length());
            sendListener.sendSound(sendMessage, length);
        }
    }

    //the interface to callback , tell the UI thread to send sounds messages
    public interface TestSoundWithBluetooth {
        void sendSound(Object data,int size);
    }

}
