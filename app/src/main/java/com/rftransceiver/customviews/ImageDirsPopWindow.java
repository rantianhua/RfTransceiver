package com.rftransceiver.customviews;

import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.rftransceiver.R;
import com.rftransceiver.datasets.ImageFolderData;

import java.util.List;

/**
 * Created by rantianhua on 15-6-12.
 */
public class ImageDirsPopWindow extends BasePopwindow<ImageFolderData> {

    private ListView listView;
    private OnPictureDirSelected pictureDirSelected;    //callback function

    public ImageDirsPopWindow(View convertView,int width,int height,List<ImageFolderData> dataList) {
        super(convertView,width,height,true,dataList);
    }

    @Override
    public void initViews() {
        listView = (ListView) findViewById(R.id.listview_popwindow_img_dirs);
        listView.setAdapter(new CommonAdapter<ImageFolderData>(context,datas,R.layout.item_popwindow_img_dirs) {
            @Override
            public void convert(CommonViewHolder helper, ImageFolderData item) {
                helper.setImageByUrl(R.id.img_item_dir, item.getFirstPicPath());
                helper.setText(R.id.tv_name_item_dir,item.getName());
                helper.setText(R.id.tv_count_item_dir,item.getCounts()+"张");
                helper.getView(R.id.img_choose_item_dir).setVisibility(
                        item.isSelected() ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    @Override
    public void iniEvents() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(pictureDirSelected != null) {
                    pictureDirSelected.dirSelected(datas.get(i),i);
                }
            }
        });
    }

    @Override
    protected void initPatrams(Object[] params) {

    }

    public void setPictureDirSelected(OnPictureDirSelected selected) {
        this.pictureDirSelected = selected;
    }

    public interface OnPictureDirSelected {
        void dirSelected(ImageFolderData data,int selectedId);
    }

    public void notifyData(int oldSelect,int newSelect) {
        datas.get(oldSelect).setSelected(false);
        datas.get(newSelect).setSelected(true);
        BaseAdapter adapter = (BaseAdapter)listView.getAdapter();
        adapter.notifyDataSetChanged();
    }
}
