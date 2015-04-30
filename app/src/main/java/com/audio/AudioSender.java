package com.audio;


import android.util.Log;

import com.rftransceiver.datasets.AudioData;
import com.rftransceiver.datasets.MyDataQueue;


public class AudioSender  implements Runnable
{
	private volatile  boolean isSendering = false;  //mark is sendering sounds
    private boolean isRunning = false;  //mark the thread is running，only control by this thread
	private MyDataQueue dataQueue;  //sounds data cache

    public static TestSoundWithBluetooth sendListener = null;

    private StringBuilder sb = new StringBuilder();

    private byte[] note = new byte[66]; //the note packet of sounds,it tell me to packet next receive is sounds

    private boolean havaNote = false;   //mark weather send note packet or not

	 public AudioSender()
	 {
         note[0] = (byte) 0x01;
         note[65] = (byte) 0x04;
         for(int i = 1; i < 65; i++) {
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
         /**
          * the temp is a temporary cache to save the data which will be send
          * temp[0] is the head of the packet and temp[65] is the tail of the packet
          * temp[1] is a mark byte ,when it is 0x7e,it indicate the packet is a normal data packet,the value is two times of 63 ,else it indicates this
          * packet is the last packet(judgement for android) and the value of this byte is the effective data's length
          */
            byte[] temp = new byte[66];
            temp[0] = (byte) 0x01;
            temp[1] = (byte) 0x7e;
            temp[65] = (byte) 0x04;
            int index = 2; //the packets counter ,63 byte per packet
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
                                //temp have been full,can to be sent
                                packetToSend(temp,index,++sum);
                                temp = null;
                                temp = new byte[66];
                                temp[0] = (byte) 0x01;
                                temp[1] = (byte) 0x7e;
                                temp[65] = (byte) 0x04;
                                //reset to recount
                                index = 2;
                            }
                        }
                    }
                }else { //the user have stop,now the cache's length is changeless
                    int restCounts = dataQueue.getSize();
                    if(restCounts == 0) {
                        //there is no data
                        if(index > 2) {
                            //this is the last packet
                            temp[1] = (byte) (index-2);     //the last packet's tail is 0x07
                            packetToSend(temp,index,++sum);
                        }
                        isRunning = false;  //shutdown this thread
                        sum = 0;    //ready to count next send
                        index = 2;
                    }else {
                        for(int i = 0;i < restCounts;i++) {
                            AudioData restData = (AudioData)dataQueue.get();
                            for(int j = 0;j<restData.getSize();j++) {
                                temp[index++] = restData.getencodeData()[j];
                                if (index == 65) {
                                    //temp have been full,can to be sent
                                    packetToSend(temp,index,++sum);
                                    temp = null;
                                    temp = new byte[66];
                                    temp[0] = (byte) 0x01;
                                    temp[1] = (byte) 0x7e;
                                    temp[65] = (byte) 0x04;
                                    //reset to recount
                                    index = 2;
                                }
                            }
                        }
                        if(index > 2) {
                            //now temp is the last packet
                            temp[1] = (byte) (index-2);
                            packetToSend(temp,index,++sum);
                            isRunning = false;  //shutdown this thread
                            sum = 0;    //ready to count next send
                            index = 2;
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
            sendMessage = null;
        }
    }

    //the interface to callback , tell the UI thread to send sounds messages
    public interface TestSoundWithBluetooth {
        void sendSound(Object data,int size);
    }

}
