package com.my_interface;

/**
 * Created by Rth on 2015/5/6.
 */
public interface SendMessageListener {
    void sendSound(byte[] data,boolean end);
    void sendText(byte[] temp,boolean end);
}
