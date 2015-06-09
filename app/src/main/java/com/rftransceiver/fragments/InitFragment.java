package com.rftransceiver.fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-1.
 * this fragment used to setting some content such as user's nickname, the app's password
 */
public class InitFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.img_init_camera)
    ImageView camera;
    @InjectView(R.id.ibn_init_cancel)
    ImageButton ibnCancel;
    @InjectView(R.id.btn_init_ok)
    Button btnOk;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_init,container,false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);
        camera.setOnClickListener(this);
        ibnCancel.setOnClickListener(this);
        btnOk.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_init_ok:
                break;
            case R.id.ibn_init_cancel:
                break;
            case R.id.img_init_camera:
                break;
            default:
                break;
        }
    }
}
