package com.rftransceiver.activity;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.blutooth.le.BleService;
import com.rftransceiver.R;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.customviews.LockerView;
import com.rftransceiver.fragments.BindDeviceFragment;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.fragments.LoadDialogFragment;
import com.rftransceiver.util.Constants;
import com.source.DataPacketOptions;
import com.source.SendMessageListener;
import com.source.parse.ParseFactory;
import com.source.sounds.Audio_Reciver;
import com.source.sounds.Audio_Recorder;
import com.source.sounds.SoundsEntity;
import com.source.text.TextEntity;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity implements View.OnClickListener,
        SendMessageListener,HomeFragment.CallbackInHomeFragment,
        BindDeviceFragment.CallbackInBindDeviceFragment,BleService.CallbackInBle{

    @InjectView(R.id.img_menu_photo)
    ImageView imgPhoto;
    @InjectView(R.id.tv_menu_name)
    TextView tvName;
    @InjectView(R.id.tv_menu_create_group)
    TextView tvCreateGruop;
    @InjectView(R.id.tv_menu_add_group)
    TextView tvAddGroup;
    @InjectView(R.id.lockerview)
    LockerView lockerView;

    private final String TAG = getClass().getSimpleName();

    /**
     * the reference of BlueLeService
     */
    private BleService bluetoothLeService;
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
     * the bounded device's address
     */
    private String bindAddress;

    private SharedPreferences sp;

    /**
     * true if open application add have bounded device before
     */
    private boolean needConnectDevice = false;

    /**
     * callback when BlueLeService bind or unbind
     */
    private final ServiceConnection serviceConnectionBle = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BleService.LocalBinder) service).getService();
            bluetoothLeService.setCallback(MainActivity.this);
            if (!bluetoothLeService.initialize()) {
                showToast("Unable to initialize Bluetooth");
                finish();
            }else {
                if(!bluetoothLeService.isBluetoothEnable()) {
                    loadDialogFragment = LoadDialogFragment.getInstance("正在打开蓝牙");
                    loadDialogFragment.show(getFragmentManager(),null);
                    bluetoothLeService.openBluetooth();
                }
                if(needConnectDevice) {
                    bluetoothLeService.connect(bindAddress);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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
        setContentView(R.layout.activity_main);
        System.loadLibrary("speex");

        initView();
        initEvent();

        //open bluetooth and start bluetooth sever
        initDataExchangeHandler();
        iniInterphone();
        //bind the ble service
        bindService(new Intent(this, BleService.class), serviceConnectionBle, BIND_AUTO_CREATE);
    }

    private void initView() {

        ButterKnife.inject(this);
        sp = getSharedPreferences(Constants.SP_USER,0);
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

        bindAddress = sp.getString(Constants.BIND_DEVICE_ADDRESS,null);

        if(bindAddress == null) {
            //choose device to bind
            initBindDeiveFragment();
            changeFragment(bindDeviceFragment);
        }else {
            needConnectDevice = true;
            initHomeFragment();
            changeFragment(homeFragment);
        }
    }

    private void  initEvent(){
        tvAddGroup.setOnClickListener(this);
        tvCreateGruop.setOnClickListener(this);
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

    /**
     * crate group
     */
    private void createGroup() {
        Intent intent = new Intent(this,GroupActivity.class);
        intent.putExtra(GroupActivity.ACTION_MODE,0);
        startActivity(intent);
    }

    /**
     * add a group
     */
    private void addGroup() {
        Intent intent = new Intent(this,GroupActivity.class);
        intent.putExtra(GroupActivity.ACTION_MODE,1);
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

    private void changeChanel(byte channel) {
        if(!isReceiving && findWriteCharac) {
            Constants.CHANNEL[2] = channel;
            bluetoothLeService.writeInstruction(Constants.CHANNEL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bluetoothSate,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        bluetoothLeService.setCallback(null);
        bluetoothLeService.disconnect();
        bluetoothLeService.close();
        bluetoothLeService = null;

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(homeFragment != null) {
            int touchX = (int)ev.getX();
            int touchY = (int)ev.getY();
            if(homeFragment.checkTouch(touchX, touchY)) {
                return false;
            }else {
                return super.dispatchTouchEvent(ev);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_menu_add_group:
                addGroup();
                break;
            case R.id.tv_menu_create_group:
                createGroup();
                break;
        }
    }

    /**
     * callback in HomeFragment
     */
    @Override
    public void toggleMenu() {
        lockerView.toggleMenu();
    }

    /**
     * get channel state , if busy ,can't send message
     */
    private void chenckChannel() {
        if(bluetoothLeService != null){
            bluetoothLeService.writeInstruction(Constants.CHANNEL_STATE);
        }
    }

    /**
     * send text message to ble
     */
    private void sendText() {
        if(sendText != null) {
            homeFragment.sendText(sendText);
        }
        textEntity.unpacking(sendText);
    }

    private void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * call in SendMessageListener
     * @param data
     * @param end
     */
    @Override
    public void sendPacketedData (byte[] data,boolean end){
        if(data == null || bluetoothLeService == null) return;
        bluetoothLeService.write(data);
        data = null;
    }

    /**
     * callback in BleService
     * @param data
     * @param mode other message 0 indicate received correct data
     *             2 indicates send data failed
     */
    @Override
    public void sendUnPacketedData(byte[] data,int mode) {
        switch (mode) {
            case 0:
                //correct data
                parseFactory.sendToRelativeParser(data);
                break;
            case 2:
                //send failed
                showToast(getString(R.string.send_failed));
                break;
        }
    }

    /**
     * callback in BleService
     * @param connect true if ble connect,else false
     */
    @Override
    public void bleConnection(boolean connect) {
        if(bindDeviceFragment != null && bindDeviceFragment.isVisible()) {
            bindDeviceFragment.deviceConnected();
            initHomeFragment();
            changeFragment(homeFragment);
            saveBoundedDevice();
            bindDeviceFragment = null;
        }
        if(!connect) {
            if(homeFragment != null && homeFragment.isVisible()) {
                homeFragment.bleLose();
            }
        }
        deviceBinded = connect;
        needConnectDevice = !connect;
    }

    /**
     * save have bounded device
     */
    private void saveBoundedDevice() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Constants.BIND_DEVICE_ADDRESS,bindAddress);
        editor.putString(Constants.BIND_DEVICE_NAME,deviceName);
        editor.apply();
    }

    /**
     * callback in BleService
     * tell activity the characteristic have found and register
     */
    @Override
    public void writeCharacterFind() {
        findWriteCharac = true;
        if(homeFragment != null && homeFragment.isVisible()) {
            homeFragment.writeable = true;
            homeFragment.deviceConnected();
        }
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

    @Override
    public void resetCms() {
        resetCms(true);
    }

    /**
     * call in HomeFragment
     * reconnect device
     */
    @Override
    public void reconnectDevice() {
        if(bindAddress == null) {
            bindAddress = sp.getString(Constants.BIND_DEVICE_ADDRESS,"");
        }
        if(bluetoothLeService == null) return;
        bluetoothLeService.connect(bindAddress);
    }

    /**
     * call in BindDeviceFragment
     * @param device ble device
     */
    @Override
    public void connectDevice(BluetoothDevice device) {
        deviceName = device.getName();
        bindAddress = device.getAddress();
        if(bluetoothLeService != null) {
            bluetoothLeService.connect(bindAddress);
        }
    }
}
