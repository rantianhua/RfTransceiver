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

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-26.
 */
public class SettingActivity extends Activity {
    @InjectView(R.id.img_back_setting)
    ImageView imgBack;
    @InjectView(R.id.tv_title_setting)
    TextView tvTitle;

    private String titleChannel;
    private String titleSetting;

    private SettingFragment settingFrag;
    private ChannelFragment channelFrag;

    /**
     * local broadcastReceiver to receive info about change channel
     */
    private final BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null) return;
            if(intent.getAction().equals(Constants.TOAST)) {
                String message = intent.getStringExtra(Constants.TOAST);
                if(!TextUtils.isEmpty(message)) {
                    showToast(message);
                }
            }
        }
    } ;

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SettingActivity.this,message,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private LocalBroadcastManager localBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        titleChannel = getResources().getString(R.string.choose_channel);
        titleSetting = getResources().getString(R.string.setting);
        initView();
        initEvent();
        localBroadcast = LocalBroadcastManager.getInstance(this);
    }

    private void initView() {
        ButterKnife.inject(this);
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

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcast.registerReceiver(localReceiver,new IntentFilter(Constants.TOAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcast.unregisterReceiver(localReceiver);
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
