package com.source.parse;

import android.os.Handler;

import com.rftransceiver.util.Constants;
import com.source.DataPacketOptions;

/**
 * Created by Rth on 2015/5/6.
 */
public class TextParser {

    private DataPacketOptions options;

    private Handler handler;

    /**
     * a cache to save text bytes
     */
    private byte[] textTemp = new byte[1000];

    /**
     * the real text bytes's length
     */
    private int length = 0;

    public void parseText(byte[] data) {
        if(data[options.getRealLenIndex()] == options.getRealLen()) {
            makeText(data,options.getLength() - options.getOffset()-1);
        }else {
            //this is the last packet
            makeText(data,data[options.getRealLenIndex()]);
            byte[] sendData = new byte[length];
            System.arraycopy(textTemp,0,sendData,0,length);
            handler.obtainMessage(Constants.MESSAGE_READ,1,-1,sendData).sendToTarget();
            sendData = null;
            length = 0;
        }

    }

    private void makeText(byte[] data,int len) {
        if(len > options.getLength() - options.getOffset() -1) {
            return;
        }
        for(int i = options.getOffset();i < len + options.getOffset();i++) {
            textTemp[length++] = data[i];
        }
    }

    public void setOptions(DataPacketOptions options) {
        this.options = null;
        this.options = options;
    }


    public void setHandler(Handler han) {
        this.handler = null;
        this.handler = han;
    }
}
