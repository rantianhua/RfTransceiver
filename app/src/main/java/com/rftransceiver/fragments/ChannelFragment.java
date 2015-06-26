package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.util.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-26.
 */
public class ChannelFragment extends Fragment {

    @InjectView(R.id.btn_sure_channel)
    Button btnSure;
    @InjectView(R.id.sp_channel)
    Spinner spChannel;

    /**
     * channels
     */
    private String[] channels;

    /**
     * selected channel
     */
    private int selectedChannel;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        channels = getResources().getStringArray(R.array.channel);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_channel,container,false);
        initView(v);
        initEvent();
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedChannel == MainActivity.CURRENT_CHANNEL) {
                    Toast.makeText(getActivity(),"当前已在"+channels[selectedChannel],Toast.LENGTH_SHORT).show();
                    return;
                }
                if(getTargetFragment() == null) return;
                Intent intent = new Intent();
                intent.putExtra(Constants.SELECTED_CHANNEL,selectedChannel);
                getTargetFragment().onActivityResult(SettingFragment.REQUEST_CHANNEL,
                        Activity.RESULT_OK, intent);
                intent = null;
            }
        });
    }

    private void initEvent() {
        spChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedChannel = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        channels = null;
    }

}
