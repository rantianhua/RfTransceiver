package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-14.
 */
public class DevicePwdDialogFragment extends BaseDialogFragment {

    @InjectView(R.id.et_pass_dialog_bind)
    EditText etPassword;
    @InjectView(R.id.tv_sure_dialog_bind)
    TextView tvSure;

    @Override
    public View initContentView(ViewGroup container) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bind_device,container,false);
        ButterKnife.inject(this,v);
        return v;
    }

    @Override
    public void initEvent() {
        tvSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_PWD,etPassword.getText().toString());
                if(getTargetFragment() != null) {
                    getTargetFragment().onActivityResult(BindDeviceFragment.REQUEST_PWD,
                            Activity.RESULT_OK,intent);
                }
            }
        });
    }

    public static final String EXTRA_PWD = "pwd";
}
