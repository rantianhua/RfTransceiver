package com.rftransceiver;

import com.audio.Speex;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//PCM数据编码类
//一般无需修改本类
public class Audio_Encoder implements Runnable
{
	   private static Audio_Encoder encoder;  //编码器实例
	    private boolean isEncoding = false; 	  
	   
	    private List<AudioData> dataList = null;// 存放数据
	   private Speex coder;
	    public static Audio_Encoder getInstance() 
	    {  
	        if (encoder == null)
	        {  
	            encoder = new Audio_Encoder();  
	        }  
	        return encoder;  
	    }  
	  
	    private Audio_Encoder() //构造函数
	    {  
	    	//多线程中同步数据
	        dataList = Collections.synchronizedList(new LinkedList<AudioData>());  
	    }  
	  
	    public void addData(short[] data, int size) //添加数据
	    {
	        AudioData rawData = new AudioData();
	        rawData.setSize(size);  
	        short[] tempData = new short[size];  
	        System.arraycopy(data, 0, tempData, 0, size);  
	        rawData.setRealData(tempData);  
	        dataList.add(rawData);  
	    }  
	  
	    // 开始编码  
	    public void startEncoding()
	    {  
	        if (isEncoding)
	        {  	          
	            return;  
	        }  
	        new Thread(this).start();  
	    }  
	  
	    // 结束  
	    public void stopEncoding() 
	    {  
	        this.isEncoding = false;  
	    }  
	  
	    public void run() 
	    {  
	        // 先启动发送端  
	        AudioSender sender = new AudioSender();
	        sender.startSending();  
	  
	        int encodeSize = 0;  
	        byte[] encodedData;
	  
	        // 初始化编码器  
	        try 
	        {
	        	coder=new Speex();
	        	coder.init();	         
	       
			} 
	        catch (Exception e) 
	        {
				 isEncoding=false;
				 return;
			}

	        isEncoding = true;  
	        while (isEncoding)
	        {  
	        	//暂无数据处理
	            if (dataList.size() == 0)
	            {  
	                try 
	                {  
	                    Thread.sleep(20);  
	                } catch (InterruptedException e) 
	                {  
	                    e.printStackTrace();  
	                }  
	                continue;  
	            }  
	            if (isEncoding)
	            {
	                AudioData rawData = dataList.remove(0);  //取出待编码数据
	                encodedData = new byte[rawData.getSize()];  
	                encodeSize=coder.encode(rawData.getRealData(),0,encodedData, rawData.getSize());
	                 
	                if (encodeSize > 0) 
	                {  
	                    sender.addData(encodedData, encodeSize);  
	                    //encodedData = new byte[encodedData.length];
	                }
	            }  
	        }  
	      //停止发送
	        sender.stopSending();  
	    }  
}
