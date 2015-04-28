package com.rftransceiver;

import java.util.ArrayList;
import java.util.List;

public class DATA_TEST 
{
    private static DATA_TEST data_test = null;

    List<AudioData> audiodatalist=new ArrayList<>();

    public static DATA_TEST getInstance() {
        if(data_test == null) {
            data_test = new DATA_TEST();
        }
        return data_test;
    }
}
