package com.rftransceiver.db;

import android.content.Context;

import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.PoolThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/7/26.
 */
public class ListTest {

    public static void saveGroups(final Context context) {
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                for(int i= 0; i < 2;i++) {
                    GroupEntity entity = new GroupEntity("GG"+i,new byte[]{0,1});
                    entity.setPicFilePath("");
                    List<GroupMember> members = new ArrayList<>();
                    for(int j = 0 ;j < 5;j++) {
                        GroupMember member = new GroupMember("hh",i+j);
                        members.add(member);
                    }
                    entity.setMembers(members);
                    try {
                        DBManager.getInstance(context).saveGroup(entity);
                    }catch (Exception e){

                    }

                }
            }
        });
    }
}
