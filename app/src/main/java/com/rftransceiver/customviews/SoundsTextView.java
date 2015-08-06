package com.rftransceiver.customviews;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rftransceiver.R;
import com.source.sounds.StaticPlay;


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
    private float width;

    //���߳�ֹͣsoundAnim��runable
    Runnable runable = null;
    private static final Handler mHan = new Handler(Looper.getMainLooper());
    public SoundsTextView(Context context) {
        super(context);
        scale = context.getResources().getDisplayMetrics().density;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        width = wm.getDefaultDisplay().getWidth();
    }

    public SoundsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        scale = context.getResources().getDisplayMetrics().density;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        width = wm.getDefaultDisplay().getWidth();
    }

    public long getSoundsTime() {
        return soundsTime;
    }

    public void setSoundsTime(long soundsTime) {
        this.soundsTime = soundsTime;
        //�����򳤶ȶ�̬�仯
        RelativeLayout.LayoutParams lp =(RelativeLayout.LayoutParams) getLayoutParams();
        if(((soundsTime/60)+ (scale * 50 + 0.5f)) < width - scale * 50 +0.5f)
            lp.width=(int)((soundsTime/60)+ (scale * 50 + 0.5f));
        else
            lp.width=(int)(width - scale * 50 +0.5f);

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
     * ��ʾsoundAnim��֡����
     */
    public void playSoundAnim(){

        soundPlayAnim = (AnimationDrawable) soundPlay.getBackground();
        soundImg.setVisibility(View.INVISIBLE);
        soundPlayAnim.start();
    }

    /**
     * ֹͣsoundsAnim����
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
     * ���ö�����ʱ�Զ��ر�
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
     * �ж������̷߳�����ʱ�Զ��ر�runable
     */
    public void cancleAnimEnd() {
        if(runable != null)
            mHan.removeCallbacks( runable );
    }
}
