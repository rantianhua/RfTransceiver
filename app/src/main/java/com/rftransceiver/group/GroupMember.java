package com.rftransceiver.group;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupMember {


    private String name;
    private int id;

    public GroupMember(String name,int id) {
        this.id = id;
        this.name = name;
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
}
