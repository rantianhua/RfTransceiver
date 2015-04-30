package com.source.text;

/**
 * Created by Rth on 2015/4/29.
 * this class handle the text content
 * if user input a content ,this class need to unpack the content
 * a packets (66bytes per packet)
 */
public class TextEntity {

    private boolean haveNote = false;


    /**
     * note that will send text message
     */
    private byte[] note = new byte[66];

    /**
     *
     * @param sendListener callback in MainActivity
     */
    private SendTextListener sendListener = null;

    public TextEntity(SendTextListener sendListener) {
        this.sendListener = sendListener;
        for(int i = 0;i < 66; i++) {
            note[i] = (byte) 0x02;
        }
    }

    public void unpacking(String content) {
        byte[] temp = new byte[66];
        temp[0] = (byte) 0x01;
        temp[1] = (byte) 0x02;
        temp[65] = (byte) 0x04;
        int index = 2;  //counter
        byte[] text = content.getBytes();
        if(text.length <= 63) {
            for(int i = 0; i < text.length;i ++) {
                temp[index++] = text[i];
            }
            temp[1] = (byte) text.length;
            send(temp,66);
        }
    }

    private void send(byte[] data,int size) {

    }

    /**
     * the interface to callback in MainActivity to send message
     */
    public interface SendTextListener{
        void sendText(byte[] temp,int length);
    }
}
