package com.rftransceiver.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rth on 2015/4/27.
 */
public class ListConversationAdapter extends BaseAdapter {

    private List<ConversationData> listData = new ArrayList<>();
    private LayoutInflater inflater = null;

    /**
     * parse expression data from content
     */
    private Html.ImageGetter imageGetter;

    public ListConversationAdapter(Context context,Html.ImageGetter imageGetter) {
        inflater = LayoutInflater.from(context);
        this.imageGetter = imageGetter;
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
                convertView = inflater.inflate(R.layout.list_conversation_right,null);
                hodler.tvContent = (TextView) convertView.findViewById(R.id.tv_list_right);
            }else {
                convertView = inflater.inflate(R.layout.list_conversation_left,null);
                hodler.tvContent = (TextView) convertView.findViewById(R.id.tv_list_left);
                hodler.imgLevel = (ImageView) convertView.findViewById(R.id.img_conversation_level);
                hodler.imgPhoto = (ImageView) convertView.findViewById(R.id.img_conversation_photo);
                hodler.tvLevel = (TextView) convertView.findViewById(R.id.tv_conversation_level);
            }
            convertView.setTag(hodler);
        }else {
            hodler = (ViewHodler) convertView.getTag();
        }
        hodler.tvContent.setText( Html.fromHtml(data.getContent(),
                imageGetter,null));

        if(data.getConversationType() == ConversationType.Me) {

        }else if(data.getConversationType() == ConversationType.Me.Other) {
            String instance = data.getInstance();
            if(instance != null) {
                hodler.tvLevel.setText(instance);
            }
            hodler.imgLevel.setImageResource(data.getLevelId());
            Drawable drawable = data.getPhotoDrawable();
            if(drawable != null) {
                hodler.imgPhoto.setImageDrawable(drawable);
            }
        }

        return convertView;
    }

    public void clear() {
        listData.clear();
        notifyDataSetChanged();
    }

    class ViewHodler {
        TextView tvContent,tvLevel;
        ImageView imgPhoto,imgLevel;
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
