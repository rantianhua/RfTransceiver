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
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.util.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-21.
 */
public class MyDeviceFragment extends Fragment {

    private boolean haveBindDevice = false;
    private SharedPreferences sp;
    private CallbackInMyDevice callback;

    @InjectView(R.id.btn_handle_device)
    Button btnHandle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getActivity().getSharedPreferences(Constants.SP_USER,0);
        String binAddress = sp
                .getString(Constants.BIND_DEVICE_ADDRESS,null);
        haveBindDevice = !TextUtils.isEmpty(binAddress);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mydevice,container,false);
        initView(view);
        initEvent();
        return view;
    }

    private void initEvent() {
        btnHandle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = btnHandle.getText().toString();
                if(getString(R.string.bind_device).equals(text)) {
                    //to bind a device
                    if(callback != null) callback.bindDevice();
                }else if(getString(R.string.unbind_device).equals(text)){
                    //to unbind bounded device
                    unbindDevice();
                }

            }
        });
    }

    private void initView(View view) {
        ButterKnife.inject(this,view);
        if(haveBindDevice) {
            btnHandle.setText(getString(R.string.unbind_device));
        }else {
            btnHandle.setText(getString(R.string.bind_device));
        }
    }

    /**
     * unbind have bounded device
     */
    private void unbindDevice() {
        if(callback == null) return;
        callback.unbindDevice();
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Constants.BIND_DEVICE_ADDRESS, "");
        editor.apply();
        Toast.makeText(getActivity(),"已解除绑定",Toast.LENGTH_SHORT).show();
        btnHandle.setText(getString(R.string.bind_device));
    }

    public void setCallback(CallbackInMyDevice callback) {
        this.callback = callback;
    }

    public CallbackInMyDevice getCallback() {
        return callback;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setCallback(null);
    }

    public interface CallbackInMyDevice {
        void bindDevice();
        void unbindDevice();
    }
}
