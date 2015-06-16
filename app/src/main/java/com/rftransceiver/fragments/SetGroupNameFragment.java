package com.rftransceiver.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-15.
 */
public class SetGroupNameFragment extends Fragment {

    @InjectView(R.id.et_group_name_set)
    EditText etGroupName;
    @InjectView(R.id.btn_sure_set_group_name)
    Button btnSure;

    /**
     * the instance of OnGroupNameSet
     */
    private OnGroupNameSet listener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setgruopname,container,false);
        initView(view);
        initEvent();
        return view;
    }


    private void initView(View view) {
        ButterKnife.inject(this,view);

    }

    private void initEvent() {
        btnSure.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = etGroupName.getText().toString();
                if(!TextUtils.isEmpty(name)) {
                    if(listener != null) {
                        listener.getGroupName(name);
                    }
                }
            }
        });
    }

    public void setOnGroupNameSetCallback(OnGroupNameSet listener) {
        this.listener = listener;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setOnGroupNameSetCallback(null);
    }

    /**
     * interface interactive with ACtivity
     */
    public interface OnGroupNameSet {
        void getGroupName(String name);
    }
}
