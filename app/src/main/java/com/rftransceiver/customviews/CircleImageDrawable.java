package com.rftransceiver.customviews;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Created by rantianhua on 15-6-10.
 */
public class CircleImageDrawable extends Drawable {

    private Paint paint;   //the paint to draw bitmap
    private int width;     //the min width of bitmap
    private Bitmap bitmap;

    public CircleImageDrawable(Bitmap bitmap)
    {
        this.bitmap = bitmap ;
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(bitmapShader);
        width = Math.min(this.bitmap.getWidth(), this.bitmap.getHeight());
    }

    @Override
    public void draw(Canvas canvas)
    {
        canvas.drawCircle(width / 2, width / 2, width / 2, paint);
    }

    @Override
    public int getIntrinsicWidth()
    {
        return width;
    }

    @Override
    public int getIntrinsicHeight()
    {
        return width;
    }

    @Override
    public void setAlpha(int alpha)
    {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf)
    {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }
}
