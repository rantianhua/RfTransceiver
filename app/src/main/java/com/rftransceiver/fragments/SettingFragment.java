package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageUtil;

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
    @InjectView(R.id.img_switch_sounds)
    ImageView imgSwitch;
    @InjectView(R.id.tv_change_channel)
    TextView tvChangeChannel;
    @InjectView(R.id.tv_clear_cache)
    TextView tvClearCache;
    @InjectView(R.id.tv_cache_size)
    TextView cacheSize;
    @InjectView(R.id.tv_about)
    TextView tvAbout;
    @InjectView(R.id.tv_current_channel)
    TextView tvCurrentChannel;

    private CallbackInSF callbackInSF;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting,container,false);
        initView(view);
        initEvent();
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this,view);
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.SP_USER,0);
        String path = sp.getString(Constants.PHOTO_PATH,"");
        if(!TextUtils.isEmpty(path)) {
            int size = (int)(80 * getResources().getDisplayMetrics().density + 0.5f);
            size *= size;
            Bitmap bitmap = ImageUtil.createImageThumbnail(path,size);
            if(bitmap != null) {
                imgPhoto.setImageDrawable(new CircleImageDrawable(bitmap));
                bitmap = null;
            }
        }
        String name = sp.getString(Constants.NICKNAME,"");
        if(!TextUtils.isEmpty(name)) {
            tvName.setText(name);
        }

        String[] channels = getResources().getStringArray(R.array.channel);
        tvCurrentChannel.setText(channels[MainActivity.CURRENT_CHANNEL]);
        channels = null;
    }

    public void setCallbackInSF ( CallbackInSF callbackInSF) {
        this.callbackInSF  = callbackInSF;
    }

    private void initEvent() {
        rlPersonal.setOnClickListener(this);
        imgSwitch.setOnClickListener(this);
        tvChangeChannel.setOnClickListener(this);
        tvClearCache.setOnClickListener(this);
        tvAbout.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_personal_setting:
                //go to personal center

                break;
            case R.id.img_switch_sounds:
                //save or not save sounds data
                if(imgSwitch.isSelected()) {
                    imgSwitch.setSelected(false);
//                    setSaveAllSounds(false);
                }else {
                    imgSwitch.setSelected(true);
//                    setSaveAllSounds(true);
                }
                break;
            case R.id.tv_change_channel:
                //change channel
                if(callbackInSF != null) callbackInSF.chageChannelRequest();
                break;
            case R.id.tv_clear_cache:
                //clear cache
                break;
            case R.id.tv_about:
                //about
                break;
        }

    }

//    private void setSaveAllSounds(boolean isSave){
//        if(callbackInSettingFragment != null){
//            callbackInSettingFragment.isSaveAllSounds(isSave);
//        }
//    }

//    private CallbackInSettingFragment callbackInSettingFragment;
//
//    public void setCallback(CallbackInSettingFragment callbackInSettingFragment){
//        this.callbackInSettingFragment = callbackInSettingFragment;
//    }
//
//    public interface CallbackInSettingFragment{
//        void isSaveAllSounds(boolean isSave);
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        setCallbackInSF(null);
    }

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
