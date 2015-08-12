package com.rftransceiver.util;

import android.util.Log;

import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.datasets.ConversationData;

import java.util.ArrayList;

/**
 * 该类存储已经显示在界面上却需要被发送至硬件的消息内容
 * Created by wuyang on 2015/8/6.
 */
public class MessageCacheUtil {
    /**
     * 单一示例
     */
    private static  MessageCacheUtil cacheUtil;
    /**
     * 未发送消息的文本内容
     */
    private ArrayList<String> cacheContentList;
    /**
     * 未发送消息的ConversationData
     */
    private ArrayList<ConversationData> cacheDataList;
    /**
     * 未操作消息的文本内容
     */
    private ArrayList<String> unCheckContentList;
    /**
     * 未操作消息的ConversationData
     */
    private ArrayList<ConversationData> unCheckDataList;

    public static MessageCacheUtil getInstance(){
        if(cacheUtil==null) {
            cacheUtil= new MessageCacheUtil();
        }
        return cacheUtil;
    }

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

    public void addUnCheckCacheContent(String content){
        unCheckContentList.add(content);
        unCheckDataList.add(cacheDataList.get(cacheContentList.indexOf(content)));
        cacheDataList.get(cacheContentList.indexOf(content)).reset();
        Log.i("-------uncheck---------",unCheckDataList.size()+"");
    }

    /**
     * 待发送的数据发送失败，变为未发送的数据，即数据从unCheck列表中删除
     */
    public void checkMessage(){
        unCheckContentList.remove(0);
        unCheckDataList.remove(0);
    }
    /**
     * 待发送的数据发送成功，即数据从cache列表以及unCheck列表中删除
     */
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
