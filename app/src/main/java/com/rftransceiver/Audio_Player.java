package com.rftransceiver;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;




//播放解码后的PCM数据类
//一般无需修改本类
public class Audio_Player implements Runnable
{
		private static Audio_Player player;  
		private AudioTrack audioTrack;  
	  
		private List<AudioData> dataList = null;  
		private AudioData playData;  
		private boolean isPlaying = false;  
		
		////解码配置
	    private static final int sampleRate = 8000; 	    
	   
		private static final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
	    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	  
		private Audio_Player() 
		{
            //同步多线程数据
	        dataList = Collections.synchronizedList(new LinkedList<AudioData>());  
	    }



    //获取播放类的实例对象
		public static Audio_Player getInstance()
	    {  
		        if (player == null) 
		        {  
		            player = new Audio_Player();  
		        }  
		        return player;  
	    }

    // 接收解码后的PCM数据
		public void addData(short[] decodedData2, int size) 
		{  
	        AudioData decodedData = new AudioData();  
	        decodedData.setSize(size);  
	  
	        short[] tempData = new short[size];  
	        System.arraycopy(decodedData2, 0, tempData, 0, size);  
	        decodedData.setRealData(tempData);  
	        dataList.add(decodedData);  
	    }

    //启动播放器
		private boolean initAudioTrack() 
		{  
	        int bufferSize = AudioTrack.getMinBufferSize(sampleRate,  
	                channelConfig, audioFormat);  
	        if (bufferSize < 0)
	        {  	            
	            return false;  
	        }  
	        try 
	        {
	        	 audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,  
	 	                channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);  
			} 
	        catch (Exception e) 
	        {
				// TODO: handle exception
			}

            // 设置播放音量
	        audioTrack.setStereoVolume(1.0f, 1.0f);
            //启动播放
	        audioTrack.play();  
	        return true;  
	    }  
		
		
		private void playFromList()
		{
            //取出解码的数据进行播放
	        while (dataList.size() > 0 && isPlaying)
	        {  
	            playData = dataList.remove(0);  
	            audioTrack.write(playData.getRealData(), 0, playData.getSize());  
	        }  
	    }

    //启动播放
		public void startPlaying() 
		{  
	        if (isPlaying) 
	        {  
	            return;  
	        }  
	        new Thread(this).start();  
	    }




    //停止播放
		public void stopPlaying() 
        {  
            this.isPlaying = false;  
        }


    //线程启动执行
		public void run() 
		{
			this.isPlaying = true;  
	          
	        if (!initAudioTrack()) 
	        {              
	            return;  
	        }  
	        while (isPlaying)
	        {  
	            if (dataList.size() > 0) 
	            {  
	                playFromList();  
	            } 
	            else 
	            {  
	                try 
	                {  
	                    Thread.sleep(20);  
	                } 
	                catch (InterruptedException e)
	                {  
	                }  
	            }  
	        }  
	        if (this.audioTrack != null)
	        {
                //停止播放
	            if (this.audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) 
	            {  
	                this.audioTrack.stop();  
	                this.audioTrack.release();  
	            }  
	        }     
			
		 }  
}
