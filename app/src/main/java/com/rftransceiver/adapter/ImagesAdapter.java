package com.rftransceiver.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;

import com.rftransceiver.R;
import com.rftransceiver.datasets.ImageFolderData;
import com.rftransceiver.customviews.CommonAdapter;
import com.rftransceiver.customviews.CommonViewHolder;

import java.util.List;

/**
 * Created by rantianhua on 15-6-11.
 */
public class ImagesAdapter extends CommonAdapter {

    private String dirPath; //the path of folder
    private boolean canTake;    //标识是否可以拍照

    /**
     *
     * @param dirPath     图片目录
     * @param datas 图片路径
     * @param context
     * @param layoutId  布局iid
     */
    public ImagesAdapter(String dirPath,List<String> datas,Context context,int layoutId) {
        super(context,datas,layoutId);
        this.dirPath = dirPath;
    }

    @Override
    public void convert(CommonViewHolder helper, Object item) {
        if(item.equals(ImageFolderData.CAMERA)) {
            ImageView imageView = helper.getView(R.id.img_grid_images);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setBackgroundColor(mContext.getResources().getColor(R.color.take_pic));
            helper.setImageResource(R.id.img_grid_images,R.drawable.camera);
        }else {
            if(item instanceof String && TextUtils.isEmpty((String)item)) return;
            helper.setImageByUrl(R.id.img_grid_images, dirPath + "/" + item);
        }
    }

    public String getDirPath() {
        return this.dirPath;
    }
}
