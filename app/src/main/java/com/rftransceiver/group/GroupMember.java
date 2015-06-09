package com.rftransceiver.group;

/**
 * Created by rantianhua on 15-5-30.
 */
public class GroupMember {


    private String name;    //the name of group member
    private String path;    //the path of picture
    private int id; //the member's id in the group

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
