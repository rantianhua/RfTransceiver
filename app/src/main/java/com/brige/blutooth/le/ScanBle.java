package com.brige.blutooth.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Created by rth on 15-7-28.
 * 搜索BLE设备
 */
public class ScanBle {

    private BluetoothAdapter adapter;

    private static ScanBle scanBle; //单一实例

    private ScanBleListener listener;   //接口实例

    private boolean isScanning = false; //正在搜索设备的标识

    //搜索结果的回调
    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int i, byte[] bytes) {
            //将搜索到的设备传给接口调用者
            if(listener != null) listener.scanBleResult(device);
        }
    };

    //特定服务的UUID，缩小搜索范围
//    private static final UUID[] myServices = {UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
//            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")};

    private ScanBle() {

    }

    /**
     * 获取单一实例
     * @param adapter 操作蓝牙
     * @param listener 回调接口
     * @return
     */
    public static ScanBle getInstance(BluetoothAdapter adapter,ScanBleListener listener) {
        if(scanBle == null) {
            scanBle = new ScanBle();
        }
        scanBle.setAdapter(adapter);
        scanBle.setScanLisener(listener);
        return scanBle;
    }

    public void setAdapter(BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    public void setScanLisener(ScanBleListener lisener) {
        this.listener = lisener;
    }

    /**
     * 开始搜索拥有特定服务的设备
     * @return 返回一个Runnable对象用以在主线程中设定一个时间限制，避免一直搜索
     */
    public Runnable startScan() {
        if(isScanning || adapter == null) return null;
        //adapter.startLeScan(myServices, leScanCallback);
        adapter.startLeScan(leScanCallback);
        isScanning = true;
        return new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        };
    }

    /**
     * 停止搜索设备
     */
    public boolean stopScan() {
        if(isScanning && adapter != null) {
            adapter.stopLeScan(leScanCallback);
            isScanning = false;
            if(listener != null) listener.scanStoped();
            return true;
        }else {
            return false;
        }
    }

    //回调接口，发送搜索结果给调用者
    public interface ScanBleListener{
        void scanBleResult(BluetoothDevice device);
        void scanStoped();
    }
}

