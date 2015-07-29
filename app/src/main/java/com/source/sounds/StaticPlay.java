package com.source.sounds;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Base64;
import android.util.Log;

import com.audio.Speex;
import com.rftransceiver.customviews.SoundsTextView;
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

    private SoundsTextView preTv;   //记录上一次播放的视图
    private SoundsTextView currentTv;   //记录当前要播放动画的视图

    //speex库
    private Speex coder;

    //解码数据的容器
    private short[] decodedData;

    //缓存每一个解码包
    private List<short[]> decodeDataSet;

    //解码包的大小
    private int decodeSize;
    //标识正在播放语音
    private boolean isPlaying = false;

    private AudioTrack audioTrack;

    private StaticPlay() {
        coder=new Speex();
        decodeDataSet = new ArrayList<>();
    }

    public static StaticPlay getInstance(SoundsTextView tv) {
        if(staticPlay == null) {
            staticPlay = new StaticPlay();
        }
        staticPlay.setSoundsTextView(tv);
        return staticPlay;
    }

    public void setSoundsTextView(SoundsTextView tv) {
        if(currentTv != null) {
            preTv = currentTv;
        }
        currentTv  = tv;
    }

    public String getSoudsData() {
        return soudsData;
    }

    public void setSoudsData(String soudsData) {
        this.soudsData = soudsData;
    }

    public synchronized boolean isPlaying() {
        return isPlaying;
    }

    public synchronized void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    @Override
    public void run() {
        if(getSoudsData() == null) return;
        coder.init();
        byte[] soundsData = Base64.decode(getSoudsData(), Base64.DEFAULT);
        int count = soundsData.length / Constants.Small_Sounds_Packet_Length;
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
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,
                channelConfig, audioFormat, decodeData.length * 2, AudioTrack.MODE_STATIC);
        audioTrack.write(decodeData,0,decodeData.length);
        audioTrack.play();
        setIsPlaying(true);
        decodeDataSet.clear();
    }

    /**
     * 播放语音
     * @param sounds
     */
    public void play(String sounds) {
        if(preTv!=currentTv){
            stop();
            //重置音频
            setSoudsData(sounds);
            PoolThreadUtil.getInstance().addTask(StaticPlay.this);
            currentTv.playSoundAnim();
            currentTv.setAnimEnd();
            setIsPlaying(true);
        }else {
            if(staticPlay.isPlaying()) {
                stop();
                currentTv.stopAnim();
            }else {
//                stop();
                //重置音频
                setSoudsData(sounds);
                PoolThreadUtil.getInstance().addTask(StaticPlay.this);
                currentTv.playSoundAnim();
                currentTv.setAnimEnd();
                setIsPlaying(true);
            }
        }
    }

    /**
     * 控制播放状态，设置为false
     */
    public void playEnd() {
        setIsPlaying(false);
    }

    /**
     * 停止播放语音
     */
    public void stop(){
        if(audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
            if(preTv != null) {
                setIsPlaying(false);
                preTv.stopAnim();
                audioTrack.stop();
                //将未播放完的数据释放
                audioTrack.release();
            }
    }

}
