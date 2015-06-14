package com.rftransceiver.util;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import java.util.List;
import java.util.Objects;

/**
 * Created by rantianhua on 15-6-12.
 */
public abstract class BasePopwindow<T> extends PopupWindow {

    private View convertView;
    public Context context;
    public List<T> datas;  //the data set of listview or gridview

    public BasePopwindow(View convertView,int width,int height,boolean focusable) {
        this(convertView,width,height,focusable,null);
    }

    public BasePopwindow(View convertView,int width,int height,boolean focusable,
                         List<T> datas) {
        this(convertView,width,height,focusable,datas,new Object[0]);
    }

    public BasePopwindow(View convertView,int width,int height,
                         boolean focusable,List<T> datas,Object... params) {
        super(convertView,width,height,focusable);
        this.convertView = convertView;
        this.datas = datas;
        this.context = convertView.getContext();

        if(params != null && params.length > 0) {
            initPatrams(params);
        }

        setBackgroundDrawable(new BitmapDrawable());
        setTouchable(true);
        setOutsideTouchable(true);
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initViews();
        iniEvents();
    }

    protected abstract void initPatrams(Object[] params);

    public abstract void initViews();
    public abstract void iniEvents();
    
    public View findViewById(int id) {
        return convertView.findViewById(id);
    }
}
