package com.source.sounds;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.rftransceiver.util.PoolThreadUtil;

//¼��PCM������
//��Ҫ����Ȩ�� <uses-permission android:name="android.permission.RECORD_AUDIO" />
//��Ҫ�޸�Audio_Sender��ķ������ݲ���
//һ�������޸ı���
public class Audio_Recorder  implements Runnable
{

    private boolean isRecording = false;   //¼����־
    private AudioRecord audioRecord;    
    
    //¼�Ʋ���ѡ��
    private static final int audioSource = MediaRecorder.AudioSource.MIC;  
    private static final int sampleRate = 8000;   //ȡ����8000hz
    private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;  //������ 
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;    //16λ
    private static final int BUFFER_FRAME_SIZE =160;  
    private int audioBufSize = 0;  
    
    private short[] samples;// ������  
    private int bufferRead = 0;// ��recorder�ж�ȡ��samples�Ĵ�С  
  
    private int bufferSize = 0;// samples�Ĵ�С  
    
    private Audio_Encoder encoder = Audio_Encoder.getInstance();

    public Audio_Recorder() {
    }
    
    public void startRecording()
    {  
        bufferSize = BUFFER_FRAME_SIZE;  
        audioBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                audioFormat);  
        if (audioBufSize == AudioRecord.ERROR_BAD_VALUE) 
        {  
            //do something
            return;  
        }  
        samples = new short[audioBufSize];
        if (null == audioRecord)
        {  
            audioRecord = new AudioRecord(audioSource, sampleRate,  
                    channelConfig, audioFormat, audioBufSize);  
        }
        //start to record data
        PoolThreadUtil.getInstance().addTask(this);
    }
    
    public void run() 
    {
        //start the encoder
        encoder.startEncoding();
        try
        {
        	  audioRecord.startRecording();  
		}
        catch (IllegalStateException e) 
        {
            setRecording(false);
        	return;
		}

        setRecording(true);
        while (isRecording())
        {  
            bufferRead = audioRecord.read(samples, 0, bufferSize);
//            for(int i = 0; i < bufferRead;i++) {
//                samples[i] *= 4;
//            }
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

    public synchronized boolean isRecording() {
        return isRecording;
    }

    public synchronized void setRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public void stopRecording()
    {  
        setRecording(false);
    }

    public void setSoundsEntity(SoundsEntity soundsEntity) {
        encoder.setSoundsEntity(soundsEntity);
    }

}
