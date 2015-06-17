package com.rftransceiver.customviews;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.rftransceiver.R;


/**
 * Created by rantianhua on 15-6-6.
 */
public class LockerView extends HorizontalScrollView {


    private int menuWidth; //the menu's width
    private int halfMenuWidth;  //the half width of menu width,used to judge open menu or not
    private boolean menuOpened = false; //indicate the menu opened or not
    private int menuPaddingRight = 100; //the default padding is 100dp
    private int screenWidth;
    private boolean show = false;
    private View menu;
    private View content;
    private boolean clickToClose = false;
    private static int contentPaddingTop = 30;

    public LockerView(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        screenWidth = dm.widthPixels;

        menuPaddingRight = (int)(menuPaddingRight * density + 0.5f);

        TypedArray a = context.getTheme().obtainStyledAttributes(attributeSet,
                R.styleable.LockerView, 0, 0);
        try{
            menuPaddingRight = a.getDimensionPixelSize(R.styleable.LockerView_padding_right,
                    menuPaddingRight);
            contentPaddingTop = a.getDimensionPixelSize(R.styleable.LockerView_content_paddingTop,
                    contentPaddingTop);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            a.recycle();
        }

        menuWidth = screenWidth - menuPaddingRight;
        halfMenuWidth = menuWidth / 2;
    }

    /**
     * should calculate a measurement width and height which will be required to render the component.
     * It should try to stay within the specifications passed in, although it can choose to exceed them
     * (in this case, the parent can choose what to do, including clipping, scrolling, throwing an exception,
     * or asking the onMeasure() to try again, perhaps with different measurement specifications).
     * @param widthMeasureSpec the requirements for the restrictions on the width
     * @param heightMeasureSpec the requirements for the restrictions on the height
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //calculate the superior container recommends this viewGroup's width ,
        if(!show) {
            LinearLayout wrapper = (LinearLayout)getChildAt(0);
            if(menu == null) {
                menu = wrapper.getChildAt(0);
            }
            if(content == null) {
                content = wrapper.getChildAt(1);
            }
            menu.getLayoutParams().width = menuWidth;
            content.getLayoutParams().width = screenWidth;
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean b, int i, int i2, int i3, int i4) {
       super.onLayout(b,i,i2,i3,i4);
        if(b) {
            this.scrollTo(menuWidth,0);
            show = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                clickToClose = menuOpened && event.getX() > menuWidth;
                return true;
            case MotionEvent.ACTION_UP:
                if(clickToClose) {
                    clickToClose = false;
                    closeMenu();
                    return true;
                }
                int scrollX = getScrollX();
                if (scrollX > halfMenuWidth) {
                    this.smoothScrollTo(menuWidth, 0);
                    menuOpened = false;
                }
                else {
                    this.smoothScrollTo(0, 0);
                    menuOpened = true;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void closeMenu() {
        if(menuOpened) {
            this.smoothScrollTo(menuWidth,0);
            menuOpened = false;
        }
    }

    public void openMenu() {
        if(!menuOpened) {
            this.smoothScrollTo(0,0);
            menuOpened = true;
        }
    }

    public void toggleMenu() {
        if(menuOpened) {
            closeMenu();
        }else {
            openMenu();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        float scale = l * 1.0f / menuWidth; //0.0~1.0
        ViewAnimator.anim(menu,content,scale);
    }

    /**
     * this static class used to add property animation for menu view and content view when scrolling
     */
    static class ViewAnimator {
        public static void anim(final View m,final View c,final float scale) {
            ValueAnimator animator = ValueAnimator.ofFloat(scale);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = (float)valueAnimator.getAnimatedValue();
                    m.setScaleX(1.0f - 0.3f * scale);
                    m.setScaleY(1.0f - 0.3f * scale);
                    m.setAlpha(0.6f + 0.4f * (1-scale));
                    c.setTranslationY((1-scale) * contentPaddingTop);
                }
            });
            animator.start();
        }
    }
}
