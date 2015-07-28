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

    @InjectView(R.id.btn_handle_device)
    Button btnHandle;       //处理“解除绑定”和“绑定设备”的事件
    @InjectView(R.id.img_top_left)
    ImageView imgBack;  //标题栏的返回按钮
    @InjectView(R.id.tv_title_left)
    TextView tvTitle;
    @InjectView(R.id.tv_mydevice_name)
    TextView tvMydevice;    //显示当前设备的名称


    private String bindDeviceName ; //已绑定设备的名称
    private CallbackInMyDevice callback;    //回调接口

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //从SharedPrefernce中获取当前设备的名称
        bindDeviceName = getActivity().getSharedPreferences(Constants.SP_USER,0).getString(Constants.BIND_DEVICE_NAME,
                null);
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
                    //绑定设备
                    if (callback != null) callback.bindDevice();
                } else {//解绑当前设备
                    unbindDevice();
                }
            }
        });
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //退出当前页面
                getFragmentManager().popBackStackImmediate();
            }
        });
    }

    private void initView(View view) {
        ButterKnife.inject(this, view);
        //若已绑定设备，则按钮显示“解除绑定”，否则显示“绑定设备”
        if(!TextUtils.isEmpty(bindDeviceName)) {
            btnHandle.setText(R.string.unbind_device);
            btnHandle.setSelected(false);
        }else {
            btnHandle.setText(R.string.bind_device);
            btnHandle.setSelected(true);
        }

        //为回退按钮添加图片资源
        imgBack.setImageResource(R.drawable.back);
        tvTitle.setText(R.string.my_device);
        //显示绑定设备名称
        tvMydevice.setText(TextUtils.isEmpty(bindDeviceName) ? "未绑定任何设备" : bindDeviceName );
    }

    /**
     * 解除绑定的实现
     */
    private void unbindDevice() {
        if(callback == null) return;
        //利用回调接口执行具体的解绑操作
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
        if(callback != null) callback.openScrollLockView(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(callback != null) callback.openScrollLockView(true);
        setCallback(null);
    }

    public interface CallbackInMyDevice {
        void bindDevice();
        void unbindDevice();
        void openScrollLockView(boolean open);
    }
}
