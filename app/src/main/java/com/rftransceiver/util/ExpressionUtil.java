package com.rftransceiver.util;

import android.text.Html;
import android.widget.ImageView;

import com.rftransceiver.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rantianhua on 15-6-18.
 */
public class ExpressionUtil {

    /**
     * HashMap , filled with expressions' drawable id
     * used to find expression id when user choose a expression
     * in UI
     */
    public static Map<Integer,List<Integer>> epDatas = new HashMap<>();

    /**
     * filled with expressions' id,
     * used to find expressions' id when saved text data contained expression
     */
    public static Map<Integer,Integer> expressions = new HashMap<>();

    static {
        int preSize = expressions.size();
        List<Integer> list1 = new ArrayList<>();
        list1.add(R.drawable.ep_1);
        list1.add(R.drawable.ep_2);
        list1.add(R.drawable.ep_3);
        list1.add(R.drawable.ep_4);
        list1.add(R.drawable.ep_5);
        list1.add(R.drawable.ep_6);
        list1.add(R.drawable.ep_7);
        list1.add(R.drawable.ep_8);
        list1.add(R.drawable.ep_9);
        list1.add(R.drawable.ep_10);
        list1.add(R.drawable.ep_11);
        list1.add(R.drawable.ep_12);
        list1.add(R.drawable.ep_13);
        list1.add(R.drawable.ep_14);
        list1.add(R.drawable.ep_15);
        list1.add(R.drawable.ep_16);
        for(int i = preSize;i < list1.size();i ++) {
            expressions.put(i,list1.get(i));
        }
        epDatas.put(0, list1);
        List<Integer> list2 = new ArrayList<>();
        list2.add(R.drawable.ep_17);
        list2.add(R.drawable.ep_18);
        list2.add(R.drawable.ep_19);
        list2.add(R.drawable.ep_20);
        list2.add(R.drawable.ep_21);
        list2.add(R.drawable.ep_22);
        list2.add(R.drawable.ep_23);
        list2.add(R.drawable.ep_24);
        list2.add(R.drawable.ep_25);
        list2.add(R.drawable.ep_26);
        list2.add(R.drawable.ep_27);
        list2.add(R.drawable.ep_28);
        list2.add(R.drawable.ep_29);
        list2.add(R.drawable.ep_30);
        list2.add(R.drawable.ep_31);
        list2.add(R.drawable.ep_32);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(1, list2);
        List<Integer> list3 = new ArrayList<>();
        list3.add(R.drawable.ep_33);
        list3.add(R.drawable.ep_34);
        list3.add(R.drawable.ep_35);
        list3.add(R.drawable.ep_36);
        list3.add(R.drawable.ep_37);
        list3.add(R.drawable.ep_38);
        list3.add(R.drawable.ep_39);
        list3.add(R.drawable.ep_40);
        list3.add(R.drawable.ep_41);
        list3.add(R.drawable.ep_42);
        list3.add(R.drawable.ep_43);
        list3.add(R.drawable.ep_44);
        list3.add(R.drawable.ep_45);
        list3.add(R.drawable.ep_46);
        list3.add(R.drawable.ep_47);
        list3.add(R.drawable.ep_48);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(2, list3);
        List<Integer> list4 = new ArrayList<>();
        list4.add(R.drawable.ep_49);
        list4.add(R.drawable.ep_50);
        list4.add(R.drawable.ep_51);
        list4.add(R.drawable.ep_52);
        list4.add(R.drawable.ep_53);
        list4.add(R.drawable.ep_54);
        list4.add(R.drawable.ep_55);
        list4.add(R.drawable.ep_56);
        list4.add(R.drawable.ep_57);
        list4.add(R.drawable.ep_58);
        list4.add(R.drawable.ep_59);
        list4.add(R.drawable.ep_60);
        list4.add(R.drawable.ep_61);
        list4.add(R.drawable.ep_62);
        list4.add(R.drawable.ep_63);
        list4.add(R.drawable.ep_64);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(3, list4);
        List<Integer> list5 = new ArrayList<>();
        list5.add(R.drawable.ep_65);
        list5.add(R.drawable.ep_66);
        list5.add(R.drawable.ep_67);
        list5.add(R.drawable.ep_68);
        list5.add(R.drawable.ep_69);
        list5.add(R.drawable.ep_70);
        list5.add(R.drawable.ep_71);
        list5.add(R.drawable.ep_72);
        list5.add(R.drawable.ep_73);
        list5.add(R.drawable.ep_74);
        list5.add(R.drawable.ep_75);
        list5.add(R.drawable.ep_76);
        list5.add(R.drawable.ep_77);
        list5.add(R.drawable.ep_78);
        list5.add(R.drawable.ep_79);
        list5.add(R.drawable.ep_80);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(4, list5);
        List<Integer> list6 = new ArrayList<>();
        list6.add(R.drawable.ep_81);
        list6.add(R.drawable.ep_82);
        list6.add(R.drawable.ep_83);
        list6.add(R.drawable.ep_84);
        list6.add(R.drawable.ep_85);
        list6.add(R.drawable.ep_86);
        list6.add(R.drawable.ep_87);
        list6.add(R.drawable.ep_88);
        list6.add(R.drawable.ep_89);
        list6.add(R.drawable.ep_90);
        list6.add(R.drawable.ep_91);
        list6.add(R.drawable.ep_92);
        list6.add(R.drawable.ep_93);
        list6.add(R.drawable.ep_94);
        list6.add(R.drawable.ep_95);
        list6.add(R.drawable.ep_96);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(5, list6);
        List<Integer> list7 = new ArrayList<>();
        list7.add(R.drawable.ep_97);
        list7.add(R.drawable.ep_98);
        list7.add(R.drawable.ep_99);
        list7.add(R.drawable.ep_100);
        list7.add(R.drawable.ep_101);
        list7.add(R.drawable.ep_102);
        list7.add(R.drawable.ep_103);
        list7.add(R.drawable.ep_104);
        list7.add(R.drawable.ep_105);
        list7.add(R.drawable.ep_106);
        list7.add(R.drawable.ep_107);
        list7.add(R.drawable.ep_108);
        list7.add(R.drawable.ep_109);
        list7.add(R.drawable.ep_110);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(6,list7);
    }
}
