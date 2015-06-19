package com.rftransceiver.group;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.rftransceiver.customviews.CircleImageDrawable;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupMember {


    private String name;    //the name of group member
    private String path;    //the path of picture
    private int id; //the member's id in the group
    private Drawable drawable;  //the member's photo
    private Bitmap bitmap;


    public GroupMember(String name,int id) {
        this.id = id;
        this.name = name;
    }

    public GroupMember(String name,int id,String path) {
        this(name,id);
        this.path = path;
    }

    public GroupMember(String name,int id,Bitmap bitmap) {
        this(name,id);
        setBitmap(bitmap);
        setDrawable(bitmap);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Bitmap bitmap) {
        this.drawable = new CircleImageDrawable(bitmap);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
