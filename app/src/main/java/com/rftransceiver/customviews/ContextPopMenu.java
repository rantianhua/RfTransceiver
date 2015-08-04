package com.rftransceiver.customviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.rftransceiver.R;


/**
 * Created by rth on 15-7-25.
 */
public class ContextPopMenu extends MyPopuMenu {


    //保存string资源文件中的字符串
    private String textOpenRealSounds, textCloseRealSouds;
    private boolean isRealTimePlaying = true;   //实时播放语音的标识

    @TargetApi(19)
    public ContextPopMenu(Context context, View anchor) {
        super(context, anchor, Gravity.END);
        //获取资源文件的文字信息
        textOpenRealSounds = context.getString(R.string.open_realtime_sounds);
        textCloseRealSouds = context.getString(R.string.close_realtime_sounds);
        initView();
    }

    private void initView() {
        inflate(R.menu.popup_menu);
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_see_group:
                //查看组的详细信息
                if (callbackInContextMenu != null)
                    callbackInContextMenu.showGroup();//回调接口，在HomeFragment中实现该接口
                dismiss();
                break;
            case R.id.action_realtime_play:
                //设置实时播放语音
                updateMenu(item);
                break;
            case R.id.action_exit_group:
                //退出改组
                if(callbackInContextMenu != null) {
                    callbackInContextMenu.exitGroup();
                }
                break;
            case R.id.action_reset:
                //复位
                if(callbackInContextMenu != null) {
                    callbackInContextMenu.reset();
                }
                break;
        }
        return super.onMenuItemSelected(menu, item);
    }

    /**
     * 根据点击事件更新菜单项的显示
     *
     * @param item
     */
    private void updateMenu(MenuItem item) {
        if (isRealTimePlaying) {
            //关闭实时播放语音
            isRealTimePlaying = false;
            item.setTitle(textOpenRealSounds);
            setRealTimePlay(false);
            item.setIcon(R.drawable.open_real_sounds);

        } else {
            //开启语音实时播放
            isRealTimePlaying = true;
            item.setTitle(textCloseRealSouds);
            setRealTimePlay(true);
            item.setIcon(R.drawable.close_real_sounds);

        }
    }

    private CallbackInContextMenu callbackInContextMenu;

    public void setCallBack(CallbackInContextMenu callBack) {
        this.callbackInContextMenu = callBack;
    }

    private void setRealTimePlay(boolean isPlay) {
        if (callbackInContextMenu != null) {
            callbackInContextMenu.isRealTimePlay(isPlay);
        }
    }
    //用于操作HomeFragment--设置groupEntity实例中的实时语音标识
    public interface CallbackInContextMenu {
        void isRealTimePlay(boolean isPlay);
        void showGroup();
        void exitGroup();   //退出该组
        void reset();   //复位
    }
}
