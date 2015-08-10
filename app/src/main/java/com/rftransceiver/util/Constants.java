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

    //表示语音数据
    public static final byte Type_Sounds = (byte) 0x03;
    //表示文本信息
    public static final byte Type_Words = (byte) 0x02;
    //表示地理位置信息
    public static final byte Type_Address = (byte) 0x04;
    //表示图片信息
    public static final byte Type_Image = (byte) 0x05;
    //接收到语音信息
    public static final int READ_SOUNDS = 0;
    //接收到文字信息
    public static final int READ_WORDS = 1;
    //接收到位置信息
    public static final int READ_ADDRESS = 2;
    //接收到图片信息
    public static final int READ_Image = 3;
    //接收到设置同步字的反馈
    public static final int READ_SETASYNCWORD = 4;
    //读取到设备的rssi值
    public static final int READ_RSSI = 5;
    //读取到信道状态
    public static final int READ_CHANNEL = 6;
    //改变信道成功的反馈
    public static final int READ_CHANGE_CHANNEL = 7;
    //读到错误数据
    public static final int READ_ERROR = 8;
    //加载图片完毕
    public static final int GET_BITMAP = 9;
    //接收到未知数据
    public static final int READ_UNKNOWN = 10;
    //发送数据后处理更新UI的操作
    public static final int HANDLE_SEND = 11;
    //数据包的长度
    public static final int Data_Packet_Length = 60;
    //指令包的长度
    public static final int Instruction_Packet_Length = 10;
    //数据包的帧头
    public static final byte Data_Packet_Head = (byte) 0x01;
    //数据包的帧尾
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

    public static final String PRE_GROUP = "pre_group";

    //调试，打印log的开关
    public static boolean DEBUG = false;
    //用来判断要删除的是否是当前所在的组
    public static int GROUPID = -1;
}
