package com.rftransceiver.activity;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.blutooth.le.BluetoothLeService;
import com.my_interface.SendMessageListener;
import com.rftransceiver.R;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.fragments.BindDeviceFragment;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.fragments.LoadDialogFragment;
import com.rftransceiver.util.Constants;
import com.source.DataPacketOptions;
import com.source.parse.ParseFactory;
import com.source.sounds.Audio_Reciver;
import com.source.sounds.Audio_Recorder;
import com.source.sounds.SoundsEntity;
import com.source.text.TextEntity;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity implements View.OnClickListener,
        SearchActivity.Connectlistener,SendMessageListener,HomeFragment.CallbackInHomeFragment,
        BindDeviceFragment.CallbackInBindDeviceFragment{

    @InjectView(R.id.img_menu_photo)
    ImageView imgPhoto;
    @InjectView(R.id.tv_menu_name)
    TextView tvName;

    private final String TAG = getClass().getSimpleName();

    /**
     * the reference of BlueLeService
     */
    private BluetoothLeService bluetoothLeService;
    /**
     * true if is receiving data
     */
    private volatile boolean isReceiving = false;
    /**
     *
     */
    private boolean findWriteCharac = false;

    /**
     * indicate weather the ble device bind or not
     */
    private boolean deviceBinded = false;

    /**
     * the handler to interactive between child thread and Ui thread
     */
    private Handler dataExchangeHandler =  null;
    /**
     * the text entity to decide how packet text data to be sent
     */
    private TextEntity textEntity;
    /**
     * the recorder to record sounds and send
     */
    private Audio_Recorder record;
    /**
     * handle the received sounds data---to play them
     */
    private Audio_Reciver receiver;
    /**
     * manage how to parse received data
     */
    private  ParseFactory parseFactory;

    /**
     * save the text waiting to be sent
     */
    private String sendText;

    /**
     * the reference of LoadDialogFragment
     */
    private LoadDialogFragment loadDialogFragment;
    /**
     * the reference of BindDeviceFragment
     */
    private BindDeviceFragment bindDeviceFragment;
    /**
     *  the reference of HomeFragment
     */
    private HomeFragment homeFragment;

    /**
     * the menu to mark send action
     */
    private SendAction action = SendAction.NONE;
    public enum SendAction {
        TEXT,
        SOUNDS,
        NONE
    }

    /**
     * when in BindDeviceFragment, if the bluetooth is opening this will be true
     * after bluetooth is opened , the BindDevicesFragment will start to search device
     */
    public static boolean needSearchDevice = false;

    /**
     * the connected device's name
     */
    private String deviceName;

    /**
     *  Handles various events fired by the Service.
        ACTION_GATT_CONNECTED: connected to a GATT server.
     ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
     or notification operations.
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                if(bindDeviceFragment != null) {
                    bindDeviceFragment.deviceConnected();
                    deviceBinded = true;
                    initHomeFragment();
                    changeFragment(homeFragment);
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                deviceBinded = false;
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                findWriteCharac = true;
                if(homeFragment != null) {
                    homeFragment.writeable = true;
                }
            }
        }
    };


    /**
     * callback when BlueLeService bind or unbind
     */
    private final ServiceConnection serviceConnectionBle = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            bluetoothLeService.setSendMessageListener(MainActivity.this);
            if (!bluetoothLeService.initialize()) {
                showToast("Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService.setSendMessageListener(null);
            bluetoothLeService = null;
        }
    };

    /**
     * receive the state of bluetooth
     */
    private final BroadcastReceiver bluetoothSate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        showToast(getString(R.string.bluetooth_closed));
                        if(bluetoothLeService != null) {
                            bluetoothLeService.disconnect();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if(loadDialogFragment != null && loadDialogFragment.isVisible()) {
                            loadDialogFragment.dismiss();
                        }
                        if(needSearchDevice && bindDeviceFragment != null) {
                            needSearchDevice = false;
                            bindDeviceFragment.searchDevices();
                        }
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_none);
        System.loadLibrary("speex");

        initView();

        //open bluetooth and start bluetooth sever
        openBluetooth();
        initDataExchangeHandler();
        iniInterphone();
        //bind the ble service
        bindService(new Intent(this, BluetoothLeService.class), serviceConnectionBle, BIND_AUTO_CREATE);
    }

    private void initView() {

        ButterKnife.inject(this);
        SharedPreferences sp = getSharedPreferences(Constants.SP_USER,0);
        String name = sp.getString(Constants.NICKNAME,"");
        if(!TextUtils.isEmpty(name)) {
            tvName.setText(name);
        }
        name = null;
        String photoPath = sp.getString(Constants.PHOTO_PATH,"");
        if(!TextUtils.isEmpty(photoPath)) {
            imgPhoto.setImageDrawable(new CircleImageDrawable(
                    BitmapFactory.decodeFile(photoPath)));
        }
        photoPath = null;

        if(deviceBinded) {
            initHomeFragment();
            changeFragment(homeFragment);
        }else {
           initBindDeiveFragment();
            changeFragment(bindDeviceFragment);
        }
    }

    /**
     *
     * @param fragment needed display
     */
    private void changeFragment(Fragment fragment) {
        getFragmentManager().beginTransaction().replace(R.id.frame_content,fragment)
                .commit();
    }

    private void iniInterphone() {
        //initial the receiver
        receiver = new Audio_Reciver();
        DataPacketOptions soundsOptions = new DataPacketOptions(DataPacketOptions.Data_Type_InOptions.sounds,
                9);
        SoundsEntity soundsEntity = new SoundsEntity();
        soundsEntity.setOptions(soundsOptions);
        soundsEntity.setSendListener(this);

        //initial the recorder
        record = new Audio_Recorder();
        record.setSoundsEntity(soundsEntity);

        //initial the text entity
        textEntity = new TextEntity();
        DataPacketOptions textOptions = new DataPacketOptions(DataPacketOptions.Data_Type_InOptions.text,
                3);
        textEntity.setOptions(textOptions);
        textEntity.setSendListener(this);

        //initial parseFactory
        parseFactory = new ParseFactory();
        parseFactory.setHandler(dataExchangeHandler);
        parseFactory.setSoundsOptions(soundsOptions);
        parseFactory.setTextOptions(textOptions);

        soundsOptions = null;
        textOptions = null;
    }

    /**
     * init HomeFragment , avoid forget to set callback
     */
    private void initHomeFragment() {
        if(homeFragment == null) {
            homeFragment = new HomeFragment();
            homeFragment.setCallback(this);
        }
    }

    /**
     * init BindDeviceFragment , avoid forget to set callback
     */
    private void initBindDeiveFragment() {
        if(bindDeviceFragment == null) {
            bindDeviceFragment = new BindDeviceFragment();
            bindDeviceFragment.setCallback(this);
        }
    }

    private void initDataExchangeHandler() {
        dataExchangeHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constants.MESSAGE_READ:
                        switch (msg.arg1) {
                            case 0:
                                switch (msg.arg2) {
                                    case 0:
                                        //start to receive sounds data
                                        isReceiving = true;
                                        receiver.startReceiver();
                                        if(homeFragment != null) {
                                            homeFragment.receivingData(0,null);
                                        }
                                        break;
                                    case 1:
                                        //end to receive sounds data
                                        stopReceiveSounds();
                                        break;
                                    case 2:
                                        //cache the received sounds data
                                        byte[] sound = (byte[]) msg.obj;
                                        receiver.cacheData(sound, Constants.Small_Sounds_Packet_Length);
                                        break;
                                }
                                break;
                            case 1:
                                //receive text data
                                byte[] data = (byte[]) msg.obj;
                                if(homeFragment != null) {
                                    homeFragment.receivingData(1,new String(data));
                                }
                                break;
                            case 2:
//                                switch (msg.arg2) {
//                                    case 0:
//                                        updateChannel(getString(R.string.channel_1));
//                                        break;
//                                    case 1:
//                                        updateChannel(getString(R.string.channel_2));
//                                        break;
//                                    case 2:
//                                        updateChannel(getString(R.string.channel_3));
//                                        break;
//                                    case 3:
//                                        updateChannel(getString(R.string.channel_4));
//                                        break;
//                                }
                                break;
                            case 3:
                                showToast("设置同步字成功");
                                break;
                            case 4:
                                showToast("读到rssi值是：" + msg.arg2);
                                break;
                            case 5:
                                if (msg.arg2 == 0) {
                                    isReceiving = false;
                                    stopReceiveSounds();
                                    if (action == SendAction.SOUNDS) {
                                        //start record
                                        action = SendAction.NONE;
                                        record.startRecording();
                                        if(homeFragment != null) {
                                            homeFragment.startSendingSounds();
                                        }
                                    } else if (action == SendAction.TEXT) {
                                        action = SendAction.NONE;
                                        sendText();
                                    }
                                } else if (msg.arg2 == 1) {
                                    if (action != SendAction.NONE) {
                                        showToast("信道占用中，不能发送");
                                    }
                                }
                                break;
                            case 6:
                                resetCms(true);
                                showToast("接收到错误数据");
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        break;
                    case Constants.MESSAGE_TOAST:
                        showToast(msg.getData().getString(Constants.TOAST));
                        break;
                }
            }
        };
    }

    private void stopReceiveSounds() {
        receiver.stopReceiver();
        if(homeFragment != null) {
            homeFragment.endReceive(0);
        }
    }

    private void createGroup() {
        Intent intent = new Intent(this,MyWifiActivity.class);
        intent.putExtra(MyWifiActivity.ACTION,MyWifiActivity.CREATE_GROUP);
        startActivity(intent);
    }

    private void addGroup() {
        Intent intent = new Intent(this,MyWifiActivity.class);
        intent.putExtra(MyWifiActivity.ACTION,MyWifiActivity.ADD_GROUP);
        startActivity(intent);
    }

    private void sendAsyncWord() {
        if(!isReceiving && findWriteCharac) {
            Constants.ASYNC_WORD[2] = 9;
            Constants.ASYNC_WORD[3] = 10;
            bluetoothLeService.writeInstruction(Constants.ASYNC_WORD);
        }
    }

    //reset the cms
    private void resetCms(boolean write) {
        receiver.stopReceiver();
        record.stopRecording();
        receiver.clear();
        parseFactory.resetSounds();
        homeFragment.reset();
        isReceiving = false;
        if(write) {
            if(!isReceiving && findWriteCharac) {
                bluetoothLeService.writeInstruction(Constants.RESET);
            }
        }
    }

    //close a bluetooth connection
    private void closeConnection() {
        if(bluetoothLeService != null) {
            bluetoothLeService.disconnect();
        }
    }

    private void changeChanel(byte channel) {
        if(!isReceiving && findWriteCharac) {
            Constants.CHANNEL[2] = channel;
            bluetoothLeService.writeInstruction(Constants.CHANNEL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(bluetoothSate,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(bluetoothSate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        parseFactory = null;

        record.stopRecording();
        receiver.stopReceiver();
        textEntity.setSendListener(null);
        textEntity.setOptions(null);

        record = null;
        receiver = null;
        textEntity = null;

        unbindService(serviceConnectionBle);
        bluetoothLeService = null;
    }

    //open the bluetooth
    private void openBluetooth() {
        BluetoothAdapter bluetoothAdapter = null;
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        try {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //check this device weather support bluetooth or not
        //if support then enable the bluetooth
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                loadDialogFragment = LoadDialogFragment.getInstance("正在打开蓝牙");
                loadDialogFragment.show(getFragmentManager(),null);
                bluetoothAdapter.enable();
            }
        }else {
            showToast(getString(R.string.device_bluetooth_not_support));
            //finish();
        }
    }

    @Override
    public void onClick(View v) {
    }

    private void chenckChannel() {
        if(bluetoothLeService != null){
            bluetoothLeService.writeInstruction(Constants.CHANNEL_STATE);
        }
    }

    private void sendText() {
        if(sendText != null) {
            homeFragment.sendText(sendText);
        }
        textEntity.unpacking(sendText);
    }

    @Override
    public void startConnect(BluetoothDevice device) {
        deviceName = device.getName();
        if(bluetoothLeService != null) {
            bluetoothLeService.connect(device.getAddress());
        }
    }

    private void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void sendPacketedData (byte[] data,boolean end){
        if(data == null) return;
        if(bluetoothLeService != null) {
            bluetoothLeService.write(data);
        }
        data = null;
    }



    @Override
    public void sendUnPacketedData(byte[] data,int mode) {
        switch (mode) {
            case 0:
                //correct data
                if(data != null) {
                    parseFactory.sendToRelativeParser(data);
                }
                break;
            case 2:
                //send failed
                showToast(getString(R.string.send_failed));
                break;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    /**
     * call in HomeFragment
     * @param sendAction    send text Action or sound action
     * @param text  the text wait to be sent
     */
    @Override
    public void send(SendAction sendAction,String text) {
        action = sendAction;
        sendText = text;
        chenckChannel();
    }

    /**
     * call in HomeFragment
     * stop record sounds
     */
    @Override
    public void stopSendSounds() {
        record.stopRecording();
    }

    /**
     * call in BindDeviceFragment
     * @param device ble device
     */
    @Override
    public void connectDevice(BluetoothDevice device) {
        deviceName = device.getName();
        if(bluetoothLeService != null) {
            bluetoothLeService.connect(device.getAddress());
        }
    }
}
