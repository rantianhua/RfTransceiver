package com.rftransceiver;


import android.util.Log;

import com.datasets.MyDataQueue;


//发送编码数据类
//补全初始化通道 以及发送代码
public class AudioSender  implements Runnable
{
	private boolean isSendering = false;  
	private MyDataQueue dataQueue;
    public static int num = 1;

    public static TestSoundWithBluetooth sendListener = null;

    public static volatile boolean end = false;
	
	 public AudioSender() 
	 {
         //初始化发送通道
         dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Sound_Decoder);
	 }
	
	public void startSending() 
	{
		 new Thread(this).start(); 
	
	}

	//将数据添加至发送缓冲区
	public void addData(byte[] data, int size) 
	{
		    AudioData encodedData = new AudioData();
	        encodedData.setSize(size);  
            encodedData.setencodeData(data);
	        dataQueue.add(encodedData);  //添加已编码数据
	}

	
	//停止发送
	public void stopSending() 
	{
		this.isSendering = false;  
	}
	
	 //线程操作
	public void run() 
	 {
            StringBuilder sb = new StringBuilder();
            byte[] temp = new byte[64];
	        this.isSendering = true;
            int index = 0; //63字节计数器
            int sum = 0;   //包计数器
	        while (isSendering)
	        {
                AudioData sendData = (AudioData)dataQueue.get();
                if(sendData == null) {
                    //此处可以加个延时，减少取出数据为空的次数
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else {
                    for(int i = 0;i<sendData.getSize();i++) {
                        temp[index++] = sendData.getencodeData()[i];
                        if(index == 64) {
                            //temp已满，可以发送
                            if(sendListener != null) {
                                for(byte b : temp)  {
                                    sb.append(String.format("%#x ",b));
                                }
                                Log.e("send the " + (++sum) + "th pakage is ",sb.toString());
                                sb.delete(0,sb.length());
                                sendListener.sendSound(temp,index);
                            }
                            //重新将计数器清0，准备下次装入
                            index = 0;
                        }else {
                            if(dataQueue.getSize() == 0 && end) {
                                for(byte b : temp)  {
                                    sb.append(String.format("%#x ",b));
                                }
                                Log.e("send the " + (++sum) + "th pakage(may be the last) is ",sb.toString());
                                sb.delete(0,sb.length());
                                sum = 0;
                                if(sendListener != null) {
                                    sendListener.sendSound(temp,index);
                                }
                                end = false;
                                isSendering = false;
                            }
                        }
                    }
                }
	        }
	 }

    //测试发送接口
    public interface TestSoundWithBluetooth {
        void sendSound(Object data,int size);
    }

}
