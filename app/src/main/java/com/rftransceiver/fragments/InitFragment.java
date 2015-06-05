package com.rftransceiver.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
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
 * Created by rantianhua on 15-6-1.
 * this fragment used to setting some content such as user's nickname, the app's password
 */
public class InitFragment extends Fragment {

    @InjectView(R.id.et_launcher_nicname)
    EditText etNicName;
    @InjectView(R.id.btn_launcher_sure)
    Button btnSure;

    private OnNicknameFinished listener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_launcher,container,false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = etNicName.getText().toString();
                if(!TextUtils.isEmpty(name)) {
//                    SharedPreferences sp = getActivity().getSharedPreferences(Constants.SP_USER,0);
//                    SharedPreferences.Editor editor = sp.edit();
//                    editor.putString(Constants.NICKNAME,name);
//                    editor.apply();
                    if(listener != null) {
                        listener.finishNicname();
                    }
                }
            }
        });
    }

    public void setListener(OnNicknameFinished listener) {
        this.listener = listener;
    }

    public interface OnNicknameFinished {
        void finishNicname();
    }
}
