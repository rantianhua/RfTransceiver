package com.rftransceiver.group;

import android.app.ListFragment;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rftransceiver.R;
import com.rftransceiver.adapter.GroupItemsAdapter;

import java.util.List;

/**
 * Created by rantianhua on 15-6-5.
 */
public abstract class BaseGroupFragment extends ListFragment {

    public GroupItemsAdapter adapter;
    public Callback callback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group,container,false);
        initView(v);
        return v;
    }

    public interface Callback {
        List<ScanResult> getScanResults();
        void getGroupData(ScanResult result);
    }

    public abstract void initView(View v);
    public abstract void setCallback(Callback call);

    @Override
    public void onDestroy() {
        super.onDestroy();
        callback = null;
        setListAdapter(null);
        adapter.clear();
        adapter = null;
    }
}
