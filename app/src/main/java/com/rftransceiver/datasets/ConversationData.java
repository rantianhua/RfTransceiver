package com.rftransceiver.datasets;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

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
     * use a drawable to show Signal intensity,
     * this id the id of the drawable
     */
    private int levelId = R.drawable.level1;

    /**
     * the instance between two persons
     */
    private String instance;

    /**
     * the address data
     */
    private String address;

    private MainActivity.SendAction dataType;

    public ConversationData(ListConversationAdapter.ConversationType type,
                            String text) {
        setConversationType(type);
        setContent(text);
    }

    public ConversationData(ListConversationAdapter.ConversationType type,
                            String text,Bitmap bitmap,int levelId,String instance) {
        this(type,text);
        setPhotoDrawable(bitmap);
        setLevelId(levelId);
        setInstance(instance);
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

    public void setPhotoDrawable(Bitmap bitmap) {
        this.photoDrawable = new CircleImageDrawable(bitmap);
    }

    public int getLevelId() {
        return levelId;
    }

    public void setLevelId(int levelId) {
        if(levelId != 0) this.levelId = levelId;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public MainActivity.SendAction getDataType() {
        return dataType;
    }

    public void setDataType(MainActivity.SendAction dataType) {
        this.dataType = dataType;
    }
}
