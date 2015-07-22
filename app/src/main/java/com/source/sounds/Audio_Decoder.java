package com.source.sounds;

import android.util.Log;

import com.audio.Speex;
import com.rftransceiver.datasets.AudioData;
import com.rftransceiver.datasets.MyDataQueue;
import com.rftransceiver.util.PoolThreadUtil;


public class Audio_Decoder implements Runnable
{
	private static Audio_Decoder decoder;   //解码器单一实例
	private MyDataQueue dataList = null;
    private static final int MAX_BUFFER_SIZE = 256;
    private Speex coder=new Speex();
    private short[] decodedData;
    public boolean isDecoding=false;

	private Audio_Decoder()
	{  
		//实例化解码缓冲区
        this.dataList = MyDataQueue.getInstance(MyDataQueue.DataType.Sound_Decoder);
    }

	@Override
	public void run() 
	{
        Audio_Player player = Audio_Player.getInstance();
        player.startPlaying();
        coder.init();
        int decodeSize = 0;
        setIsDecoding(true);
        while (isDecoding) {

            AudioData encodeData  = (AudioData)dataList.get();
            if(encodeData == null) {
                //此处可增加一个延时

            }else{
                decodedData = new short[MAX_BUFFER_SIZE];
                //  解码数据的大小
                byte[] raw = encodeData.getencodeData();
                decodeSize=coder.decode(raw, decodedData, encodeData.getSize());
                if (decodeSize > 0)
                {
                    player.addData(decodedData, decodeSize);
                    //decodedData = new short[decodeSize];
                }
            }
        }
        player.stopPlaying();
	}
    
	 public void addData(AudioData data)
	 {
         //将数据添加至解码器缓存区
	        dataList.add(data);
	 }

    //开始解码
	public void startDecoding()
	{
		 if (getIsDecoding())
		 {
	            return;
	     }
        PoolThreadUtil.getInstance().addTask(this);
	}

	//关闭解码器
    public void stopDecoding() 
    {
        setIsDecoding(false);

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

    public synchronized boolean getIsDecoding() {
        return this.isDecoding;
    }

    public synchronized void setIsDecoding(boolean decode) {
        this.isDecoding = decode;
    }
}
