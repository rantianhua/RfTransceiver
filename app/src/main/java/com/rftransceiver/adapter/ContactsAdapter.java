package com.rftransceiver.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.datasets.ContactsData;

import org.w3c.dom.Text;

import java.util.List;
import java.util.Map;

/**
 * Created by rth on 15-7-23.
 */
public class ContactsAdapter extends BaseExpandableListAdapter {

    private Map<String,List<ContactsData>> mapDatas ;

    private Context context;

    public ContactsAdapter(Map<String,List<ContactsData>> dataSet,Context context) {
        this.context = context;
        mapDatas = dataSet;
    }

    @Override
    public int getGroupCount() {
        return mapDatas.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return mapDatas.get(getKeyByIndex(i)).size();
    }

    @Override
    public Object getGroup(int i) {
        return getKeyByIndex(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return mapDatas.get(getKeyByIndex(i)).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        GroupHolder groupHolder = null;
        if(view == null) {
            groupHolder = new GroupHolder();
            groupHolder.tvLetter = new TextView(context);
            groupHolder.tvLetter.setWidth(context.getResources().getDisplayMetrics().widthPixels);
            groupHolder.tvLetter.setPadding(20, 10, 0, 10);
            groupHolder.tvLetter.setTextColor(Color.BLUE);
            groupHolder.tvLetter.setTextSize(40f);
            view = groupHolder.tvLetter;
            view.setTag(groupHolder);
        }else {
            groupHolder = (GroupHolder)view.getTag();
        }
        groupHolder.tvLetter.setText(getKeyByIndex(i));
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        ChildHolder childHolder = null;
        if(view == null) {
            childHolder = new ChildHolder();
            childHolder.tvName = new TextView(context);
            childHolder.tvName.setWidth(context.getResources().getDisplayMetrics().widthPixels);
            childHolder.tvName.setPadding(60, 15, 0, 15);
            childHolder.tvName.setTextSize(18f);
            childHolder.tvName.setTextColor(Color.BLACK);
            childHolder.tvName.setBackgroundResource(R.drawable.click_with_color);
            view = childHolder.tvName;
            view.setTag(childHolder);
        }else {
            childHolder = (ChildHolder) view.getTag();
        }
        childHolder.tvName.setText(mapDatas.get(getKeyByIndex(i)).get(i1).getGroupName());
        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private String getKeyByIndex(int i) {
        int index = 0;
        String res = null;
        for(String key : mapDatas.keySet()) {
            if(index == i) {
                res = key;
                break;
            }
            index++;
        }
        return res;
    }

    static class GroupHolder {
        TextView tvLetter;
    }

    static class ChildHolder {
        TextView tvName;
    }


}
