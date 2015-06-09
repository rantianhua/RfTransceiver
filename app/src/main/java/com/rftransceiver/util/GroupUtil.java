package com.rftransceiver.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * Created by rantianhua on 15-6-4.
 */
public class GroupUtil {

    /**
     * @return a asyncWord to set scm
     */
    public static byte[] createAsynWord() {
        Random random = new Random(System.currentTimeMillis());
        byte[] word = new byte[2];
        random.nextBytes(word);
        random = null;
        return word;
    }

    /**
     *
     * @param groupEntity   the Group's description
     * @return the jsonobject contain group's base info
     */
    private static JSONObject getGroupBaseInfo(GroupEntity groupEntity) {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put(NAME,groupEntity.getName());
            byte[] async = groupEntity.getAsyncWord();
            String asyncWord = async[0] + "|" + async[1];
            object.put(GROUP_ASYNC_WORD,asyncWord);
            asyncWord = null;
            String path = groupEntity.getPicFilePath();
            if(path != null) {
               object.put(PIC,getPicBytes(path));
                path = null;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     *
     * @param path the file path of specific picture
     * @return byte[] data of the picture
     */
    private static byte[] getPicBytes(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        bitmap.recycle();
        return outputStream.toByteArray();
    }


    /**
     *
     * @param member the group member object
     * @return jsonobject contains member's base info
     */
    private static JSONObject getMemberBaseInfo(GroupMember member) {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put(NAME,member.getName());
            String path = member.getPath();
            if(path != null){
                object.put(PIC,getPicBytes(path));
                path = null;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     *
     * @param id the group owner distribute a id to every member
     * @return jsonobject contains member's id
     */
    private static JSONObject getMemberId(int id) {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put(GROUP_MEMBER_ID,id);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    public static JSONObject getWriteData(WriteDataType type,Object o) {
        switch (type) {
            case GROUP_BASEINFO:
                return getGroupBaseInfo((GroupEntity)o);
            case MEMBER_BASEINFO:
                return getMemberBaseInfo((GroupMember)o);
            case MEMBER_ID:
                return getMemberId((int)o);
        }
        return null;
    }

    public enum WriteDataType {
        GROUP_BASEINFO,
        MEMBER_BASEINFO,
        MEMBER_ID
    }

    public static final String NAME = "name";   //the key of group owner's or member's name
    public static final String PIC = "pic";     //the key of group owner's or member's picture
    public static final String GROUP_MEMBER_ID = "memberId";    //the key of group member's id
    public static final String GROUP_ASYNC_WORD = "async_word"; //the key of group's async word

}
