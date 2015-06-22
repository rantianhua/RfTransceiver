package com.rftransceiver.group;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.util.GroupUtil;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupMember implements Parcelable{


    private String name;    //the name of group member
    private String path;    //the path of picture
    private int id; //the member's id in the group
    private Drawable drawable;  //the member's photo
    private Bitmap bitmap;

    public GroupMember() {

    }

    public GroupMember(String name,int id) {
        this.id = id;
        this.name = name;
    }

    public GroupMember(String name,int id,String path) {
        this(name,id);
        this.path = path;
    }

    public GroupMember(String name,int id,Bitmap bitmap) {
        this(name, id);
        setBitmap(bitmap);
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
        setDrawable(bitmap);
    }

    public static final Creator<GroupMember> CREATOR = new Creator<GroupMember>() {
        @Override
        public GroupMember createFromParcel(Parcel parcel) {
            GroupMember member = new GroupMember();
            member.setId(parcel.readInt());
            member.setName(parcel.readString());
            member.setPath(parcel.readString());
            member.setBitmap((Bitmap)parcel.readBundle().getParcelable(BITMAP));
            return member;
        }

        @Override
        public GroupMember[] newArray(int i) {
            return new GroupMember[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeString(path);
        Bundle bundle = new Bundle();
        if(bitmap == null) {
            Log.e("writeToParcel", "bitmap is null");
        }
        if(bitmap.isRecycled()) {
            Log.e("writeToParcel", "bitmap is recycled");
        }
        bundle.putParcelable(BITMAP,bitmap);
        parcel.writeBundle(bundle);
    }

    public static final String BITMAP = "bitmap";

}
