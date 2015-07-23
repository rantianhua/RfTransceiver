package com.rftransceiver.datasets;


import android.util.Log;

import com.rftransceiver.util.HanziToPinyin;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by rth on 15-7-23.
 */
public class ContactsData {

    private String groupName;

    private int groupId;

    private final Pattern pattern = Pattern.compile("^[A-Z]");

    /**
     * the first letter of pinyin or string
     */
    private String firstLetter;

    public ContactsData(String groupName,int groupId ){
        setGroupId(groupId);
        setGroupName(groupName);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
        setFirstLetter(groupName);
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getFirstLetter() {
        return firstLetter;
    }

    public void setFirstLetter(String source) {
        String res = null;
        ArrayList<HanziToPinyin.Token> tokens = HanziToPinyin.getInstance().get(source);
        if(tokens != null && tokens.size() > 0) {
            for (HanziToPinyin.Token token : tokens) {
                if (token.type == HanziToPinyin.Token.PINYIN) {
                    res = token.target;
                    break;
                } else {
                    res = token.source;
                    break;
                }
            }
        }else {
            res = source.substring(0,1).charAt(0) + "";
        }
        if(res == null) {
            res = "#";
        }else {
            res = res.substring(0,1);
            res = res.toUpperCase();
            Log.e("the res",res);
            if(!pattern.matcher(res).matches()) {
                res = "#";
            }
        }
        this.firstLetter = res;
    }

}
