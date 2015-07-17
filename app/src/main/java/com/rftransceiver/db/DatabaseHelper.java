package com.rftransceiver.db;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.rftransceiver.datasets.ConversationData;

/**
 * Created by rantianhua on 15-6-27.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * the version of database
     */
    public static final int VERSION = 1;

    /**
     * the name of database
     */
    public static final String DB_NAME = "INTERPHONE.db";

    /**
     * there are three table
     * a group table to save different group
     * a member table to save different members in different groups
     * a data table to save different datas in different groups
     */
    public static String TABLE_GROUP = "my_group";
    public static String TABLE_MEMBER = "member";
    public static String TABLE_DATA = "data";


    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    public DatabaseHelper(Context context) {
        this(context,DB_NAME,null,VERSION);
    }

    @Override
    public String getDatabaseName() {
        return super.getDatabaseName();
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        /**
         * call when the database is created the first time
         */
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(TABLE_GROUP).append("(")
            .append("_gid INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append("_gname TEXT NOT NULL, ").append("_syncword TEXT NOT NULL, ")
                .append("_myId INTEGER")
                .append(");");
        sqLiteDatabase.execSQL(sb.toString());
        sb.delete(0, sb.length());

        sb.append("CREATE TABLE ").append(TABLE_MEMBER).append("(")
                .append("_gid INTEGER, ").append("_mid INTEGER, ")
                .append("_nickname TEXT, ").append("_photopath TEXT,")
                .append("PRIMARY KEY(_gid,_mid)")
                .append(");");
        sqLiteDatabase.execSQL(sb.toString());
        sb.delete(0,sb.length());

        sb.append("CREATE TABLE ").append(TABLE_DATA).append("(")
                .append("_date_time NUMERIC")
                .append("_gid INTEGER, ").append("_mid INTEGER, ")
                .append("_type INTEGER, ").append("_data TEXT ")
                .append(");");
        sqLiteDatabase.execSQL(sb.toString());
        sb.delete(0,sb.length());

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        super.onOpen(sqLiteDatabase);
    }
}
