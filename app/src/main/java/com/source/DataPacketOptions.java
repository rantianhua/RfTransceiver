package com.source;

import com.rftransceiver.util.Constants;

import java.util.Random;

/**
 * Created by Rth on 2015/5/6.
 * this class define some options of the data packets for sounds or text
 * this class will tell the TextEntity or SoundsEntity how to packet the source data ,
 */
public class DataPacketOptions {

    /**
     * the packet's total length,default is 66
     */
    private int length = Constants.Data_Packet_Length;

    /**
     * the packet head
     */
    private byte head = (byte) 0x01;

    /**
     * the packet tail
     */
    private byte tail = (byte) 0xff;

    /**
     * the index of the mark of data type,default is 1
     */
    private int typeFlagIndex =  1;

    /**
     * to mark the data type by one byte
     */
    private byte typeFlag;

    /**
     * may be every packet need a byte to save the length of real data in the packet
     * default value is 2
     */
    private int realLenIndex = 2;

    /**
     * the real length of every packet
     * in the most case,the value is fixed,only when the packet is the total message's last packet ,the value is
     * the real data's length ,by this way ,not only can know the data's length of last packet,but can know the
     * data's last packet,which is very important.
     * the default fixed value is the param of the packet's length, there is no impossible that the packet is real data,
     * so it won't have any ambiguity
     */
    private byte realLen = (byte) length;

    /**
     * mostly,because of some special limit ,the real data isn't start of the begin of packet,
     * and there need a offset
     */
    private int offset;

    /**
     * the members' id int the group
     */
    private int memberIdIndex = Constants.Group_Member_Id_index;

    public DataPacketOptions(Data_Type_InOptions type,int offset) {
        this.offset = offset;
        setTypeFlag(type);
    }

    public DataPacketOptions(Data_Type_InOptions type,int offset,byte realLen) {
        this(type,offset);
        this.realLen = realLen;
    }

    public DataPacketOptions(Data_Type_InOptions type,int offset,int realLenIndex,byte realLen) {
        this(type,offset,realLen);
        this.realLenIndex = realLenIndex;
    }

    public DataPacketOptions(int len,byte head,byte tail,int typeFlagIndex,Data_Type_InOptions type,int offset,int realLenIndex,byte realLen) {
        this(type,offset,realLenIndex,realLen);
        this.length = len;
        this.head = head;
        this.tail = tail;
        this.typeFlagIndex = typeFlagIndex;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte getHead() {
        return head;
    }

    public void setHead(byte head) {
        this.head = head;
    }

    public byte getTail() {
        return tail;
    }

    public void setTail(byte tail) {
        this.tail = tail;
    }

    public int getTypeFlagIndex() {
        return typeFlagIndex;
    }

    public void setTypeFlagIndex(int typeFlagIndex) {
        this.typeFlagIndex = typeFlagIndex;
    }

    public byte getTypeFlag() {
        return typeFlag;
    }

    public void setTypeFlag(Data_Type_InOptions type) {
        if(type == Data_Type_InOptions.sounds) {
            this.typeFlag = Constants.Type_Sounds;
        }else if (type == Data_Type_InOptions.text) {
            this.typeFlag = Constants.Type_Text;
        }
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getRealLenIndex() {
        return realLenIndex;
    }

    public void setRealLenIndex(int realLenIndex) {
        this.realLenIndex = realLenIndex;
    }

    public void setTypeFlag(byte typeFlag) {
        this.typeFlag = typeFlag;
    }

    public byte getRealLen() {
        return realLen;
    }

    public void setRealLen(byte realLen) {
        this.realLen = realLen;
    }

    public int getMemberIdIndex() {
        return memberIdIndex;
    }

    public void setMemberIdIndex(int memberIdIndex) {
        this.memberIdIndex = memberIdIndex;
    }

    /**
     * the enum to distinguish the data type
     */
    public enum Data_Type_InOptions{
        sounds,
        text
    }
}
