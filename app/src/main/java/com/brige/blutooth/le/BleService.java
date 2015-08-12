/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brige.blutooth.le;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.rftransceiver.BuildConfig;
import com.rftransceiver.util.Constants;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleService extends Service {

    private final static String TAG = BleService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;    //没有连接
    private static final int STATE_CONNECTING = 1;      //正在连接
    private static final int STATE_CONNECTED = 2;       //连接成功

    //写入数据发送的Characteristic
    private  BluetoothGattCharacteristic writeCharacteristic;

    //读取数据的Characteristic
    private BluetoothGattCharacteristic notifyCharacter;

    //回调函数
    private CallbackInBle callback;

    //记录将要连接的设备的ssid,在判断连接可用时有用
    private String waitConnectDevice;

    //标识是否找到了可读和可写的特性
    private boolean findCharacter = false;

    //扫描设备的实体类
    private ScanBle scanBle;

    //扫描类ScanBle的回调接口
    private final ScanBle.ScanBleListener scanBleListener = new ScanBle.ScanBleListener() {
        @Override
        public void scanBleResult(BluetoothDevice device) {
            if(waitConnectDevice != null) {
                if(device.getAddress().equalsIgnoreCase(waitConnectDevice)) {
                    //表示要连的设备是可用的
                    isCheckDeviceAddress = false;
                    if(Constants.DEBUG) {
                        Log.e("waitConnectDevice","设备是可用的");
                    }
                    scanBle.stopScan(); //停止扫描
                    connect(waitConnectDevice, false);   //再次连接，此次连接不需要检查设备地址的可用性
                }
            }
        }

        @Override
        public void scanStoped() {
            if(isCheckDeviceAddress) {
                isCheckDeviceAddress = false;
                if(Constants.DEBUG) {
                    Log.e("waitConnectDevice","设备是不可用");
                }
                //告知连接的发起者该设备的地址当前不可用
                if(callback != null) callback.deviceNotWork();
            }
        }
    };

    //用来在主线程执行一些延时的操作或者其他必须在主线程执行的操作
    private static final Handler mainHan = new Handler(Looper.getMainLooper());

    //标识是否正在检查要连的设备地址的可用性
    private boolean isCheckDeviceAddress = false;

    //BluetoothAdapter的回调接口
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                // 发现服务,
                try {
                    //参考http://www.apkbus.com/android-204863-1-1.html上的经验说在发现服务之前先sleep一段时间
                    //对于小米机比较有必要
                    Thread.sleep(500);
                }catch (Exception e) {

                }
                mBluetoothGatt.discoverServices();
                //利用接口报告蓝牙连接成功
                Log.e("onConnectionStateChange","设备已连接");
                if(callback != null) callback.bleConnection(true);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                //利用接口报告蓝牙BLE连接断开
                Log.e("onConnectionStateChange","设备已断开");
                if(callback != null) callback.bleConnection(false);
                if(writeCharacteristic != null) {
                    setCharacteristicNotification(
                            writeCharacteristic, false);
                }
                //重新连接后重新查找服务和特性
                findCharacter = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(!findCharacter) {
                    //发送数据服务的UUID
                    BluetoothGattService writeServer = mBluetoothGatt.getService(
                            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
                    );
                    //发送数据特性的UUID
                    writeCharacteristic = writeServer.getCharacteristic(
                            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
                    );
                    //读取数据的服务
                    BluetoothGattService readServer = mBluetoothGatt.getService(
                            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
                    );
                    //读取数据的特性
                    notifyCharacter = readServer.getCharacteristic(
                            UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb")
                    );
                    findCharacter = true;
                }
                //设置发送通知
                setCharacteristicNotification(
                        writeCharacteristic, true);
                //得到读取特性的属性
                final int charaProp = notifyCharacter.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    readCharacteristic(notifyCharacter);
                }
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * 当接收到数据时调用
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                senMyMessage(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status != BluetoothGatt.GATT_SUCCESS) {
                callback.sendUnPacketedData(null, 2);
            }
        }

        /**
         * 当用于通知的characteristic改变时调用
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            senMyMessage(characteristic);
        }
    };

    //缓存收到的数据，以数据包的长度为大小
    private byte[] temp = new byte[Constants.Data_Packet_Length];
    private int index = 0;  //记录缓存接收数据的索引

    /**
     * 将接收到的数据发送给接口调用者
     * @param characteristic
     */
    private void senMyMessage(BluetoothGattCharacteristic characteristic) {

        final byte[] data = characteristic.getValue();
        if(data == null) return;
        int bytes = data.length;
        if (bytes == 20) {
            if(Constants.DEBUG) {
                Log.e("receive",bytes + "");
            }
            try {
                for(int i =0; i < bytes;i++) {
                    temp[index++] =data[i];
                    if(index == Constants.Data_Packet_Length) {
                        //缓存已满，发送给接口调用者
                        index = 0;  //先复位索引，准备接收下一个数据
                        callback.sendUnPacketedData(temp, 0);
                        temp = null;
                        temp = new  byte[Constants.Data_Packet_Length];
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            index = 0;
//            for(int i =0;i < bytes;i++) {
//                Log.e("unknow",String.format("%#X",data[i]));
//            }
        }
    }

    /**
     * 发送数据
     * @param send 要发送的数据
     */
    public void write(byte[] send) {
        if(canWrite()) {
            byte[] sam = new byte[20];
            for(int i =0; i < 3;i++) {
                System.arraycopy(send,i*20,sam,0,20);
                writeCharacteristic.setValue(sam);
                mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            }
            send = null;
            sam = null;
        }
    }

    /**
     * 发送指令
     * @param instruction
     */
    public boolean writeInstruction(byte[] instruction) {
        if(canWrite()) {
            index = 0;  //避免上次遗留的错误数据对本次造成影响
            writeCharacteristic.setValue(instruction);
            mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            if(Constants.DEBUG) {
                Log.e("writeInstruction","write a writeInstruction");
            }
            return true;
        }else {
            return false;
        }
    }

    /**
     * 检查是否能够发送数据
     * @return
     */
    private boolean canWrite() {
        if(callback == null) return false;
        if(mBluetoothGatt == null) {
            //设备未连接
            callback.bleConnection(false);
            return false;
        }else if(writeCharacteristic == null) {
            if(mConnectionState == STATE_DISCONNECTED) {
                //设备未连接
                callback.bleConnection(false);
            }else if(mConnectionState == STATE_CONNECTING) {
                //设备未工作
                callback.deviceNotWork();
            }else if(mConnectionState == STATE_CONNECTED) {
                //设备已连接，但还未初始化
                callback.serviceNotInit();
                //重新搜索服务
                mBluetoothGatt.discoverServices();
            }
            return false;
        }else {
            return true;
        }
    }

    public void setCallback(CallbackInBle listener) {
        this.callback = null;
        this.callback = listener;
    }

    /**
     * 解除绑定、重新启动蓝牙
     */
    public void unBindDevice() {
        disconnect();
        close();
        mBluetoothAdapter.disable();
    }

    /**
     * 返回已实例化的BluetoothAdapter对象
     * @return
     */
    public BluetoothAdapter getBleAdapter() {
        return mBluetoothAdapter;
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * 初始化BluetoothAdater
     * @return
     */
    public boolean initialize() {
        //从系统服务中获取BlutoothAdater
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }

        return true;
    }

    public boolean isBluetoothEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * open bluetooth
     */
    public void openBluetooth() {
        mBluetoothAdapter.enable();
    }

    /**
     * 连接设备
     * @param address
     * @param needResearch
     * @return
     */
    public void connect(final String address,boolean needResearch) {
        if (mBluetoothAdapter == null || address == null || mConnectionState != STATE_DISCONNECTED) {
            return;
        }
        if(needResearch) {
            waitConnectDevice = address;
            // 连接前要搜索该设备，看设备是否在工作
            if(scanBle == null) {
                scanBle = ScanBle.getInstance(mBluetoothAdapter,scanBleListener);
            }
            Runnable run = scanBle.startScan();
            isCheckDeviceAddress = true;
            mainHan.postDelayed(run,4000);
            return;
        }
        //和传入的address建立新的连接
        try {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            mConnectionState = STATE_CONNECTING;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解除已有的连接
     */
    public void disconnect() {
        try {
            mBluetoothGatt.disconnect();
        }catch (Exception e) {

        }
        mConnectionState = STATE_DISCONNECTED;
        findCharacter = false;
    }

    /**
     * 关闭BLE设备
     */
    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        writeCharacteristic = null;
        notifyCharacter = null;
    }

    /**
     * 请求读取数据的“特性”
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 运行在某个“特性”上接收通知
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public interface CallbackInBle {
        /**
         * 发送接收到的数据包
         * @param data
         * @param mode
         */
        void sendUnPacketedData(byte[] data,int mode);

        /**
         * 告知回调方，连接成功或连接断开
         */
        void bleConnection(boolean connect);

        /**
         * 告知调用方，设备未工作
         */
        void deviceNotWork();

        /**
         * 告知回调方服务未初始化
         */
        void serviceNotInit();
    }
}
