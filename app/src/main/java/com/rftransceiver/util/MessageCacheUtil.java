package com.rftransceiver.util;

import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.datasets.ConversationData;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015/8/6.
 */
public class MessageCacheUtil {

    private ArrayList<String> cacheContentList;
    private ArrayList<ConversationData> cacheDataList;
    private ArrayList<String> unCheckContentList;
    private ArrayList<ConversationData> unCheckDataList;

    public MessageCacheUtil(){
        cacheContentList=new ArrayList<String>();
        cacheDataList = new ArrayList<ConversationData>();
        unCheckContentList = new ArrayList<String>();
        unCheckDataList = new ArrayList<ConversationData>();
    }

    public ArrayList<String> getCacheContentList(){
        return cacheContentList;
    }

    public ArrayList<ConversationData> getUnCheckDataList(){
        return unCheckDataList;
    }

    public ArrayList<String> getUnCheckContentList(){
        return unCheckContentList;
    }

    public ArrayList<ConversationData> getCacheDataList(){
        return cacheDataList;
    }

    public void addCache(String content,ConversationData data){
        cacheContentList.add(content);
        cacheDataList.add(data);
        unCheckContentList.add(content);
        unCheckDataList.add(data);
    }

    public void checkMessage(){
        unCheckContentList.remove(0);
        unCheckDataList.remove(0);
    }

    public void removeCache(ConversationData data){
        cacheContentList.remove(cacheDataList.indexOf(data));
        cacheDataList.remove(data);
        unCheckContentList.remove(unCheckDataList.indexOf(data));
        unCheckDataList.remove(data);
    }

    public int getUnCheckNum(){
        return unCheckContentList.size();
    }

    public int getCacheNum(){
        return cacheContentList.size();
    }
}
