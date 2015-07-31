package com.rftransceiver.datasets;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;

import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.customviews.CircleImageDrawable;

/**
 * Created by Rth on 2015/4/27.
 */
public class ConversationData {

    /**
     * the content of conversation data
     */
    private String content;

    /**
     * type : me or other
     */
    private ListConversationAdapter.ConversationType conversationType;

    /**
     * the other people's photo
     */
    private Drawable photoDrawable;

    /**
     * the address data
     */
    private String address;

    /**
     * picture data
     */
    private Bitmap bitmap;

    /**
     * the send or receive time
     */
    private long dateTime;

    /**
     * my id in group
     */
    private int mid;
    private long soundsTime;
    // 发送图片的进度
    private int percent;

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
}
