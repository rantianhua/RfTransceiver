package com.rftransceiver.customviews;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

import com.rftransceiver.R;
import com.rftransceiver.util.ImageUtil;

/**
 * Created by rth on 15-7-23.
 */
public class MyListView extends ListView implements AbsListView.OnScrollListener {

    /**
     * header view
     */
    private View header;
    private ImageView loading;

    private AnimationDrawable animationDrawable;

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }


    private void initView(Context context) {
        header = LayoutInflater.from(context).inflate(R.layout.chat_list_header,null);
        loading = (ImageView)header.findViewById(R.id.img_header_list);
        addHeaderView(header);
        setOnScrollListener(this);

        animationDrawable = (AnimationDrawable)loading.getDrawable();
        animationDrawable.start();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {

    }
}
