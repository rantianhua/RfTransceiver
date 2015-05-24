package com.rftransceiver.activity;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rftransceiver.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-5-21.
 */
public class SearchActivity extends ListActivity implements View.OnClickListener{
    @InjectView(R.id.rl_loading_dialog_bluetooth_connect)
    RelativeLayout rlLoading;
    @InjectView(R.id.btn_cancel_discovery)
    Button btnCancel;

    public static Connectlistener connectlistener = null;
    public static MainActivity.Blue_Style style = null;

    private BluetoothAdapter adapter = null;

    private Set<BluetoothDevice> devices = new HashSet<>();    //record scanned device

    private ScanCallback callback = null;
    private BluetoothLeScanner scanner = null;
    private BroadcastReceiver receiver = null;

    private BluetoothAdapter.LeScanCallback leScanCallback = null;

    private LeDeviceListAdapter listAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bluetooth_connect);
        initView();

        if(style == MainActivity.Blue_Style.Ble) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = bluetoothManager.getAdapter();
        }else {
            adapter = BluetoothAdapter.getDefaultAdapter();
        }

        listAdapter = new LeDeviceListAdapter();

        setListAdapter(listAdapter);
    }

    @TargetApi(21)
    private void initCallback() {
        callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if(result == null) return;
                final BluetoothDevice device = result.getDevice();
                if(device == null) return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.addDevice(device);
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
    }

    private void initLeScanCallback() {
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int i, byte[] bytes) {
                if(device == null) return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.addDevice(device);
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
    }

    private void initView() {
        ButterKnife.inject(this);
        showLoading(false);
        btnCancel.setOnClickListener(this);
    }

    private void showLoading(boolean b) {
        rlLoading.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadPairedDevices() {
        //展示已配对的设备
        Set<BluetoothDevice> pairedDevices  = adapter.getBondedDevices();
        for(BluetoothDevice device : pairedDevices) {
            if(device != null) {
                listAdapter.addDevice(device);
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    private void  initNormalReceive() {
        //监听ACTION_FOUND事件
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    listAdapter.addDevice(device);
                    listAdapter.notifyDataSetChanged();
                }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    showLoading(false);
                }
            }
        };
    }


    @Override
    public void onResume() {
        super.onResume();
        if(style == MainActivity.Blue_Style.Normal) {
            loadPairedDevices();
            if(receiver == null) {
                initNormalReceive();
            }
            //注册监听器
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(receiver, filter);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if(style == MainActivity.Blue_Style.Normal) {
            //解除监听器
            unregisterReceiver(receiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelSearch();
        if(connectlistener != null) {
            connectlistener = null;
        }
        listAdapter.clear();
        listAdapter = null;
        style = null;
        devices.clear();
        devices = null;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = listAdapter.getDevice(position);
        if (device == null) return;
        connectlistener.startConnect(device);
        finish();
    }

    //扫描蓝牙设备
    private void searchDevices() {
        showLoading(true);
        if(style == MainActivity.Blue_Style.Normal) {
            adapter.startDiscovery();
        }else if(style == MainActivity.Blue_Style.Ble) {
            if(Build.VERSION.SDK_INT >= 21) {
                if(callback == null) {
                    initCallback();
                }
                if(scanner == null) {
                    scanner = adapter.getBluetoothLeScanner();
                }
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @TargetApi(21)
                    @Override
                    public void run() {
                        cancelSearch();
                    }
                },10000);
                scanner.startScan(callback);
            }else {
                initLeScanCallback();
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cancelSearch();
                    }
                }, 10000);
                adapter.startLeScan(leScanCallback);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel_discovery:
                if(btnCancel.getText().equals(getResources().getString(R.string.start_search))) {
                    searchDevices();
                    btnCancel.setText(R.string.cancel_discovery);
                    break;
                }else{
                    cancelSearch();
                    btnCancel.setText(R.string.start_search);
                    break;
                }
        }
    }

    //stop to search device
    @TargetApi(21)
    private void cancelSearch() {
        rlLoading.setVisibility(View.INVISIBLE);
        btnCancel.setText(getString(R.string.start_search));
        if(style == MainActivity.Blue_Style.Ble) {
            if(scanner != null) {
                scanner.stopScan(callback);
            }else {
                adapter.stopLeScan(leScanCallback);
            }
        }else if(style == MainActivity.Blue_Style.Normal) {
            adapter.cancelDiscovery();
        }
    }

    //回调接口，开启蓝牙连接（客户端连接）
    public interface Connectlistener{
        void startConnect(BluetoothDevice device);
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = SearchActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
