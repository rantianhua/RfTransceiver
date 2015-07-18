package com.source.sounds;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.provider.MediaStore;
import android.util.Base64;

import com.audio.Speex;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rth on 15-7-18.
 * 静态播放语音数据
 */
public class StaticPlay implements Runnable{

    private static StaticPlay staticPlay;

    private String soudsData;

    private int sampleRate = 8000;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    //speex库
    private Speex coder;

    //解码数据的容器
    private short[] decodedData;

    //缓存每一个解码包
    private List<short[]> decodeDataSet;

    //解码包的大小
    private int decodeSize;

    private StaticPlay() {
        coder=new Speex();
        decodeDataSet = new ArrayList<>();
    }

    public static StaticPlay getInstance() {
        if(staticPlay == null) {
            staticPlay = new StaticPlay();
        }
        return staticPlay;
    }

    public String getSoudsData() {
        return soudsData;
    }

    public void setSoudsData(String soudsData) {
        this.soudsData = soudsData;
    }

    @Override
    public void run() {
        if(getSoudsData() == null) return;
        coder.init();
        byte[] soundsData = Base64.decode(getSoudsData(), Base64.DEFAULT);
        int count = soundsData.length / Constants.Small_Sounds_Packet_Length;
        int decodeLen = 0;
        for(int i = 0; i < count;i++) {
            byte[] subData = new byte[Constants.Small_Sounds_Packet_Length];
            System.arraycopy(soundsData, i * subData.length, subData, 0, subData.length);
            //decode data
            decodedData = new short[256];
            decodeSize = coder.decode(subData, decodedData, subData.length);
            decodeDataSet.add(decodedData);
            subData = null;
        }
        int index = 0;
        short[] decodeData = new short[decodeDataSet.size() * decodeSize];
        for(short[] dd : decodeDataSet) {
            System.arraycopy(dd,0,decodeData,index,decodeSize);
            index += decodeSize;
        }
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,
                channelConfig, audioFormat, decodeData.length * 2, AudioTrack.MODE_STATIC);
        audioTrack.write(decodeData,0,decodeData.length);
        audioTrack.play();
        decodeDataSet.clear();
    }

    public void play(String sounds) {
        setSoudsData(sounds);
        PoolThreadUtil.getInstance().addTask(StaticPlay.this);
    }
}
