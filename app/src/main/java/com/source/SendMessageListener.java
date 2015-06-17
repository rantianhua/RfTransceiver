package com.source;

/**
 * Created by rantianhua on 15-6-17.
 */
public interface SendMessageListener {
    /**
     * call to send packted data to ble
     * @param data
     * @param end
     */
    void sendPacketedData(byte[] data, boolean end);
}
