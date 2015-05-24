package com.source.parse;


import android.os.Handler;

import com.rftransceiver.util.Constants;
import com.source.Crc16Check;
import com.source.DataPacketOptions;

/**
 * Created by Rth on 2015/5/6.
 * packet the data received,every packet length is defined when send data
 * as every packet,judge it belongs sounds data or text data,then give it to relative parser
 */
public class ParseFactory {

    private SoundsParser soundsParser;
    private TextParser textParser;

    /**
     * the temporary cache to cache fixed length data
     */
    //byte[] temp;

    /**
     * the counter to count temp length
     */
    //private int index = 0;

    /**
     * can roughly judge a packet is right or not by packet's length , head and tail.
     * if there is a error occur,need a tag to mark it,and then discard the wrong packet and the packet is font of it;
     * so ,int the moment ,need to record the wrong packet's length,finally when the length is tow times as every right packet length
     */
    //private byte[] error = null;

    private Handler handler;

   // private Crc16Check crc;

    public ParseFactory() {
        soundsParser = new SoundsParser();
        textParser = new TextParser();
        //crc = new Crc16Check();
    }

    private void initTemp() {
//        temp = null;
//        temp= new byte[Constants.Packet_Length];
    }

//    public void roughParse(byte[] buff,int length) throws Exception{
//        for(int i =0; i < length;i++) {
//            if(error != null) {
//                initTemp();
//                temp[index++] =buff[i];
//                if(buff[i] == (byte) 0xff) {
//                    //have receive all wrong data
//                    parseRightDataFromWrong();
//                }
//            }else {
//                if(buff[i] == Constants.Packet_Head && index == 0) {
//                    //now build a new cache
//                    initTemp();
//                    temp[index++] = buff[i];
//                }else {
//                    temp[index++] = buff[i];
//                    if(index == Constants.Packet_Length) {
//                        //the cache now is full
//                        //check this cache is right or not
//                        if(temp[Constants.Packet_Length-1] != Constants.Packet_Data_Tail  &&
//                                temp[Constants.Packet_Length-1] != Constants.Packet_Channel_Tail) {
//                            //receive a wrong packet
//                            error = temp;
//                        }
//                        if(error == null) {
//                            sendToRelativeParser();
//                        }
//                        index = 0;
//                    }
//                }
//            }
//        }
//    }

//    private void parseRightDataFromWrong() {
//        //calculate the right data's numbers in error
//        int right  = Constants.Packet_Length - index;
//        for(int i = 0; i < index;i ++) {
//            temp[i+right] = temp[i];
//        }
//        for(int i = index;i < Constants.Packet_Length;i ++) {
//            temp[i] = error[i];
//        }
//        sendToRelativeParser(temp);
//        error = null;
//        index = 0;
//    }

    public void sendToRelativeParser(byte[] temp) {
        if(temp[Constants.Packet_Length-1] == Constants.Packet_Data_Tail) {
            //check the data type and then send to relative parser
            if(temp[Constants.Packet_Type_flag_Index] == Constants.Type_Sounds) {
                //sounds packet
                soundsParser.parseSounds(temp);
            }else if(Constants.Type_Text == temp[Constants.Packet_Type_flag_Index]) {
                //text packet
                textParser.parseText(temp);
            }
        }else if(temp[Constants.Packet_Length-1] == Constants.Packet_Channel_Tail) {
            int channel = temp[Constants.Packet_Length-2] ;
            handler.obtainMessage(Constants.MESSAGE_READ,2,channel,null).sendToTarget();
        }
    }

    public void setHandler(Handler han) {
        this.handler = null;
        this.handler = han;
        soundsParser.setHandler(han);
        textParser.setHandler(han);
    }

    public void setSoundsOptions(DataPacketOptions options) {
        soundsParser.setOptions(options);
    }

    public void setTextOptions(DataPacketOptions options) {
        textParser.setOptions(options);
    }

    public void resetSounds() {
        soundsParser.reset();
    }
}
