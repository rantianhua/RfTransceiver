package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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
    @InjectView(R.id.seekbar_setting_volume)
    SeekBar seekBar;    //调节音量

    private CallbackInSF callbackInSF;
    private DBManager dbManager;

    private Drawable dwHead;    //用户头像
    private String name;    //用户昵称
    private AudioManager audioManager;  //设置音量

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
    }

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

        //显示当前音量
        int maxVoluem = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekBar.setMax(maxVoluem);
        int currentVoluem = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar.setProgress(currentVoluem);
    }

    public void setCallbackInSF ( CallbackInSF callbackInSF) {
        this.callbackInSF  = callbackInSF;
    }

    private void initEvent() {
        rlPersonal.setOnClickListener(this);
        tvClearCache.setOnClickListener(this);
        rlPersonal.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        i, AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
                selfInfoFragment.setChangeInfo(true);
                selfInfoFragment.setTargetFragment(SettingFragment.this,REQUEST_CHANGEINFO);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CHANGEINFO && resultCode == Activity.RESULT_OK && data != null) {
            String name = data.getStringExtra("name");
            //修该名字
            if(callbackInSF != null) {
                callbackInSF.changeINfo(data);
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface CallbackInSF{
        void changeINfo(Intent data);
    }

    public static final int REQUEST_CHANGEINFO = 401; //请求修改个人信息的代码
}
