package com.rftransceiver.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rftransceiver.R;

import butterknife.ButterKnife;

/**
 * Created by rth on 15-7-22.
 */
public class ContactsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts,container,false);
        initView(view);
        initEvent();
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this, view);
    }

    private void initEvent() {
    }

}
