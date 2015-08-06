package com.source.parse;


import android.os.Handler;
import android.util.Log;

import com.rftransceiver.util.Constants;
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

    StringBuilder sb = new StringBuilder();

    public ParseFactory() {
        soundsParser = new SoundsParser();
        textParser = new TextParser();
    }

    public void sendToRelativeParser(byte[] temp) {
        if(temp[Constants.Data_Packet_Length-1] == Constants.Data_Packet_Tail) {
            //check the data type and then send to relative parser
            switch (temp[Constants.Packet_Type_flag_Index]) {
                case Constants.Type_Sounds:
                    //sounds packet
                    soundsParser.parseSounds(temp);
                    break;
                case Constants.Type_Words:
                    //text packet
                    textParser.parseText(temp, DataPacketOptions.TextType.Words);
                    break;
                case Constants.Type_Address:
                    //text packet
                    textParser.parseText(temp, DataPacketOptions.TextType.Address);
                    break;
                case Constants.Type_Image:
                    //text packet
                    textParser.parseText(temp, DataPacketOptions.TextType.Image);
                    break;
                default:
                    unKnowData(temp);
                    Log.e("receive", "unknow data content" + temp[Constants.Packet_Type_flag_Index]);
                    break;
            }
        }else if(temp[Constants.Data_Packet_Length-1] == Constants.Instruction_Packet_Tail) {
            switch (temp[1]) {
                case 1:
                    handler.obtainMessage(Constants.MESSAGE_READ,Constants.READ_SETASYNCWORD
                            ,-1,null).sendToTarget();
                    break;
                case 2:
                    handler.obtainMessage(Constants.MESSAGE_READ,
                            Constants.READ_CHANGE_CHANNEL,temp[2],null).sendToTarget();
                    break;
                case 3:
                    handler.obtainMessage(Constants.MESSAGE_READ,
                            Constants.READ_RSSI,temp[2],null).sendToTarget();
                    break;
                case 4:
                    handler.obtainMessage(Constants.MESSAGE_READ,Constants.READ_CHANNEL
                            ,temp[2],temp[3]).sendToTarget();
                    break;
                case 5:
                    handler.obtainMessage(Constants.MESSAGE_READ,
                            Constants.READ_ERROR,-1,null).sendToTarget();
                    break;
                default:
                    unKnowData(temp);
                    Log.e("receive", "unknow data instruct" + temp[1]);
                    break;
            }
        }else {
           unKnowData(temp);
        }
    }

    private void unKnowData(byte[] data) {
        if(Constants.DEBUG) {
            Log.e("receive", "unknow data tail"+data[Constants.Data_Packet_Length-1]);
        }
        if(handler != null) {
            handler.obtainMessage(Constants.MESSAGE_READ,Constants.READ_UNKNOWN,-1,null).sendToTarget();
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
