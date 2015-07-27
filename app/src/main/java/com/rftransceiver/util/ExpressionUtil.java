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
        list1.add(R.drawable.e1);
        list1.add(R.drawable.e2);
        list1.add(R.drawable.e3);
        list1.add(R.drawable.e4);
        list1.add(R.drawable.e5);
        list1.add(R.drawable.e6);
        list1.add(R.drawable.e7);
        list1.add(R.drawable.e8);
        list1.add(R.drawable.e9);
        list1.add(R.drawable.e10);
        list1.add(R.drawable.e11);
        list1.add(R.drawable.e12);
        list1.add(R.drawable.e13);
        list1.add(R.drawable.e14);
        list1.add(R.drawable.e15);
        list1.add(R.drawable.e16);
        list1.add(R.drawable.e17);
        list1.add(R.drawable.e18);
        list1.add(R.drawable.e19);
        list1.add(R.drawable.e20);
        list1.add(R.drawable.delete_v2);
        for(int i = preSize;i < list1.size();i ++) {
            expressions.put(i,list1.get(i));
        }
        epDatas.put(0, list1);
        List<Integer> list2 = new ArrayList<>();
        list2.add(R.drawable.e21);
        list2.add(R.drawable.e22);
        list2.add(R.drawable.e23);
        list2.add(R.drawable.e24);
        list2.add(R.drawable.e25);
        list2.add(R.drawable.e26);
        list2.add(R.drawable.e27);
        list2.add(R.drawable.e28);
        list2.add(R.drawable.e29);
        list2.add(R.drawable.e30);
        list2.add(R.drawable.e31);
        list2.add(R.drawable.e32);
        list2.add(R.drawable.e33);
        list2.add(R.drawable.e34);
        list2.add(R.drawable.e35);
        list2.add(R.drawable.e36);
        list2.add(R.drawable.e37);
        list2.add(R.drawable.e38);
        list2.add(R.drawable.e39);
        list2.add(R.drawable.e40);
        list2.add(R.drawable.delete_v2);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(1, list2);
        List<Integer> list3 = new ArrayList<>();
        list3.add(R.drawable.e41);
        list3.add(R.drawable.e42);
        list3.add(R.drawable.e43);
        list3.add(R.drawable.e44);
        list3.add(R.drawable.e45);
        list3.add(R.drawable.e46);
        list3.add(R.drawable.e47);
        list3.add(R.drawable.e57);
        list3.add(R.drawable.e58);
        list3.add(R.drawable.e59);
        list3.add(R.drawable.e60);
        list3.add(R.drawable.e61);
        list3.add(R.drawable.e62);
        list3.add(R.drawable.e63);
        list3.add(R.drawable.e64);
        list3.add(R.drawable.e65);
        list3.add(R.drawable.e66);
        list3.add(R.drawable.e67);
        list3.add(R.drawable.e68);
        list3.add(R.drawable.e69);
        list3.add(R.drawable.delete_v2);
        preSize = expressions.size();
        for(int i = preSize;i < list1.size() + preSize;i ++) {
            expressions.put(i,list1.get(i-preSize));
        }
        epDatas.put(2, list3);
        List<Integer> list4 = new ArrayList<>();
        list4.add(R.drawable.e70);
        list4.add(R.drawable.e71);
        list4.add(R.drawable.e72);
        list4.add(R.drawable.e73);
        list4.add(R.drawable.e74);
        list4.add(R.drawable.e75);
        list4.add(R.drawable.e76);
        list4.add(R.drawable.e77);
        list4.add(R.drawable.e78);
        list4.add(R.drawable.e79);
        list4.add(R.drawable.e80);
        list4.add(R.drawable.e81);
        list4.add(R.drawable.e82);
        list4.add(R.drawable.e83);
        list4.add(R.drawable.e84);
        list4.add(R.drawable.e85);
        list4.add(R.drawable.e86);
        list4.add(R.drawable.e87);
        list4.add(R.drawable.e88);
        list4.add(R.drawable.e89);
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
