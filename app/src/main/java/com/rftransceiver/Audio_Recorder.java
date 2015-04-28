package com.rftransceiver;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

//录制PCM数据类
//需要加入权限 <uses-permission android:name="android.permission.RECORD_AUDIO" />
//主要修改Audio_Sender类的发送数据操作
//一般无需修改本类
public class Audio_Recorder  implements Runnable
{

    private volatile boolean isRecording = false;   //录音标志
    private AudioRecord audioRecord;    
    
    //录制参数选项
    private static final int audioSource = MediaRecorder.AudioSource.MIC;  
    private static final int sampleRate = 8000;   //取样率8000hz
    private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;  //单声道 
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;    //16位
    private static final int BUFFER_FRAME_SIZE =160;  
    private int audioBufSize = 0;  
    
    private short[] samples;// 缓冲区  
    private int bufferRead = 0;// 从recorder中读取的samples的大小  
  
    private int bufferSize = 0;// samples的大小  
    
    
    
  
    public void startRecording()
    {  
        bufferSize = BUFFER_FRAME_SIZE;  
       //获取最小缓冲区
        audioBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,  
                audioFormat);  
        if (audioBufSize == AudioRecord.ERROR_BAD_VALUE) 
        {  
            //do something
            return;  
        }  
        //初始化采样缓冲区
        samples = new short[audioBufSize];  
        // 初始化recorder  
        if (null == audioRecord)
        {  
            audioRecord = new AudioRecord(audioSource, sampleRate,  
                    channelConfig, audioFormat, audioBufSize);  
        }  
        //启动线程中的run方法
        new Thread(this).start();  
    }  
    
    public void run() 
    {
        //start the encoder
        Audio_Encoder encoder = Audio_Encoder.getInstance();
        encoder.startEncoding();     
        try 
        {
        	  audioRecord.startRecording();  
		}
        catch (IllegalStateException e) 
        {
        	this.isRecording = false; 
        	return;
		}
      
  
        this.isRecording = true;  
        while (isRecording) 
        {  
            bufferRead = audioRecord.read(samples, 0, bufferSize);  
            if (bufferRead > 0) 
            {  
                // add the data to the encoder
                encoder.addData(samples, bufferRead);
            }
            try
            {  
                Thread.sleep(20);  
            } catch (InterruptedException e) 
            {  
                e.printStackTrace();  
            }  
        }  
        audioRecord.stop();
        encoder.stopEncoding();
    }
    
    public void stopRecording() 
    {  
        this.isRecording = false;  
    }  
  
    public boolean isRecording()
    {  
        return isRecording;  
    }  
    
}
