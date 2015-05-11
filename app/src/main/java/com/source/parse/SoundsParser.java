package com.source.parse;

import android.os.Handler;
import android.util.Log;

import com.rftransceiver.util.Constants;
import com.source.DataPacketOptions;

/**
 * Created by Rth on 2015/5/6.
 */
public class SoundsParser {

    /**
     * the handler to communicate with UI thread
     */
    private Handler  handler;

    /**
     * note the UI thread start receiving sounds data
     */
    private boolean startNoteToUi = false;

    /**
     * need to unpack the big packet to small packet so that it can be decode
     */
    byte[] soundsTemp = new byte[Constants.Small_Sounds_Packet_Length];

    /**
     * the counter to count soundsTemp
     */
    int soundsIndex = 0;

    private DataPacketOptions options;

    StringBuilder sb = new StringBuilder();

    public void parseSounds(byte[] data) {

        if(!startNoteToUi) {
            handler.obtainMessage(Constants.MESSAGE_READ,0,0,null).sendToTarget();
            startNoteToUi  = true;
        }
        //check weather this packet is last packet or not
        if(data[options.getRealLenIndex()] == options.getRealLen()) {
            unPackData(data,options.getLength()-options.getOffset()-1);
        }else {
            //this is the last packet
            unPackData(data,data[Constants.Packet_real_data_index]);
            handler.obtainMessage(Constants.MESSAGE_READ,0,1,null).sendToTarget();
            startNoteToUi = false;
            soundsIndex = 0;
        }
    }

    private void unPackData(byte[] data,int len) {
        if(len > (options.getLength()-options.getOffset()-1) || len % Constants.Small_Sounds_Packet_Length != 0) {
            return;
        }
        try {
            for(int i = options.getOffset();i < options.getOffset()+len;i++) {
                soundsTemp[soundsIndex++] = data[i];
                if(soundsIndex==Constants.Small_Sounds_Packet_Length) {
                    soundsIndex = 0;
                    //soundsTemp have been full
                    handler.obtainMessage(Constants.MESSAGE_READ,0,2,soundsTemp).sendToTarget();
                    soundsTemp = null;
                    soundsTemp = new byte[Constants.Small_Sounds_Packet_Length];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("unPackSoundsData","the len is "+ len);
        }
    }

    public void setHandler(Handler han) {
        this.handler = null;
        this.handler = han;
    }

    public void setOptions(DataPacketOptions options) {
        this.options = null;
        this.options = options;
    }

    public void reset() {
        startNoteToUi = false;
        soundsIndex = 0;
    }

}
