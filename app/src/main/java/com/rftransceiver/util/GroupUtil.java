package com.rftransceiver.util;

import java.util.Random;

/**
 * Created by rantianhua on 15-6-4.
 */
public class GroupUtil {

    /**
     * @return a asyncWord to set scm
     */
    public static byte[] createAsynWord() {
        Random random = new Random(System.currentTimeMillis());
        byte[] word = new byte[2];
        random.nextBytes(word);
        random = null;
        return word;
    }
}
