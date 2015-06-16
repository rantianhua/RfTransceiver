package com.rftransceiver.customviews;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationSet;

import com.rftransceiver.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rantianhua on 15-6-16.
 */
public class ArcView extends View {

    private int strokeWidth;
    private int color;
    private int radiu;
    private RectF oval;
    private Paint paint;
    private int offset;

    private AnimatorSet animationSet;

    private List<Animator> list;

    public ArcView(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);

        TypedArray a = context.getTheme().obtainStyledAttributes(attributeSet,
                R.styleable.ArcView, 0, 0);
        try {
            //strokeWidth = a.getDimensionPixelSize(R.styleable.ArcView_stroke_width,10);
            color = a.getColor(R.styleable.ArcView_stroke_color, 0);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            a.recycle();
        }

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(1.2f);
        paint.setStyle(Paint.Style.STROKE);

        oval = new RectF();

        list = new ArrayList<>();
        initAnimation();

        offset = (int)(context.getResources().getDisplayMetrics().density * 4 + 0.5f);
    }

    private void initAnimation() {
        animationSet = new AnimatorSet();
        ObjectAnimator o1 = ObjectAnimator.ofFloat(this,"scaleX",1.0f,8.0f);
        ObjectAnimator o2 = ObjectAnimator.ofFloat(this,"scaleY",1.0f,8.0f);
        o1.setRepeatCount(ObjectAnimator.INFINITE);
        o1.setRepeatMode(ObjectAnimator.RESTART);
        o1.setDuration(3900);
        o2.setRepeatCount(ObjectAnimator.INFINITE);
        o2.setRepeatMode(ObjectAnimator.RESTART);
        o2.setDuration(3900);
        ObjectAnimator o3 = ObjectAnimator.ofFloat(this,"alpha",1.0f,0.0f);
        o3.setRepeatCount(ObjectAnimator.INFINITE);
        o3.setRepeatMode(ObjectAnimator.RESTART);
        o3.setDuration(3900);
        list.add(o1);
        list.add(o2);
        list.add(o3);
        animationSet.playTogether(list);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        oval.set(offset,3,width-offset,height-offset);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(oval,180,180,false,paint);
    }

    public void startRipple(int offset) {
        animationSet.setStartDelay(offset);
        animationSet.start();
    }

    public void stopRipple() {
        animationSet.cancel();
    }

}
