package com.rftransceiver.customviews;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.sounds.Audio_Reciver;
import com.source.sounds.StaticPlay;

import java.util.zip.Inflater;

/**
 * Created by rth on 15-7-17.
 */
public class SoundsTextView extends TextView implements View.OnClickListener{

    private StaticPlay staticPlay;
    private AnimationDrawable soundPlayAnim;
    private RelativeLayout rl;
    private ImageView soundImg,soundPlay;
    private long soundsTime;
    private float scale;
    //主线程停止soundAnim的runable
    Runnable runable = null;
    private static final Handler mHan = new Handler(Looper.getMainLooper());
    public SoundsTextView(Context context) {
        super(context);
        scale = context.getResources().getDisplayMetrics().density;
    }

    public SoundsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        scale = context.getResources().getDisplayMetrics().density;

    }

    public long getSoundsTime() {
        return soundsTime;
    }

    public void setSoundsTime(long soundsTime) {
        this.soundsTime = soundsTime;
        //声音框长度动态变化
        RelativeLayout.LayoutParams lp =(RelativeLayout.LayoutParams) getLayoutParams();
        if(soundsTime/1000<50) lp.width=(int)((soundsTime/60)+ (scale * 50 + 0.5f));
        setLayoutParams(lp);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        rl = (RelativeLayout)getParent();
        soundImg = (ImageView) rl.findViewById(R.id.img_sound_play);
        soundPlay = (ImageView) rl.findViewById(R.id.img_sound_anim);
    }

    @Override
    public void onClick(View view) {
        final String sounds = (String) getTag();
        if(!TextUtils.isEmpty(sounds));
        staticPlay = StaticPlay.getInstance(this);
        staticPlay.play(sounds);

    }

    /**
     * 显示soundAnim逐帧动画
     */
    public void playSoundAnim(){

        soundPlayAnim = (AnimationDrawable) soundPlay.getBackground();
        soundImg.setVisibility(View.INVISIBLE);
        soundPlayAnim.start();
    }

    /**
     * 停止soundsAnim动画
     */
    public void stopAnim(){
        cancleAnimEnd();
        mHan.post(new Runnable() {
            @Override
            public void run() {
                staticPlay.playEnd();
                soundPlayAnim.stop();
                soundImg.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * 设置动画延时自动关闭
     */
    public  void setAnimEnd() {
        runable = new Runnable() {
            @Override
            public void run() {
                staticPlay.playEnd();
                soundPlayAnim.stop();
                soundImg.setVisibility(View.VISIBLE);
            }
        };
        mHan.postDelayed(runable, soundsTime);
    }

    /**
     * 中断向主线程发送延时自动关闭runable
     */
    public void cancleAnimEnd() {
        if(runable != null)
            mHan.removeCallbacks( runable );
    }
}
