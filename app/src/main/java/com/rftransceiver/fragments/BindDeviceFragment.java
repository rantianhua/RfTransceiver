package com.rftransceiver.fragments;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.blutooth.le.ScanBle;
import com.rftransceiver.R;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;

import java.util.ArrayList;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-13.
 * 用来搜索绑定设备的类
 */
public class BindDeviceFragment extends ListFragment implements ScanBle.ScanBleListener{

    @InjectView(R.id.pb_search_devices)
    ProgressBar pbSearching;    //正在搜索设备的提示
    @InjectView(R.id.tv_handle_bind_device)
    TextView tvHandle;  //确定连接设备或者重新搜索设备

    private ProgressDialog pd;  //提示用户当前正在处理某些事情
    //搜索蓝牙的实体类
    private ScanBle scanBle;
    //保存搜索到的设备
    private ArrayList<BluetoothDevice> devices;
    //记录当前选择的listView中的视图
    private View selectedView;
    //回调接口
    private CallbackInBindDeviceFragment callback;
    //将要连接的设备
    private BluetoothDevice waitConnectDevice;
    //在主线程中接收异步消息
    private static Handler mainHandler;
    //在资源文件中定义好的文字信息
    private String textSure,textResearch;
    //用来定时关闭搜索
    private Runnable scaRun;
    //标识是否有请求连接
    private boolean requestConnection = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHandler();
        devices = new ArrayList<>();
        textResearch = getString(R.string.restart_search);
        textSure = getString(R.string.sure);
        pd = new ProgressDialog(getActivity());
    }

    private void initHandler() {
        mainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        //搜索到一个设备
                        BluetoothDevice device = (BluetoothDevice)msg.obj;
                        devices.add(device);
                        CommonAdapter commonAdapter = (CommonAdapter) getListAdapter();
                        commonAdapter.notifyDataSetChanged();
                        break;
                    case 1:
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 向MainActivity请求搜索设备，返回BlutoothAdapter用来实例化ScanBle
     */
    private void initScanBle() {
        if(callback == null || scanBle != null) return;
        //先检查服务有没有绑定完毕
        if(!callback.isBleServiceBinded()) {
            //100毫秒后重新检查一遍
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initScanBle();
                }
            },100);
            return;
        }
        BluetoothAdapter adapter = callback.requestScanDevice();
        if(adapter != null) {
            //蓝牙已打开，可以开始搜索设备
            scanBle = ScanBle.getInstance(adapter,this);
            startSearch();
        }else {
            //先打开蓝牙
            openBle();
        }
    }

    /**
     * 向MainActivity请求打开蓝牙
     */
    private void openBle() {
        if(callback != null) {
            callback.openBle();
        }
    }

    /**
     * 蓝牙已打开
     */
    public void bleOpend() {
        if(pd != null && pd.isShowing()) {
            pd.dismiss();
        }
        initScanBle();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(pbSearching.getVisibility() == View.VISIBLE) {
            if(scanBle.stopScan()) {
                cancelSearch();
            }
        }
        if(pd.isShowing()) {
            pd.dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bind_device,container,false);
        initView(v);
        //初始化搜索实例
        initScanBle();
        return v;
    }

    //定时搜索设备,10秒后自动停止搜索
    public void startSearch() {
        scaRun = scanBle.startScan();
        if(scaRun != null ) {
            tvHandle.setVisibility(View.INVISIBLE);
            pbSearching.setVisibility(View.VISIBLE);
            mainHandler.postDelayed(scaRun, 10000);
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
        tvHandle.setVisibility(View.VISIBLE);
        tvHandle.setText(textResearch);
        tvHandle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tvHandle.getText().toString().equals(textResearch)) {
                    startSearch();
                } else if (tvHandle.getText().toString().equals(textSure)) {
                    requestConnection = true;
                    pd.setMessage("正在绑定...");
                    pd.show();
                    connectDevice();
                }
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (selectedView != null) {
            selectedView.setSelected(false);
        }
        if(pbSearching.getVisibility() == View.VISIBLE) {
            cancelSearch();
            if(scaRun != null) {
                mainHandler.removeCallbacks(scaRun);
            }
            scanBle.stopScan();
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

    public boolean getRequestConnection() {
        return requestConnection;
    }

    /**
     * 设备已连接
     */
    public void deviceConnected() {
        if(pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }


    //设置回调接口
    public void setCallback(CallbackInBindDeviceFragment callback) {
        this.callback = callback;
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
        mainHandler.obtainMessage(0,-1,-1,device).sendToTarget();
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
         * 连接选中的设备
         * @param device
         */
        void connectDevice(BluetoothDevice device);

        /**
         * @return 已实例化好的BluetoothAdapter对象，
         * 用来实例化ScanBle对象
         */
        BluetoothAdapter requestScanDevice();

        /**
         * 请求打开蓝牙
         */
        void openBle();

        /**
         * 检查BleService是否已经绑定好
         * @return
         */
        boolean isBleServiceBinded();

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
