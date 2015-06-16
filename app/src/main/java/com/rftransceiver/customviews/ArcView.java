package com.rftransceiver.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.rftransceiver.R;

/**
 * Created by rantianhua on 15-6-16.
 */
public class ArcView extends View {

    private int strokeWidth;
    private int color;
    private int radiu;
    private RectF oval;
    private Paint paint;

    public ArcView(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);

        TypedArray a = context.getTheme().obtainStyledAttributes(attributeSet,
                R.styleable.ArcView, 0, 0);
        try {
            strokeWidth = a.getDimensionPixelSize(R.styleable.ArcView_stroke_width,10);
            color = a.getColor(R.styleable.ArcView_stroke_color, Color.parseColor("#FFF"));
        }catch (Exception e) {
            e.printStackTrace();
        }

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);

        oval = new RectF();
        oval.left = 0;
        oval.top = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        oval.right = getWidth();
        oval.bottom = getHeight();
        canvas.drawArc(oval,0,180,false,paint);
    }
}
