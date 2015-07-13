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

import org.w3c.dom.Text;

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
    @InjectView(R.id.tv_handle_bind_device)
    TextView tvHandle;

    private BluetoothAdapter adapter;

    private ProgressDialog pd;
    /**
     * callback after find a deivce
     */
    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int i, byte[] bytes) {
            if (device == null) return;
            if (devices.contains(device)) return;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    devices.add(device);
                    CommonAdapter commonAdapter = (CommonAdapter) getListAdapter();
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
    //private LoadDialogFragment loadDialogFragment;

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

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

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
    }

    @Override
    public void onResume() {
        super.onResume();
        if(pbSearching.getVisibility() == View.INVISIBLE) {
            startSearch();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(pbSearching.getVisibility() == View.VISIBLE) {
            cancelSearch();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bind_device,container,false);
        initView(v);
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
        if(pd.isShowing()) pd.dismiss();
        setCallback(null);
    }

    public void setCallback(CallbackInBindDeviceFragment callback) {
        this.callback = callback;
    }

    //search bluetooth device
    public void searchDevices() {
        pbSearching.setVisibility(View.VISIBLE);
        tvHandle.setVisibility(View.INVISIBLE);
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
        tvHandle.setVisibility(View.VISIBLE);
        tvHandle.setText(textResearch);
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
    public void onDestroy() {
        super.onDestroy();
        devices = null;
        if(pd.isShowing()) pd.dismiss();
        pd = null;
        setCallback(null);
    }
}
