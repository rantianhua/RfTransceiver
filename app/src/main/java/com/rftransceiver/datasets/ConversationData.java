package com.rftransceiver.datasets;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.baidu.mapapi.map.MapFragment;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.fragments.MapViewFragment;

/**
 * Created by Rth on 2015/4/27.
 */
public class ConversationData {

    //数据内容
    private String content;

    //数据类型
    private ListConversationAdapter.ConversationType conversationType;

    //发送者的图片
    private Drawable photoDrawable;

    //地址信息
    private String address;

    //图片内容
    private Bitmap bitmap;

    //数据产生的时间
    private long dateTime;
    //我在组里id
    private int mid;
    //声音的时长
    private long soundsTime;

    //发送图片的进度
    private int percent;

    //显示地图信息的Fragment
    private MapViewFragment mapFragment;

    public ConversationData(ListConversationAdapter.ConversationType type) {
        setConversationType(type);
    }

    public ConversationData(ListConversationAdapter.ConversationType type,
                            String text) {
        this(type);
        setContent(text);
    }

    public ConversationData(ListConversationAdapter.ConversationType type,
                            String text,long soundsTime) {
        this(type);
        setContent(text);
        setSoundsTime(soundsTime);
    }


    public ConversationData(ListConversationAdapter.ConversationType type,
                            String text,Bitmap bitmap) {
        this(type, text);
        if(bitmap != null) {
            setPhotoDrawable(bitmap);
        }
    }


    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public ListConversationAdapter.ConversationType getConversationType() {
        return conversationType;
    }

    public void setConversationType(ListConversationAdapter.ConversationType conversationType) {
        this.conversationType = conversationType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Drawable getPhotoDrawable() {
        return photoDrawable;
    }

    public void setSoundsTime(long time){
        this.soundsTime=time;
    }

    public long getSoundsTime(){
        return soundsTime;
    }

    public void setPhotoDrawable(Bitmap bitmap) {
        this.bitmap = bitmap;
        if(bitmap != null) {
            this.photoDrawable = new CircleImageDrawable(bitmap);
        }
    }

    public void setPhotoDrawable(Drawable drawable) {
        this.photoDrawable = drawable;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        mapFragment = MapViewFragment.getInstance(address);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public MapViewFragment getMapFragment() {
        return mapFragment;
    }
}
