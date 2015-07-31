package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.util.DataClearnManager;

import java.io.File;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-26.
 */
public class SettingFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.rl_personal_setting)
    RelativeLayout rlPersonal;
    @InjectView(R.id.img_photo_setting)
    ImageView imgPhoto;
    @InjectView(R.id.tv_name_setting)
    TextView tvName;



    @InjectView(R.id.tv_cleanCache)
    TextView tvClearCache;
    @InjectView(R.id.tv_cache_size)
    TextView cacheSize;

    private CallbackInSF callbackInSF;
    private DBManager dbManager;

    private Drawable dwHead;
    private String name;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        initView(view);
        initEvent();
        getSize();
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this,view);
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.SP_USER, 0);
        String path = sp.getString(Constants.PHOTO_PATH,"");
        if(!TextUtils.isEmpty(path)) {
            int size = (int)(100 * getResources().getDisplayMetrics().density + 0.5f);
            size *= size;
            Bitmap bitmap = ImageUtil.createImageThumbnail(path,size);
            if(bitmap != null) {
                dwHead = new CircleImageDrawable(bitmap);
                imgPhoto.setImageDrawable(dwHead);
                bitmap = null;
            }
        }
        name = sp.getString(Constants.NICKNAME, "");
        if(!TextUtils.isEmpty(name)) {
            tvName.setText(name);
        }
    }

    public void setCallbackInSF ( CallbackInSF callbackInSF) {
        this.callbackInSF  = callbackInSF;
    }

    private void initEvent() {
        rlPersonal.setOnClickListener(this);
        tvClearCache.setOnClickListener(this);
        rlPersonal.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.tv_cleanCache:
                //clear cache清除缓存
                deleteC();
                getSize();
                break;
            case R.id.rl_personal_setting:
                SelfInfoFragment selfInfoFragment = new SelfInfoFragment();
                selfInfoFragment.setName(name);
                selfInfoFragment.setHead(dwHead);
                getFragmentManager().beginTransaction().replace(R.id.frame_container_setting,selfInfoFragment)
                        .addToBackStack(null).commit();
                break;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setCallbackInSF(null);
    }
    public void getSize(){
        dbManager = DBManager.getInstance(getActivity());
        String s = dbManager.getCacheInformation();
        cacheSize.setText(s);
    }
    public void deleteC(){//删除缓存操作
      if(dbManager.getFileList().size() > 0)
      {
           ArrayList<File> files = dbManager.getFileList();
           for(int i=files.size()-1;i >= 0;i--)
          {
              DataClearnManager.deleteDir(files.get(i));
          }
      }
        dbManager.deleteCache();
    }
    //for test
//    public void ins(){
//        dbManager=DBManager.getInstance(getActivity());
//        dbManager.insertMessage();
//    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CHANNEL && resultCode == Activity.RESULT_OK && data != null) {
            //receive a request to change channel
            if(callbackInSF != null) {
                getFragmentManager().popBackStackImmediate();
                callbackInSF.changeChannel(data.getIntExtra(Constants.SELECTED_CHANNEL,-1));
            }
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface CallbackInSF{
        void chageChannelRequest();
        void changeChannel(int channel);
    }

    public static final int REQUEST_CHANNEL = 400;
}
