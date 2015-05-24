package com.my_interface;

/**
 * Created by Rth on 2015/5/6.
 */
public interface SendMessageListener {
    void sendPacketedData(byte[] data,boolean end);
    void sendUnPacketedData(byte[] data,int mode);
}
