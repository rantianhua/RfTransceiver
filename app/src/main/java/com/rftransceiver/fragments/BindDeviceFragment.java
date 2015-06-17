package com.rftransceiver.fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-13.
 */
public class BindDeviceFragment extends ListFragment {

    @InjectView(R.id.pb_search_devices)
    ProgressBar pbSearching;

    private BluetoothAdapter adapter;
    /**
     * callback after find a deivce
     */
    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int i, byte[] bytes) {
            if(device == null) return;
            if(devices.contains(device)) return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    devices.add(device);
                    CommonAdapter commonAdapter = (CommonAdapter)getListAdapter();
                    commonAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    /**
     * searched devices
     */
    private ArrayList<BluetoothDevice> devices;

    /**
     * a dialogfragment to indicate is do something
     */
    private LoadDialogFragment loadDialogFragment;

    /**
     * @param savedInstanceState
     * the select item in the listView
     */
    private View selectedView;


    /**
     * the instance of CallbackInBindDeviceFragment
     */
    private CallbackInBindDeviceFragment callback;

    /**
     * the device's password
     */
    private String pasaword;

    /**
     * the device waiting to be connected
     */
    private BluetoothDevice waitConnectDevice;

    private DevicePwdDialogFragment pwdDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        devices = new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bind_device,container,false);
        initView(v);
        startSearch();
        return v;
    }

    private void startSearch() {
        if(!adapter.isEnabled() || adapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
            MainActivity.needSearchDevice = true;
        }else {
            searchDevices();
        }
    }

    private void initView(View v) {
        ButterKnife.inject(this, v);
        setListAdapter(new CommonAdapter<BluetoothDevice>(getActivity(),
                devices,R.layout.list_item_search_devices) {
            @Override
            public void convert(CommonViewHolder helper, BluetoothDevice item) {
                helper.setText(R.id.tv_device_name_list,item.getName());
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(selectedView != null) {
            selectedView.setSelected(false);
        }
        cancelSearch();
        selectedView = v.findViewById(R.id.tv_device_name_list);
        selectedView.setSelected(true);
        if(pwdDialogFragment == null) {
            pwdDialogFragment = new DevicePwdDialogFragment();
            pwdDialogFragment.setTargetFragment(this,REQUEST_PWD);
        }
        pwdDialogFragment.show(getFragmentManager(),null);
        waitConnectDevice = devices.get(position);
    }

    /**
     * connect selected device
     */
    private void connectDevice() {
        if(callback != null) {
            callback.connectDevice(waitConnectDevice);
        }
    }

    /**
     * the device have connected
     */
    public void deviceConnected() {
        if(loadDialogFragment != null && loadDialogFragment.isVisible()) {
            loadDialogFragment.dismiss();
            setCallback(null);
        }
    }

    public void setCallback(CallbackInBindDeviceFragment callback) {
        this.callback = callback;
    }

    //search bluetooth device
    public void searchDevices() {
        pbSearching.setVisibility(View.VISIBLE);
        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                cancelSearch();
            }
        }, 10000);
        adapter.startLeScan(leScanCallback);
    }

    private void cancelSearch() {
        pbSearching.setVisibility(View.INVISIBLE);
        adapter.stopLeScan(leScanCallback);
        if(devices != null && devices.size() == 0) {
            Toast.makeText(getActivity(),"没有查找到任何设备",Toast.LENGTH_SHORT).show();
        }
    }

    public interface CallbackInBindDeviceFragment {
        /**
         * connect selected device
         * @param device ble device
         */
        void connectDevice(BluetoothDevice device);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_PWD && resultCode == Activity.RESULT_OK && data != null) {
            pasaword = data.getStringExtra(DevicePwdDialogFragment.EXTRA_PWD);
            pwdDialogFragment.dismiss();
            if(loadDialogFragment == null) {
                loadDialogFragment = LoadDialogFragment.getInstance("正在绑定");
            }
            loadDialogFragment.show(getFragmentManager(),null);
            connectDevice();
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        devices = null;
        loadDialogFragment =null;
        setCallback(null);
        pwdDialogFragment = null;
    }

    /**
     * the request need by DevicePwdFragment
     */
    public static final int REQUEST_PWD = 300;
}
