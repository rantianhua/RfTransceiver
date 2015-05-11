package com.rftransceiver.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rth on 2015/4/27.
 */
public class ListConversationAdapter extends BaseAdapter {

    private List<ConversationData> listData = new ArrayList<>();
    private LayoutInflater inflater = null;

    public ListConversationAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHodler hodler = null;
        ConversationData data = listData.get(position);
        if(convertView == null) {
            hodler = new ViewHodler();
            if(data.getConversationType() == ConversationType.Me) {
                convertView = inflater.inflate(R.layout.list_conversation_left,null);
                hodler.tvContent = (TextView) convertView.findViewById(R.id.tv_list_left);
            }else {
                convertView = inflater.inflate(R.layout.list_conversation_right,null);
                hodler.tvContent = (TextView) convertView.findViewById(R.id.tv_list_right);
            }
            convertView.setTag(hodler);
        }else {
            hodler = (ViewHodler) convertView.getTag();
        }
        hodler.tvContent.setText(data.getContent());
        return convertView;
    }

    public void clear() {
        listData.clear();
        notifyDataSetChanged();
    }

    class ViewHodler {
        TextView tvContent;
    }

    public void addData(ConversationData data) {
        listData.add(data);
        notifyDataSetChanged();
    }

    public enum ConversationType{
        Me,
        Other
    }

    @Override
    public int getItemViewType(int position) {
        if (listData.get(position).getConversationType() == ConversationType.Me) {
            return 0;
        }else {
            return 1;
        }
    }
}
