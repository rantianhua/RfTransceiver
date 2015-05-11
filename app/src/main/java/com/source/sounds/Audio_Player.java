package com.source.sounds;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.rftransceiver.datasets.AudioData;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;




//���Ž�����PCM������
//һ�������޸ı���
public class Audio_Player implements Runnable
{
		private static Audio_Player player;  
		private AudioTrack audioTrack;  
	  
		private List<AudioData> dataList = null;
		private AudioData playData;  
		private boolean isPlaying = false;  
		
		////��������
	    private static final int sampleRate = 8000; 	    
	   
		private static final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
	    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	  
		private Audio_Player() 
		{
            //ͬ�����߳�����
	        dataList = Collections.synchronizedList(new LinkedList<AudioData>());  
	    }



    //��ȡ�������ʵ������
		public static Audio_Player getInstance()
	    {  
		        if (player == null) 
		        {  
		            player = new Audio_Player();  
		        }  
		        return player;  
	    }

    // ���ս�����PCM����
		public void addData(short[] decodedData2, int size) 
		{  
	        AudioData decodedData = new AudioData();  
	        decodedData.setSize(size);  
	  
	        short[] tempData = new short[size];  
	        System.arraycopy(decodedData2, 0, tempData, 0, size);  
	        decodedData.setRealData(tempData);  
	        dataList.add(decodedData);  
	    }

    //����������
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

            // ���ò�������
	        audioTrack.setStereoVolume(1.0f, 1.0f);
            //��������
	        audioTrack.play();  
	        return true;  
	    }  
		
		
		private void playFromList()
		{
            //ȡ����������ݽ��в���
	        while (dataList.size() > 0 && isPlaying)
	        {  
	            playData = dataList.remove(0);  
	            audioTrack.write(playData.getRealData(), 0, playData.getSize());  
	        }  
	    }

    //��������
		public void startPlaying() 
		{  
	        if (isPlaying) 
	        {  
	            return;  
	        }  
	        new Thread(this).start();  
	    }




    //ֹͣ����
		public void stopPlaying() 
        {  
            this.isPlaying = false;  
        }


    //�߳�����ִ��
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
                //ֹͣ����
	            if (this.audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) 
	            {  
	                this.audioTrack.stop();  
	                this.audioTrack.release();  
	            }  
	        }     
			
		 }  
}
