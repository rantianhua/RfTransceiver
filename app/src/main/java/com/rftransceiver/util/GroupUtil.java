package com.rftransceiver.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;

import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.util.List;
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
    private static JSONObject getGroupBaseInfo(GroupEntity groupEntity,Context context) {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put("msg",GROUP_BASEINFO);
            object.put(NAME,groupEntity.getName());
            object.put(GROUP_MEMBER_ID,groupEntity.getTempId());
            object.put(GROUP_ASYNC_WORD,
                    Base64.encodeToString(groupEntity.getAsyncWord(), Base64.DEFAULT));
            Bitmap bitmap = groupEntity.getMembers().get(0).getBitmap();
            if(bitmap != null) {
                object.put(PIC,Base64.encodeToString(getPicBytes(bitmap), Base64.DEFAULT));
                bitmap = null;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }


    private static byte[] getPicBytes(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        return outputStream.toByteArray();
    }

    /**
     *
     * @param path the file path of specific picture
     * @return byte[] data of the picture
     */
    public static byte[] getPicBytes(String path,Context context) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        getSmallBitmap(path,context).compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        return outputStream.toByteArray();
    }

    public static Bitmap getSmallBitmap(String path,Context context) {
        int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                60,context.getResources().getDisplayMetrics());
        size = size * size;
        return ImageUtil.createImageThumbnail(path,size);
    }


    /**
     *
     * @return jsonobject contains member's base info
     */
    private static JSONObject getMemberInfo(Object o, Context context) {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put("msg",MEMBER_BASEINFO);
            String memberName = null;
            byte[] bitmapData = null;
            if(o != null) {
                GroupMember member = (GroupMember) o;
                memberName = member.getName();
                Bitmap bitmap = member.getBitmap();
                if(bitmap != null) {
                    bitmapData = getPicBytes(bitmap);
                }
                object.put(GroupUtil.GROUP_MEMBER_ID,member.getId());
            }else {
                SharedPreferences sp = context.getSharedPreferences(Constants.SP_USER,0);
                memberName = sp.getString(Constants.NICKNAME,"");
                String photoPath = sp.getString(Constants.PHOTO_PATH,"");
                if(!TextUtils.isEmpty(photoPath)) {
                    bitmapData = getPicBytes(photoPath,context);
                }
            }
            object.put(NAME,memberName);
            if(bitmapData != null) {
                object.put(PIC,
                        Base64.encodeToString(bitmapData,Base64.DEFAULT));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     *
     * @return jsonObject contain message to request group base info
     */
    private static JSONObject getRequstGBI() {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put("msg",REQUEST_GBI);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * @return JsonObject contains message to close socket
     */
    private static JSONObject getMsgCS() {
        JSONObject object = null;
        try {
            object = new JSONObject();
            object.put("msg",CLOSE_SOCKET);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * @return JsonObject contains message to cancel create or add group
     */
    private static JSONObject getMsgCancel(Object message) {
        JSONObject object = null;
        try {
            object = new JSONObject();
            if(message instanceof Integer) {
                object.put("msg", CANCEL_ADD);
                object.put(GroupUtil.GROUP_MEMBER_ID,(int)message);
            }else if(message instanceof String) {
                object.put("msg", CANCEL_CREATE);
                object.put(GROUP_SSID,message);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     *
     * @param type the type of data to be sent
     * @param o the message need to be send
     * @return jsonobject of the send data
     */
    public static String getWriteData(String type,Object o,Context context) {
        switch (type) {
            case GROUP_BASEINFO:
                return getGroupBaseInfo((GroupEntity)o,context).toString();
            case MEMBER_BASEINFO:
                return getMemberInfo(o, context).toString();
            case REQUEST_GBI:
                return getRequstGBI().toString();
            case CLOSE_SOCKET:
                return getMsgCS().toString();
            case CANCEL_ADD:
                return getMsgCancel(o).toString();
            case CANCEL_CREATE:
                return getMsgCancel(o).toString();
            case GROUP_FULL_INFO:
                return getGroupFullInfo(o,context);
        }
        return null;
    }

    /**
     *
     * @param o
     * @param context
     * @return full info of a created group
     */
    private static String getGroupFullInfo(Object o, Context context) {
        GroupEntity entity = (GroupEntity)o;
        List<GroupMember> members = entity.getMembers();
        JSONArray jsonArray = null;
        JSONObject data = null;
        try{
            jsonArray = new JSONArray();
            for(int i = 0;i < members.size();i++) {
                JSONObject object = getMemberInfo(members.get(i),context);
                jsonArray.put(object);
            }
            data = new JSONObject();
            data.put("msg",jsonArray);
        }catch (Exception e ){
            e.printStackTrace();
        }
        return data == null ? null : data.toString();
    }

    /**
     * recycle add unused bitmap
     * @param list
     */
    public static void recycle(List<GroupMember> list) {
        for(int i = 0; i < list.size();i++) {
            Bitmap bitmap = list.get(i).getBitmap();
            if(bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    public static final String NAME = "name";   //the key of group's or member's name
    public static final String PIC = "pic";     //the key of group owner's or member's picture
    public static final String GROUP_MEMBER_ID = "memberId";    //the key of group member's id
    public static final String GROUP_ASYNC_WORD = "async_word"; //the key of group's async word
    public static final String GROUP_SSID = "ssid"; //the key of group's async word

    public static final String REQUEST_GBI = "request_base_info"; //the value of msg,to rewuest group base info
    public static final String GROUP_BASEINFO = "group_base_info"; //the value of msg
    public static final String MEMBER_BASEINFO = "member_base_info"; //the value of msg
    public static final String CLOSE_SOCKET = "close_socket"; //the value of msg
    public static final String CANCEL_ADD = "cancel_add";   //cancel add group
    public static final String CANCEL_CREATE = "cancel_create";   //cancel create group
    public static final String GROUP_FULL_INFO = "group_full_info";   //cancel create group

}
