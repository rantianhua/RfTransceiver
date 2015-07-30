package com.rftransceiver.group;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupEntity implements Parcelable{

    private String name;    //the group's name
    private int groupId;    //this group's id
    private byte[] asyncWord;   //the async word to distinguish different group

    private String picFilePath; //the file path of picture

    private List<GroupMember> members;  //record all of group members

    private int tempId; //every new member's id;

    public GroupEntity() {
        members = new ArrayList<>();
    }

    public void  setGroupId(int gid ){
       this.groupId = gid;
   }
    public int getGroupId(){
        return this.groupId;
    }

    public GroupEntity(String name,byte[] asyncWord) {
        setAsyncWord(asyncWord);
        setName(name);
        members = new ArrayList<>();
    }

    public boolean isRealTimePlay = true;//标识是够进行实时播放

    /**
     * 标识是否保存组的语音信息
     */
    public boolean isSaveSoundOfGroup = true;

    public boolean getIsSaveSoundOfGroup() {
        return this.isSaveSoundOfGroup;
    }

    public void setIsSaveSoundOfGroup(boolean isSaveSoundOfGroup) {
        this.isSaveSoundOfGroup = isSaveSoundOfGroup;
    }

    public boolean getIsRealTimePlay() {
        return this.isRealTimePlay;
    }

    public void setIsRealTimePlay(boolean isRealTimePlay) {
        this.isRealTimePlay = isRealTimePlay;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getAsyncWord() {
        return asyncWord;
    }

    public void setAsyncWord(byte[] asyncWord) {
        this.asyncWord = asyncWord;
    }

    public String getPicFilePath() {
        return picFilePath;
    }

    public void setPicFilePath(String picFilePath) {
        this.picFilePath = picFilePath;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public int getTempId() {
        return tempId;
    }

    public void setTempId(int tempId) {
        this.tempId = tempId;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    /**
     * remove member by member id
     * @param cancelId
     */
    public void removeMemberById(int cancelId) {
        for(int i = 0; i < members.size();i ++) {
            if(members.get(i).getId() == cancelId) {
                members.remove(i);
                Log.e("removeMemberById", "remove " + cancelId);
                break;
            }
        }
    }

    public GroupEntity(Parcel parcel) {
        members = new ArrayList<>();
        name = parcel.readString();
        asyncWord = parcel.createByteArray();
        picFilePath = parcel.readString();
        tempId = parcel.readInt();
        parcel.readTypedList(members,GroupMember.CREATOR);
    }

    public static final Creator<GroupEntity> CREATOR = new Creator<GroupEntity>() {
        @Override
        public GroupEntity createFromParcel(Parcel parcel) {
            return new GroupEntity(parcel);
        }

        @Override
        public GroupEntity[] newArray(int i) {
            return new GroupEntity[i];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeByteArray(this.asyncWord);
        parcel.writeString(this.picFilePath);
        parcel.writeInt(this.tempId);
        parcel.writeTypedList(this.members);
    }
}
