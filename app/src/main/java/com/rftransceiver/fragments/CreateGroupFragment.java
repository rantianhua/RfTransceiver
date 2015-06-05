package com.rftransceiver.fragments;

import android.os.Bundle;
import android.view.View;

import com.rftransceiver.adapter.GroupItemsAdapter;
import com.rftransceiver.group.BaseGroupFragment;

/**
 * Created by rantianhua on 15-6-5.
 */
public class CreateGroupFragment extends BaseGroupFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new GroupItemsAdapter(getActivity());
    }

    @Override
    public void setCallback(Callback call) {
        callback = call;
    }

    @Override
    public void initView(View v) {
        setListAdapter(adapter);
    }

}
