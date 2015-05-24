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

    /**
     * the every data packet's length
     */
    public static final int Packet_Length = 66;

    /**
     * the head and tail of packet
     */
    public static final byte Packet_Head = (byte) 0x01;

    public static final byte Packet_Data_Tail = (byte) 0xff;

    /**
     * the packet tail tell the cms to change communication channel
     */
    public static final byte Packet_Channel_Tail = (byte) 0x07;

    /**
     * mark the index of data type in data packet
     */
    public static final int Packet_Type_flag_Index = 1;

    /**
     * mark the index of data real length in packet
     */
    public static final int Packet_real_data_index = 2;

    /**
     * mark the index of crc code in every packet
     * now use 16 bits crc code ,so need two bytes to save it
     */
    public static final int Crc_Index_hight = 3;
    public static final int Crc_Index_low = 4;

    /**
     * mark every sounds encoded packets' length
     */
    public static final int Small_Sounds_Packet_Length = 15;

    /**
     * the packet to tell the scm to clear all data
     */
    public static final byte[] Reset = new byte[66];
    static {
        Reset[0] = (byte) 0xfe;
        Reset[1] = Reset[0];
        Reset[2] = Reset[0];
        Reset[3] = Reset[0];
    }

}
