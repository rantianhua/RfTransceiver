package com.source;

import android.util.Log;

import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.util.Constants;

/**
 * Created by rantianhua on 15-5-18.
 * this class used to achieve the crc16 check
 * create the crc code from source data and
 * check the crc code is right or not
 */
public class Crc16Check {

    /**
     * the creation polynomial ,used to create crc check code
     */
    private byte[] poly;
    private StringBuilder sb;

    public Crc16Check() {
        //initial polynomial
        poly = new byte[16];
        poly[15] = 1;
        poly[14] = 1;
        poly[1]=1;
        poly[0] = 1;
        sb = new StringBuilder();
    }

    //change the byte array to binary array
    private byte[] toBinary(byte[] data) {
        int len = data.length;
        byte[] bin = new byte[len * 8];
        int index = 0;
        String s;
        for(byte b : data) {
            s = Integer.toBinaryString(b);
            if(s.length() > 8) {
                s = s.substring(s.length()-8);
            }else if(s.length() < 8) {
                for(int j = 8-s.length();j>0;j--) {
                    bin[index++] = 0;
                }
            }
            for(int j = 0;j < s.length();j++) {
                int temp = s.codePointAt(j);
                if(temp == 48) temp = 0;
                if(temp == 49) temp = 1;
                bin[index++] = (byte)temp;
            }
        }
        s = null;
        data = null;
        return bin;
    }


    /**
     *
     * @param source the data to be created crc code or checked crc code
     * @param mode if mode is 1,the source is a crc code ,now needed to be check the source is right or not
     *             if mode is 0,the source is a source data to be created crc code
     * @return the crc code
     */
    private byte[] crcCheck(byte[] source,int mode) {
        if (source.length < poly.length) {
            byte[] temp = new byte[poly.length];
            int index = 0;
            for (int i = poly.length - source.length; i > 0; i--) {
                temp[index++] = 0;
            }
            System.arraycopy(source, 0, temp, index, source.length);
            source = null;
            source = temp;
        }
        byte[] data;
        if (mode == 0) {
            data = new byte[source.length + poly.length - 1];
            System.arraycopy(source, 0, data, 0, source.length);
        } else {
            data = source;
        }
        byte[] remainder = new byte[poly.length];    //use to save remainder,also is the crc code register
        for (int i = poly.length - 1; i < data.length; i++) {
            if (i == poly.length - 1) {
                System.arraycopy(data, 0, remainder, 0, remainder.length);
            } else {
                System.arraycopy(remainder, 1, remainder, 0, remainder.length - 1);
                remainder[remainder.length - 1] = data[i];
            }
            if (remainder[0] == 1) {
                for (int j = 0; j < remainder.length; j++) {
                    remainder[j] ^= poly[j];
                }
            }
        }
        source = null;
        return remainder;
    }


    public byte[] createCrcCode(byte[] data) {
        byte[] crcCode = crcCheck(toBinary(data),0);
        byte[] crcHight = new byte[8];
        System.arraycopy(crcCode,0,crcHight,0,8);
//        data[Constants.Crc_Index_hight] = (byte)getDecimalFromCrcCode(crcHight);
        crcHight = null;
        byte[] crcLow = new byte[8];
        System.arraycopy(crcCode,8,crcLow,0,8);
//        data[Constants.Crc_Index_low] = (byte) getDecimalFromCrcCode(crcLow);
        crcLow = null;
        return data;
    }

    //get Decimal by the CrcCode
    private int getDecimalFromCrcCode(byte[] complement) {
        //first change to original code
        int res = 0;
        for(int  i = 0;i < complement.length;i++) {
            res += (complement[i] == 0 ? 0 : (int)Math.pow(2,complement.length-1-i));
        }
        complement = null;
        return res;
    }
    
    public boolean isPacketRight(byte[] data) {
        byte[] crcTemp = new byte[2];
//        crcTemp[0] = data[Constants.Crc_Index_hight];
//        crcTemp[1] = data[Constants.Crc_Index_low];
        byte[] crcCode = toBinary(crcTemp);
        crcTemp = null;
//        data[Constants.Crc_Index_hight] = 0;
//        data[Constants.Crc_Index_low] = 0;
        byte[] checkTemp = toBinary(data);
        byte[] checkData = new byte[checkTemp.length+poly.length-1];
        System.arraycopy(checkTemp,0,checkData,0,checkTemp.length);
        System.arraycopy(crcCode,1,checkData,checkTemp.length,crcCode.length-1);
        byte[] result = crcCheck(checkData,1);
        checkTemp = null;
        data = null;
        checkData = null;
        crcCode = null;
        for(byte res : result) {
            if(res == 1) {
                return false;
            }
        }
        result = null;
        return true;
    }

}
