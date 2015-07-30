package com.rftransceiver.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.fragments.ChannelFragment;
import com.rftransceiver.fragments.SettingFragment;
import com.rftransceiver.util.Constants;

import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-26.
 */
public class SettingActivity extends Activity {
    @InjectView(R.id.img_top_left)
    ImageView imgBack;
    @InjectView(R.id.tv_title_left)
    TextView tvTitle;

    private String titleChannel;
    private String titleSetting;

    private SettingFragment settingFrag;
    private ChannelFragment channelFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        titleChannel = getResources().getString(R.string.choose_channel);
        titleSetting = getResources().getString(R.string.setting);
        initView();
        initEvent();
    }

    private void initView() {
        ButterKnife.inject(this);
        imgBack.setImageResource(R.drawable.back);
        if(settingFrag == null) {
            settingFrag = new SettingFragment();
            settingFrag.setCallbackInSF(new SettingFragment.CallbackInSF() {
                @Override
                public void chageChannelRequest() {
                    if(channelFrag == null) {
                        channelFrag = new ChannelFragment();
                        channelFrag.setTargetFragment(settingFrag,SettingFragment.REQUEST_CHANNEL);
                    }
                    changeFragment(channelFrag,true);
                    tvTitle.setText(titleSetting);
                }

                /**
                 * change channel
                 * @param channel
                 */
                @Override
                public void changeChannel(int channel) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.SELECTED_CHANNEL,channel);
                    setResult(Activity.RESULT_OK,intent);
                    intent = null;
                    SettingActivity.this.finish();
                }
            });
        }
        changeFragment(settingFrag,false);
    }

    private void initEvent() {
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    /**
     * change fragment to show
     * @param fragment
     * @param addToBack if true add the fragment to background
     */
    private void changeFragment(Fragment fragment,boolean addToBack) {
        FragmentTransaction transcation = getFragmentManager().beginTransaction();
        transcation.replace(R.id.frame_container_setting,
                fragment);
        if(addToBack) {
            transcation.addToBackStack(null);
        }
        transcation.commit();
    }
}