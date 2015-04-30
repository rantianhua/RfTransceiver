package com.audio;

import android.util.Log;

import com.rftransceiver.datasets.MyDataQueue;
import com.rftransceiver.datasets.AudioData;


public class Audio_Decoder implements Runnable
{
	private static Audio_Decoder decoder;   //解码器单一实例
	private MyDataQueue dataList = null;

    private static final int MAX_BUFFER_SIZE = 2048;  
    private Speex coder=new Speex();
    private short[] decodedData = new short[1024];
    public boolean isDecoding=false;

    StringBuilder sb = new StringBuilder();

	private Audio_Decoder() 
	{  
		//实例化解码缓冲区
        this.dataList = MyDataQueue.getInstance(MyDataQueue.DataType.Sound_Decoder);
    }

	@Override
	public void run() 
	{
        Log.e("Decoder","开启解码");
        Audio_Player player = Audio_Player.getInstance();
        player.startPlaying();  
        this.isDecoding = true;
        coder.init();
        int decodeSize = 0;

        while (isDecoding) {

            AudioData encodeData  = (AudioData)dataList.get();
            if(encodeData == null) {
                //此处可增加一个延时

            }else{
                decodedData = new short[MAX_BUFFER_SIZE];
                //  解码数据的大小
                decodeSize=coder.decode(encodeData.getencodeData(), decodedData, encodeData.getSize());
                if (decodeSize > 0)
                {
                    player.addData(decodedData, decodeSize);
                    //decodedData = new short[decodeSize];
                }
            }
        }
        player.stopPlaying();
        MyDataQueue.recycle(MyDataQueue.DataType.Sound_Decoder);
	}
    
	 public void addData(AudioData data)
	 {
         //将数据添加至解码器缓存区
	        dataList.add(data);
	 }

    //开始解码
	public void startDecoding()
	{
		 if (isDecoding) 
		 {  
	            return;  
	     }
        Log.e("Audio_Decoder","start a new decode thread");
	    new Thread(this).start();
	}

	//关闭解码器
    public void stopDecoding() 
    {  
	        this.isDecoding = false;  
	}  

    //获取单一实例
	public static Audio_Decoder getInstance() 
	{
		if (decoder == null) 
		{  
            decoder = new Audio_Decoder();  
        }  
        return decoder;  
	}
}
