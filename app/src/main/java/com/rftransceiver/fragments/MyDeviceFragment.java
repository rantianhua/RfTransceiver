package com.rftransceiver.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
    @InjectView(R.id.img_top_left)
    ImageView imgBack;
    @InjectView(R.id.tv_title_left)
    TextView tvTitle;
    @InjectView(R.id.tv_mydevice_name)
    TextView tvMydevice;

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
                if (btnHandle.isSelected()) {
                    //to bind a device
                    if (callback != null) callback.bindDevice();
                } else {
                    //to unbind bounded device
                    unbindDevice();
                }
            }
        });
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStackImmediate();
            }
        });
    }

    private void initView(View view) {
        ButterKnife.inject(this, view);
        if(haveBindDevice) {
            btnHandle.setText(R.string.unbind_device);
            btnHandle.setSelected(false);
        }else {
            btnHandle.setText(R.string.bind_device);
            btnHandle.setSelected(true);
        }

        imgBack.setImageResource(R.drawable.back);
        tvTitle.setText(R.string.my_device);

        String name = sp.getString(Constants.BIND_DEVICE_NAME,"未绑定任何设备");
        tvMydevice.setText(name);
        name = null;
    }

    /**
     * unbind have bounded device
     */
    private void unbindDevice() {
        if(callback == null) return;
        callback.unbindDevice();
        btnHandle.setText(R.string.bind_device);
        tvMydevice.setText("已解绑" + tvMydevice.getText().toString());
        btnHandle.setSelected(true);
    }

    public void setCallback(CallbackInMyDevice callback) {
        this.callback = callback;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("mydevie", "onresume...");
        if(callback != null) callback.openScrollLockView(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("mydevie", "ondestroy...");
        if(callback != null) callback.openScrollLockView(true);
        setCallback(null);
    }

    public interface CallbackInMyDevice {
        void bindDevice();
        void unbindDevice();
        void openScrollLockView(boolean open);
    }
}
