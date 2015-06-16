package com.rftransceiver.group;

import android.graphics.Bitmap;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupMember {


    private String name;    //the name of group member
    private String path;    //the path of picture
    private int id; //the member's id in the group
    private Bitmap bitmap;  //the member's photo


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
        this.bitmap = bitmap;
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

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
