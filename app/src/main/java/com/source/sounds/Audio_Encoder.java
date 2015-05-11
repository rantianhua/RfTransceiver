package com.source.sounds;

import com.audio.Speex;
import com.rftransceiver.datasets.AudioData;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//PCM Encoder
public class Audio_Encoder implements Runnable
{
	   private static Audio_Encoder encoder;  //the instance os Encoder
	    private volatile  boolean isEncoding = false;
	   
	    private List<AudioData> dataList = null;// cache data that will be encode
	    private Speex coder;
        private SoundsEntity sender;

	    public static Audio_Encoder getInstance() 
	    {  
	        if (encoder == null)
	        {  
	            encoder = new Audio_Encoder();
	        }
	        return encoder;  
	    }  
	  
	    private Audio_Encoder()
	    {  
	        dataList = Collections.synchronizedList(new LinkedList<AudioData>());
	    }  
	  
	    public void addData(short[] data, int size)
	    {
	        AudioData rawData = new AudioData();
	        rawData.setSize(size);  
	        short[] tempData = new short[size];  
	        System.arraycopy(data, 0, tempData, 0, size);  
	        rawData.setRealData(tempData);  
	        dataList.add(rawData);
            data = null;
	    }  
	  
	    public void startEncoding()
	    {  
	        if (isEncoding)
	        {  	          
	            return;  
	        }  
	        new Thread(this).start();  
	    }  
	  
	    public void stopEncoding()
	    {  
	        this.isEncoding = false;  
	    }  
	  
	    public void run() 
	    {  
	        //start sender
            sender.stopSending();
	        sender.startSending();

	        int encodeSize = 0;
	        byte[] encodedData;
	  
	        // init coder
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
	        	//no data to handle
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
	                AudioData rawData = dataList.remove(0);  //get the data to encode
	                encodedData = new byte[rawData.getSize()];  
	                encodeSize=coder.encode(rawData.getRealData(),0,encodedData, rawData.getSize());
	                 
	                if (encodeSize > 0) 
	                {  
	                    sender.addData(encodedData, encodeSize);  
	                }
	            }  
	        }  
	        sender.stopSending();
	    }

    public void setSoundsEntity(SoundsEntity soundsEntity) {
        this.sender =  null;
        this.sender = soundsEntity;
    }
}
