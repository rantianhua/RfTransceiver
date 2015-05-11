package com.rftransceiver.util;

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
     *  the value of Type_Text to mark the text data
     */
    public static final byte Type_Sounds = (byte) 0x03;

    public static final byte Type_Text = (byte) 0x02;

    public static final int Packet_Length = 66;

    public static final byte Packet_Head = (byte) 0x01;

    public static final byte Packet_Data_Tail = (byte) 0xff;

    public static final byte Packet_Channel_Tail = (byte) 0x07;

    public static final int Packet_Type_flag_Index = 1;

    public static final int Packet_real_data_index = 2;

    public static final int Small_Sounds_Packet_Length = 15;
}
