package com.rftransceiver.fragments;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import com.brige.wifi.WifiNetService;
import com.rftransceiver.adapter.GroupItemsAdapter;
import com.rftransceiver.group.BaseGroupFragment;
import com.rftransceiver.group.GroupEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by rantianhua on 15-6-5.
 */
public class AddGroupFragment extends BaseGroupFragment {

    private Timer timerSearch;
    private Handler handler;
    private List<ScanResult> scanResults;
    private boolean getGroupInfo = false;
    private int count;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new GroupItemsAdapter(getActivity());
        scanResults = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case ADD_DATA:
                        //add a wifi hot to list
                        ScanResult result = (ScanResult)msg.obj;
                        if(result == null) return;
                        scanResults.add(result);
                        if(!getGroupInfo) {
                            getGroupInfo = true;
                            callback.getGroupData(result);
                        }
                        break;
                }
            }
        };
    }

    @Override
    public void initView(View v) {
        setListAdapter(adapter);
    }

    @Override
    public void setCallback(Callback call) {
        callback = call;
    }

    /**
     * search wifi hot
     */
    public void startSearch() {
        timerSearch = new Timer();
        timerSearch.schedule(new TimerTask() {
            @Override
            public void run() {
                List<ScanResult> scans = callback.getScanResults();
                for (ScanResult result : scans) {
                    if((result.SSID.startsWith(WifiNetService.WIFI_HOT_HEADER)
                        || result.SSID.startsWith("\""+WifiNetService.WIFI_HOT_HEADER))
                        && !containResult(result)) {
                        handler.obtainMessage(ADD_DATA, -1, -1, result).sendToTarget();
                    }
                }
            }
        },0,6000);
    }

    private boolean containResult(ScanResult result) {
        for(int i = 0;i < scanResults.size();i++) {
            if(scanResults.get(i).SSID.equalsIgnoreCase(result.SSID)) {
                return true;
            }
        }
        return false;
    }

    public void getAgroupInfo() {
        getGroupInfo = false;
        count++;
        /**
         * get next group info
         */
        if(count < scanResults.size()) {
            if(!getGroupInfo) {
                callback.getGroupData(scanResults.get(count));
                getGroupInfo = true;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(timerSearch != null) {
            timerSearch.cancel();
            timerSearch = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanResults.clear();
        scanResults = null;
        handler = null;
    }

    private final int ADD_DATA = 0;   //notify adapter to update data

}
