package com.rftransceiver.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.datasets.ContactsData;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.PoolThreadUtil;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by rantianhua on 15-6-27.
 */
public class DBManager {

    private DatabaseHelper helper;
    /**
     * to manipulate database
     */
    private SQLiteDatabase db;

    /**
     * the dir to save picture
     */
    private File picDir;

    private static DBManager dbManager;

    private SharedPreferences sp;

    private List<ContentValues> listChats = new ArrayList<>();

    private DBManager(Context context) {
        helper = new DatabaseHelper(context);
        picDir = context.getExternalCacheDir();
        sp = context.getSharedPreferences(Constants.SP_USER,0);
    }

    public static DBManager getInstance(Context context) {
        if(dbManager == null) {
            dbManager = new DBManager(context);
        }
        return dbManager;
    }

    private synchronized void openWriteDB() {
        db = helper.getWritableDatabase();
    }

    private synchronized void openReadDB() {
        db = helper.getReadableDatabase();
    }

    /**
     * save a group into database
     * @param groupEntity a group data
     */
    public void saveGroup(GroupEntity groupEntity) {
        String name = groupEntity.getName();
        byte[] async = groupEntity.getAsyncWord();
        String asyncWord = null;
        if(async != null) {
            asyncWord = Base64.encodeToString(async,Base64.DEFAULT);
        }
        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(asyncWord)) return;
        openWriteDB();
        db.beginTransaction();
        try {
            //save group base info
            db.execSQL("INSERT INTO " + DatabaseHelper.TABLE_GROUP + " VALUES(null,?,?,?)",
                    new Object[]{name,asyncWord,groupEntity.getTempId()});
            //get the latest primary key in group table
            int gid = -1;
            Cursor cursor = db.rawQuery("select last_insert_rowid() from " + DatabaseHelper.TABLE_GROUP,
                    null);
            if(cursor != null && cursor.moveToFirst()) {
                gid = cursor.getInt(0);
                cursor.close();
            }
            //save members into member table
            List<GroupMember> members = groupEntity.getMembers();
            if(gid != -1 && members != null && members.size() > 1) {
                saveCurrentGid(gid);
                for(GroupMember member : members) {
                    //save the picture to local storage
                    Bitmap bitmap = member.getBitmap();
                    String bpath = null;
                    if(bitmap != null) {
                        String picName = System.currentTimeMillis() + ".jpg";
                        File file = new File(picDir,picName);
                        ImageUtil.savePicToLocal(file,bitmap);
                        bpath = file.getAbsolutePath();
                    }
                    String sql = "INSERT INTO " + DatabaseHelper.TABLE_MEMBER + " VALUES(?,?,?,?)";
                    db.execSQL(sql,new Object[]{gid,member.getId(),member.getName(),bpath});
                }
            }
            db.setTransactionSuccessful();
        }catch (Exception e) {
            Log.e("saveGroup","error in save group base info or members info",e);
        }finally {
            db.endTransaction();
            closeDB();
        }
    }
    public void deleteGroup(int gid) {//ɾ�������������id����ɾ��

        try{
            openReadDB();
            db.beginTransaction();
            db.delete(DatabaseHelper.TABLE_DATA,"_gid = ?",new String[]{String.valueOf(gid)});
            db.delete(DatabaseHelper.TABLE_MEMBER,"_gid = ?",new String[]{String.valueOf(gid)});
            db.delete(DatabaseHelper.TABLE_GROUP,"_gid = ?",new String[]{String.valueOf(gid)});
            db.setTransactionSuccessful();
        }catch (Exception e) {
            Log.e("saveGroup","error in save group base info or members info",e);
        }finally {
            db.endTransaction();
            db.close();
            closeDB();
        }
    }

    /**
     * save current shown group id,
     * @param gid
     */
    private void saveCurrentGid(int gid) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(Constants.PRE_GROUP, gid);
        editor.apply();
    }

    /**
     * close the database
     */
    private synchronized void closeDB() {
        if(db != null) {
            try {
                db.close();
            }catch (Exception w) {
                w.printStackTrace();
            }
        }
    }

    public static void close() {
        if(dbManager != null) {
            dbManager.closeDB();
            dbManager.listChats = null;
            dbManager = null;
        }
    }

    /**
     * get a group data by gid
     * @param gid
     * @return
     */
    public GroupEntity getAgroup(int gid) {
        GroupEntity groupEntity = null;
        openReadDB();
        db.beginTransaction();
        try {
            Cursor cursor = db.rawQuery("select * from " + DatabaseHelper.TABLE_GROUP +
                " where _gid=" + gid,null);
            String name = null,async = null;
            int myId = -1;
            if(cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex("_gname"));
                async = cursor.getString(cursor.getColumnIndex("_syncword"));
                myId = cursor.getInt(cursor.getColumnIndex("_myId"));
                cursor.close();
            }
            if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(async)) {
                byte[] asyncword = Base64.decode(async,Base64.DEFAULT);
                groupEntity = new GroupEntity(name,asyncword);
                if(myId != -1) {
                    groupEntity.setTempId(myId);
                }
            }

            if(groupEntity != null) {
                //find all members
                String sql = "select * from " + DatabaseHelper.TABLE_MEMBER
                        + " where _gid=" + gid;
                Cursor cur = db.rawQuery(sql,null);
                if(cur != null) {
                    while (cur.moveToNext()) {
                        GroupMember groupMember = new GroupMember();
                        String data = cur.getString(cur.getColumnIndex("_nickname"));
                        groupMember.setName(data);
                        int mid = cur.getInt(cur.getColumnIndex("_mid"));
                        groupMember.setId(mid);
                        data = cur.getString(cur.getColumnIndex("_photopath"));
                        if(!TextUtils.isEmpty(data)) {
                            Bitmap bitmap = BitmapFactory.decodeFile(data);
                            if(bitmap != null) {
                                groupMember.setBitmap(bitmap);
                                bitmap = null;
                            }
                        }
                        groupEntity.getMembers().add(groupMember);
                    }
                    cur.close();
                }
            }
            db.setTransactionSuccessful();
        }catch (Exception e) {
            Log.e("getAgroup","error in getAgroup",e);
        }finally {
            db.endTransaction();
            closeDB();
        }
        return groupEntity;
    }

    /**
     * save chat data to db
     * @param data
     * @param type 0 is sounds data
     *             1 is text
     *             2 is address
     *             3 is picture
     *             4 is time data
     */
    public void readyMessage(Object data,int type,int memberId,int groupId,long timestamp) {
        ContentValues values = new ContentValues();
        String saveData = null;
        if(type == 3) {
            //save picture to local dir
            String picName = System.currentTimeMillis() + ".jpg";
            try {
                File file = new File(picDir,picName);
                Bitmap bitmap = (Bitmap)data;
                ImageUtil.savePicToLocal(file,bitmap);
                saveData = file.getAbsolutePath();
            }catch (Exception e) {
                ;e.printStackTrace();
            }
        }else {
            saveData = (String) data;
        }
        values.put("_date_time",timestamp);
        values.put("_gid",groupId);
        values.put("_mid",memberId);
        values.put("_type", type);
        values.put("_data", saveData);
        listChats.add(values);

        if(listChats.size() > 9) {
            saveMessage();
        }
    }

    //save data to db
    public void saveMessage() {
        if(listChats.size() == 0) return;
        final List<ContentValues> saveValues = new ArrayList<>();
        saveValues.addAll(listChats);
        listChats.clear();
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                openWriteDB();
                db.beginTransaction();
                try {
                    for (ContentValues values : saveValues) {

                        long re = db.insert(DatabaseHelper.TABLE_DATA, "_data", values);
                    }
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                    closeDB();
                }
            }
        });
    }



    /**
     * get message data saved in db
     * @param gid group id of the data
     * @param limits how many datas get
     */
    public List<ConversationData> getConversationData(int gid,int myId,long timeStamp,int limits) {
        String sql = "select * from " + DatabaseHelper.TABLE_DATA +
                " where _gid=" + gid;
        if(timeStamp > 0) {
            sql += " and _date_time < "+ timeStamp;
        }
        sql += " order by _date_time desc " + "limit " + limits;
        List<ConversationData> conversationDatas = null;
        openReadDB();
        try {
            db.beginTransaction();
            Cursor cursor = db.rawQuery(sql,null);
            conversationDatas = new ArrayList<>();
            int count  = cursor.getCount() -1;
            while (count >= 0) {
                cursor.moveToPosition(count);
                count--;
                int messageId = cursor.getInt(cursor.getColumnIndex("_messageid"));
                int type = cursor.getInt(cursor.getColumnIndex("_type"));
                int mid = cursor.getInt(cursor.getColumnIndex("_mid"));
                String text = cursor.getString(cursor.getColumnIndex("_data"));
                String time = cursor.getString(cursor.getColumnIndex("_date_time"));

                ConversationData data = null;
                ListConversationAdapter.ConversationType conType = null;
                Bitmap bitmapData = null;
                String address = null;
                switch (type) {
                    case 0: //sounds data
                        conType = mid == myId ? ListConversationAdapter.ConversationType.RIGHT_SOUNDS
                                : ListConversationAdapter.ConversationType.LEFT_SOUNDS;
                        break;
                    case 1: //text data
                        conType = mid == myId ? ListConversationAdapter.ConversationType.RIGHT_TEXT
                                : ListConversationAdapter.ConversationType.LEFT_TEXT;
                        break;
                    case 2: //address data
                        conType = mid == myId ? ListConversationAdapter.ConversationType.RIGHT_ADDRESS
                                : ListConversationAdapter.ConversationType.LEFT_ADDRESS;
                        address = text;
                        text = null;
                        break;
                    case 3: //picture data
                        conType = mid == myId ? ListConversationAdapter.ConversationType.RIGHT_PIC
                                : ListConversationAdapter.ConversationType.LEFT_PIC;
                        try {
                            bitmapData = BitmapFactory.decodeFile(text);
                        }catch(Exception e) {

                        }
                        break;
                    case 4:
                        conType = ListConversationAdapter.ConversationType.TIME;
                        break;
                    default:
                        break;
                }

                if(conType == null) break;
                data = new ConversationData(conType);
                data.setDateTime(Long.valueOf(time));
                data.setMid(mid);
                if(bitmapData != null) {
                    data.setBitmap(bitmapData);
                }else if (address != null){
                    data.setAddress(address);
                    address = null;
                }else {
                    data.setContent(text);
                    text = null;
                }
                conversationDatas.add(data);
            }
            cursor.close();
        }catch (Exception e) {
            e.printStackTrace();
            Log.e("getData", "error " ,e);
        }finally {
            db.endTransaction();
            closeDB();
        }
        return conversationDatas;
    }

    public List<ContactsData> getContacts() {
        String sql = "select _gname,_gid from " + DatabaseHelper.TABLE_GROUP;
        openReadDB();
        List<ContactsData> contactsDatas = null;
        try {
            db.beginTransaction();
            Cursor cursor = db.rawQuery(sql,null);
            if(cursor == null) return null;
            if(cursor.getCount() > 0) {
                contactsDatas = new ArrayList<>();
            }
            if(contactsDatas != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    int gid = cursor.getInt(1);
                    ContactsData data = new ContactsData(name,gid);
                    contactsDatas.add(data);
                }
            }
            cursor.close();
            db.setTransactionSuccessful();
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            db.endTransaction();
            closeDB();
        }
        return contactsDatas;
    }
}
