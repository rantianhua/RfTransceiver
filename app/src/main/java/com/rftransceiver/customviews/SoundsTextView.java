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
import com.source.sounds.StaticPlay;

/**
 * Created by rth on 15-7-17.
 */
public class SoundsTextView extends TextView implements View.OnClickListener{

    private final Audio_Reciver reciver = Audio_Reciver.getInstance();
    private StaticPlay staticPlay;

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
        staticPlay = StaticPlay.getInstance();
        staticPlay.play(sounds);
    }

}
