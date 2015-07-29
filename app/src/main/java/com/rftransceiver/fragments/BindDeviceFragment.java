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

import com.brige.blutooth.le.ScanBle;
import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-13.
 */
public class BindDeviceFragment extends ListFragment implements ScanBle.ScanBleListener{

    @InjectView(R.id.pb_search_devices)
    ProgressBar pbSearching;
    @InjectView(R.id.tv_handle_bind_device)
    TextView tvHandle;  //确定连接设备或者重新搜索设备

    private BluetoothAdapter adapter;

    private ProgressDialog pd;

    //搜索蓝牙的实体类
    private ScanBle scanBle;

    /**
     * searched devices
     */
    private ArrayList<BluetoothDevice> devices;

    //记录当前选择的item
    private View selectedView;

    //回调接口
    private CallbackInBindDeviceFragment callback;

    //将要连接的设备
    private BluetoothDevice waitConnectDevice;

    //在主线程中接收异步消息
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    //在资源文件中定义好的文字信息
    private String textSure,textResearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        devices = new ArrayList<>();
        textResearch = getString(R.string.restart_search);
        textSure = getString(R.string.sure);
        pd = new ProgressDialog(getActivity());
        scanBle = ScanBle.getInstance(adapter,this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(pbSearching.getVisibility() == View.VISIBLE) {
            if(scanBle.stopScan()) {
                cancelSearch();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bind_device,container,false);
        initView(v);
        startSearch();
        return v;
    }

    //搜索设备
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
                devices, R.layout.list_item_search_devices) {
            @Override
            public void convert(CommonViewHolder helper, BluetoothDevice item) {
                helper.setText(R.id.tv_device_name_list, item.getName());
            }
        });
        tvHandle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tvHandle.getText().toString().equals(textResearch)) {
                    startSearch();
                }else if(tvHandle.getText().toString().equals(textSure)) {
                    pd.setMessage("正在绑定...");
                    pd.show();
                    connectDevice();
                }
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(selectedView != null) {
            selectedView.setSelected(false);
        }
        if(pbSearching.getVisibility() == View.VISIBLE) {
            cancelSearch();
        }
        tvHandle.setText(textSure);
        selectedView = v.findViewById(R.id.tv_device_name_list);
        selectedView.setSelected(true);
        waitConnectDevice = devices.get(position);
    }

    /**
     *连接选择的设备
     */
    private void connectDevice() {
        if(callback != null) {
            callback.connectDevice(waitConnectDevice);
        }
    }

    /**
     * 设备已连接
     */
    public void deviceConnected() {
        if(pd != null && pd.isShowing()) pd.dismiss();
        setCallback(null);
    }

    //设置回调接口
    public void setCallback(CallbackInBindDeviceFragment callback) {
        this.callback = callback;
    }

    //定时搜索设备
    public void searchDevices() {
        Runnable run = scanBle.startScan();
        if(run != null ) {
            tvHandle.setVisibility(View.INVISIBLE);
            pbSearching.setVisibility(View.VISIBLE);
            new Handler(Looper.myLooper()).postDelayed(run, 10000);
        }
    }

    //取消搜索
    private void cancelSearch() {
        pbSearching.setVisibility(View.INVISIBLE);
        tvHandle.setVisibility(View.VISIBLE);
        tvHandle.setText(textResearch);
        if(devices != null && devices.size() == 0 && getActivity() != null) {
            Toast.makeText(getActivity(),"没有查找到任何设备",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ScanBle中的回调方法，
     * @param device 搜索到的BLE设备
     */
    @Override
    public void scanBleResult(final BluetoothDevice device) {
        if (device == null || devices == null) return;
        if (devices.contains(device)) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if(devices != null) {
                    devices.add(device);
                    CommonAdapter commonAdapter = (CommonAdapter) getListAdapter();
                    commonAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * ScanBle中的回调函数，通知搜索已结束
     */
    @Override
    public void scanStoped() {
        cancelSearch();
    }

    public interface CallbackInBindDeviceFragment {
        /**
         * connect selected device
         * @param device ble device
         */
        void connectDevice(BluetoothDevice device);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        devices = null;
        if(pd.isShowing()) pd.dismiss();
        pd = null;
        setCallback(null);
    }
}
