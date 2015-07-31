package com.rftransceiver.util;

import android.os.AsyncTask;

import com.rftransceiver.group.GroupEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rth on 2015/4/26.
 */
public class Constants {
    /**
     * indicate the state of bluetooth server
     */
    public static final int MSG_WHAT_STATE = 0;

    public static final int MSG_WHAT_DEVICE_NAME = 1;

    public static final int MESSAGE_TOAST= 2;

    public static final int MESSAGE_READ= 3;

    public static final int MESSAGE_WRITE= 4;

    public static final String DEVICE_NAME = "device_name";

    public static final String TOAST = "toast";

    /**
     * the value of Type_Sounds to mark the sounds data
     *  the value of Type_Words to mark the words data
     *  the value of Type_Address to mark the address data
     *  the value of Type_Image to mark the image data
     */
    public static final byte Type_Sounds = (byte) 0x03;

    public static final byte Type_Words = (byte) 0x02;

    public static final byte Type_Address = (byte) 0x04;

    public static final byte Type_Image = (byte) 0x05;

    public static final int READ_SOUNDS = 0;

    public static final int READ_WORDS = 1;

    public static final int READ_ADDRESS = 2;

    public static final int READ_Image = 3;

    public static final int READ_SETASYNCWORD = 4;

    public static final int READ_RSSI = 5;

    public static final int READ_CHANNEL = 6;

    public static final int READ_CHANGE_CHANNEL = 7;

    public static final int READ_ERROR = 8;

    public static final int GET_BITMAP = 9;
    /**
     * the every data packet's length
     */
    public static final int Data_Packet_Length = 60;

    /**
     * the length of instruction packet
     */
    public static final int Instruction_Packet_Length = 10;

    /**
     * the head and tail of data packet
     */
    public static final byte Data_Packet_Head = (byte) 0x01;

    public static final byte Data_Packet_Tail = (byte) 0xff;

    /**
     * the head and tail of instruction packet
     */
    public static final byte Instruction_Packet_Head = (byte) 0x02;

    public static final byte Instruction_Packet_Tail = (byte) 0xfe;

    /**
     * mark the index of data type in data packet
     */
    public static final int Packet_Type_flag_Index = 1;

    /**
     * mark the index of data real length in packet
     */
    public static final int Packet_real_data_index = 2;

    /**
     * the id of group members in the group
     */
    public static final int Group_Member_Id_index = 3;



    /**
     * mark every sounds encoded packets' length
     */
    public static final int Small_Sounds_Packet_Length = 10;


    public static final String WIFI_IDENTIFY = "wifi_identity";

    /**
     * instruction packets
     */
    public static final byte[] RESET = new byte[Instruction_Packet_Length];
    public static final byte[] CHANNEL = new byte[Instruction_Packet_Length];
    public static final byte[] RSSI = new byte[Instruction_Packet_Length];
    public static final byte[] CHANNEL_STATE = new byte[Instruction_Packet_Length];
    static {

        RESET[0] = Instruction_Packet_Head;
        RESET[RESET.length-1] = Instruction_Packet_Tail;
        RESET[1] = (byte)0xff;

        CHANNEL[0] = Instruction_Packet_Head;
        CHANNEL[CHANNEL.length-1] = Instruction_Packet_Tail;
        CHANNEL[1] = (byte)0x02;

        RSSI[0] = Instruction_Packet_Head;
        RSSI[RSSI.length-1] = Instruction_Packet_Tail;
        RSSI[1] = (byte) 0x03;

        CHANNEL_STATE[0] = Instruction_Packet_Head;
        CHANNEL_STATE[CHANNEL_STATE.length-1] = Instruction_Packet_Tail;
        CHANNEL_STATE[1] = (byte) 0x04;
    }

    public static final String SP_USER = "user";    //the name of sharedPreference which is tojava.lang.String save user's information,
    public static final String NICKNAME = "nickname";

    public static final int MSG_GROUP_INFO = 0; //indicate this message is group's base info

    public static final String PHOTO_NAME = "/photo.png";
    public static final String PHOTO_PATH = "photo_path";
    public static final String BIND_DEVICE_ADDRESS = "bind_device_address";
    public static final String BIND_DEVICE_NAME = "bind_device_name";
    public static final String SELECTED_CHANNEL = "selected_channel";

    public static final String PRE_GROUP = "pre_group";

    public static boolean SAVE_SOUNDS = true;
    public static int GROUPID = -1;
}
