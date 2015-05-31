package com.rftransceiver.group;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupEntity {

    private String name;    //the group's name

    private byte[] asyncWord;   //the async word to distinguish different group

    private List<GroupMember> members;

    public GroupEntity(String name,byte[] asyncWord) {
        this.asyncWord = asyncWord;
        this.name = name;
        members = new ArrayList<>();
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
}
