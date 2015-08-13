package com.rftransceiver.util;

import android.util.Log;

import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.datasets.ConversationData;

import java.util.ArrayList;

/**
 * 缓存发送失败的数据
 * Created by wuyang on 2015/8/6.
 */
public class MessageCacheUtil {
    /**
     * 单一实例
     */
    private static  MessageCacheUtil cacheUtil;
    /**
     * 未发送的内容文本
     */
    private ArrayList<String> cacheContentList;
    /**
     * 未发送的ConversationData
     */
    private ArrayList<ConversationData> cacheDataList;
    /**
     * 未操作的内容文本，用于自动发送
     */
    private ArrayList<String> unCheckContentList;
    /**
     * 未操作的ConversationData，用于自动发送
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
     * 操作数据，将数据从未操作的里表中移除，用在自动发送的动作里
     */
    public void checkMessage(){
        unCheckContentList.remove(0);
        unCheckDataList.remove(0);
    }
    /**
     * 移除数据缓存，用在发送成功的动作里
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
