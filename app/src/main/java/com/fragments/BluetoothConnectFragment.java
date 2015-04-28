package com.fragments;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rftransceiver.R;

import java.lang.reflect.Method;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Rth on 2015/4/22.
 */
public class BluetoothConnectFragment extends DialogFragment implements View.OnClickListener{

    @InjectView(R.id.rl_loading_dialog_bluetooth_connect)
    RelativeLayout rlLoading;
    @InjectView(R.id.ll_bluetooth_devices)
    LinearLayout llDevices;
    @InjectView(R.id.btn_cancel_discovery)
    Button btnCancel;

    private static Connectlistener connectlistener = null;
    private BluetoothAdapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bluetooth_connect,null,false);
        initView(v);
        dialog.setTitle("查找蓝牙设备");
        dialog.setContentView(v);
        return dialog;
    }

    private void initView(View v) {
        ButterKnife.inject(this, v);
        showLoading(false);
        btnCancel.setOnClickListener(this);
    }

    private void showLoading(boolean b) {
        rlLoading.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadPairedDevices();
    }

    private void loadPairedDevices() {
        //展示已配对的设备
        Set<BluetoothDevice> pairedDevices  = adapter.getBondedDevices();
        for(BluetoothDevice device : pairedDevices) {
            if(device != null) {
                notifyDevices(device);
            }
        }
    }

    //监听ACTION_FOUND事件
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                notifyDevices(device);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showLoading(false);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        //注册监听器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(receiver,filter);
    }


    @Override
    public void onPause() {
        super.onPause();
        //解除监听器
        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(connectlistener != null) {
            connectlistener = null;
        }
    }

    //展示已发现的蓝牙设备
    private void notifyDevices(BluetoothDevice device) {
        String name = device.getName();
        final TextView textView = new TextView(getActivity());
        textView.setText(name);
        textView.setTag(device);
        textView.setOnClickListener(this);
        textView.setPadding(0,10,0,10);
        llDevices.addView(textView);
    }

    //扫描蓝牙设备
    private void searchDevices() {
        showLoading(true);
        adapter.startDiscovery();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel_discovery:
                if(btnCancel.getText().equals(getResources().getString(R.string.start_search))) {
                    searchDevices();
                    btnCancel.setText(R.string.cancel_discovery);
                }else{
                    adapter.cancelDiscovery();
                    btnCancel.setText(R.string.start_search);
                }
                break;
            default:
                //检查设备是否已配对
                BluetoothDevice device =  (BluetoothDevice)v.getTag();
                if(BluetoothDevice.BOND_NONE == device.getBondState()) {
                    //先配对
                    bounDevice(device);
                }else if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //回调接口
                    connectlistener.startConnect((BluetoothDevice)v.getTag());
                    BluetoothConnectFragment.this.dismiss();
                }
                break;
        }
    }

    //蓝牙设备配对
    private void bounDevice(BluetoothDevice device) {
        try {
            Method createBondMethod = BluetoothDevice.class
                    .getMethod("createBond");
            createBondMethod.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BluetoothConnectFragment getInstance(Connectlistener listener) {
        connectlistener = listener;
        return new BluetoothConnectFragment();
    }

    //回调接口，开启蓝牙连接（客户端连接）
    public interface Connectlistener{
        void startConnect(BluetoothDevice device);
    }
}
