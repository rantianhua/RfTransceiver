package com.rftransceiver.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.R;

/**
 * Created by rantianhua on 15-6-15.
 */
public class GroupMemberView extends ViewGroup {

    private TextView tv;
    private ImageView img;
    
    private int radiu;
    private int tvWidth;
    private int tvHeight;
    private String text;
    private int textColor;
    private int textSize;
    private int textBg;
    private Drawable src;

    //private


    public GroupMemberView(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);

        //create a TextView and a ImageView
        tv = new TextView(context);
        img = new ImageView(context);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;

        TypedArray a = context.getTheme().obtainStyledAttributes(attributeSet,
                R.styleable.GroupMemberView, 0, 0);
        try{
            radiu = a.getDimensionPixelSize(R.styleable.GroupMemberView_image_radiu,60);
            tvWidth = a.getDimensionPixelSize(R.styleable.GroupMemberView_tv_width,60);
            tvHeight = a.getDimensionPixelSize(R.styleable.GroupMemberView_tv_height,20);
            text = a.getString(R.styleable.GroupMemberView_text);
            textColor = a.getColor(R.styleable.GroupMemberView_text_color, Color.parseColor("#65341283"));
            textSize = a.getDimensionPixelSize(R.styleable.GroupMemberView_text_size,20);
            src = a.getDrawable(R.styleable.GroupMemberView_src);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            a.recycle();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }

    @Override
    protected void onLayout(boolean b, int i, int i2, int i3, int i4) {

    }
}
