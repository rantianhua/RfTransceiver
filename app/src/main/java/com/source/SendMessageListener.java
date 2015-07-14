package com.source;

/**
 * Created by rantianhua on 15-6-17.
 */
public interface SendMessageListener {
    /**
     * call to send packted data to ble
     * @param data
     * @param end
     * @param percent the sent data's percent of whole data
     */
    void sendPacketedData(byte[] data, boolean end,int percent);

    void sendPacketedData(byte[] data, boolean end);
}
