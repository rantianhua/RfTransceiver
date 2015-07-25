package com.rftransceiver.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.util.BasePopwindow;

import org.w3c.dom.Text;

/**
 * Created by rth on 15-7-25.
 */
public class ContextPopMenu extends PopupWindow {

    private TextView tvSeeGroup;    //点击查看组信息
    private TextView tvRealSounds;  //点击设置实时播放语音
    private ImageView imgRealSounds;    //显示是否在实时播放语音

    private int screenHeight;   //屏幕宽度
    private int popWidth;

    //保存string资源文件中的字符串
    private String textOpenRealSounds,textCloseRealSouds;

    public ContextPopMenu(Context context) {
        super(context);
        //设置popwindow的宽高
        textOpenRealSounds = context.getString(R.string.open_realtime_sounds);
        textCloseRealSouds = context.getString(R.string.close_realtime_sounds);

        screenHeight = context.getResources().getDisplayMetrics().widthPixels;

        popWidth = context.getResources().getDimensionPixelSize(R.dimen.popmenu_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.popmenu_width);
        setHeight(height);
        setWidth(popWidth);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.popmenu_view, null);

        tvRealSounds = (TextView) view.findViewById(R.id.tv_popmenu_real_time);
        tvSeeGroup = (TextView) view.findViewById(R.id.tv_popmenu_seegroup);
        imgRealSounds = (ImageView) view.findViewById(R.id.img_popmenu_group);

        initEvent();
        setContentView(view);
        setBackgroundDrawable(null);
    }

    /**
     * 设置动作事件
     */
    private void initEvent() {
        tvRealSounds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        tvSeeGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    /**
     * 根据传入的参考 view 设置popwindow的显示
     * @param anchor
     */
    public void show(View anchor) {
        showAsDropDown(anchor,screenHeight-popWidth,0);
        View parent = (View)anchor.getParent();
        if(parent != null) {
            parent.setAlpha(0.6f);
        }
    }

}
