package com.rftransceiver.customviews;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.sounds.Audio_Reciver;

/**
 * Created by rth on 15-7-17.
 */
public class SoundsTextView extends TextView implements View.OnClickListener,Audio_Reciver.PlaySoundsListener{

    private final Audio_Reciver reciver = Audio_Reciver.getInstance();

    public SoundsTextView(Context context) {
        super(context);
    }

    public SoundsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onClick(View view) {
        final String sounds = (String) getTag();
        if(!TextUtils.isEmpty(sounds));
        if(reciver.isReceiving()) {
            if(getContext() != null) {
                Toast.makeText(getContext(),"等待当前播放结束",Toast.LENGTH_SHORT).show();
            }
            return;
        }
        //cache sounds data to auto_receiver
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                byte[] soundsData = Base64.decode(sounds,Base64.DEFAULT);
                int count = soundsData.length / Constants.Small_Sounds_Packet_Length;
                for(int i = 0; i < count;i++) {
                    byte[] subData = new byte[Constants.Small_Sounds_Packet_Length];
                    System.arraycopy(soundsData,i*subData.length,subData,0,subData.length);
                    reciver.cacheData(subData,subData.length);
                    subData = null;
                }
                reciver.setListener(SoundsTextView.this);
                reciver.startWithAutoStop();
            }
        });
    }

    /**
     * callback in Audio_receiver
     */
    @Override
    public void playingStart() {

    }

    /**
     * callback in Audio_receiver
     */
    @Override
    public void playingStop() {
        Log.e("playingStop","auto stop");
        reciver.setListener(null);
    }

}
