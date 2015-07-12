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
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.brige.blutooth.le.BleService;
import com.rftransceiver.R;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.customviews.LockerView;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.fragments.BindDeviceFragment;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.fragments.LoadDialogFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;
import com.rftransceiver.util.PoolThreadUtil;
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
        BindDeviceFragment.CallbackInBindDeviceFragment,BleService.CallbackInBle
{

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
    @InjectView(R.id.tv_menu_my_device)
    TextView tvMyDevice;
    @InjectView(R.id.tv_menu_interphone)
    TextView tvInterPhone;
    @InjectView(R.id.tv_menu_setting)
    TextView tvSetting;
    @InjectView(R.id.tv_menu_exit)
    TextView tvExit;

    private final String TAG = getClass().getSimpleName();

    /**
     * the reference of BlueLeService
     */
    private BleService bluetoothLeService;

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
        Words,
        SOUNDS,
        Address,
        Image,
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

    /***
     * group's asyncWord
     */
    private byte[] asyncWord;

    private boolean haveSetAsyncWord = false;

    /**
     * my id in group
     */
    private int myId;

    /**
     * true if open application add have bounded device before
     */
    private boolean needConnectDevice = false;

    public static final int REQUEST_MYDEVICE = 300;
    public static final int REQUEST_GROUP = 301;
    public static final int REQUEST_CHANGECHANNEL = 305;

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
                    if(!bluetoothLeService.connect(bindAddress)) {
                        if(homeFragment != null) {
                            homeFragment.deviceConnected(false);
                        }
                    }
                    needConnectDevice = false;
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

    public static int CURRENT_CHANNEL = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            SDKInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        if(TextUtils.isEmpty(bindAddress)) {
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
        tvMyDevice.setOnClickListener(this);
        tvInterPhone.setOnClickListener(this);
        tvSetting.setOnClickListener(this);
        tvExit.setOnClickListener(this);
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
                4);
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
                            case Constants.READ_SOUNDS:
                                switch (msg.arg2) {
                                    case 0:
                                        //start to receive sounds data
                                        receiver.startReceiver();
                                        if(homeFragment != null) {
                                            homeFragment.receivingData(0,null,(int)msg.obj);
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
                            case Constants.READ_WORDS:
                                //receive text data
                                byte[] data = (byte[]) msg.obj;
                                if(data == null) return;
                                if(homeFragment != null) {
                                    homeFragment.receivingData(1,new String(data),msg.arg2);
                                }
                                break;
                            case Constants.READ_ADDRESS:
                                byte[] add = (byte[]) msg.obj;
                                if(add != null) {
                                    if(homeFragment != null) {
                                        homeFragment.receivingData(2,new String(add),msg.arg2);
                                    }
                                }
                                break;
                            case Constants.READ_Image:
                                byte[] image = (byte[]) msg.obj;
                                if(image != null) {
                                    if(homeFragment != null) {
                                        homeFragment.receivingData(3,new String(image),msg.arg2);
                                    }
                                }
                                break;
                            case Constants.READ_CHANGE_CHANNEL:
                                int channel = msg.arg2;
                                String[] channels = getResources().getStringArray(R.array.channel);
                                showToast("信道已修改为"+channels[channel]);
                                channels = null;
                                CURRENT_CHANNEL = channel;
                                break;
                            case Constants.READ_SETASYNCWORD:
                                showToast("设置同步字成功");
                                haveSetAsyncWord = true;
                                break;
                            case Constants.READ_RSSI:
                                showToast("读到rssi值是：" + msg.arg2);
                                break;
                            case Constants.READ_CHANNEL:
                                if (msg.arg2 == 0) {
                                    stopReceiveSounds();
                                    if (action == SendAction.SOUNDS) {
                                        //start record
                                        action = SendAction.NONE;
                                        record.startRecording();
                                        if(homeFragment != null) {
                                            homeFragment.startSendingSounds();
                                        }
                                    } else if (action == SendAction.Words) {
                                        action = SendAction.NONE;
                                        sendText(DataPacketOptions.TextType.Words);
                                    }else if(action == SendAction.Address) {
                                        action = SendAction.NONE;
                                        sendText(DataPacketOptions.TextType.Address);
                                    }else if(action == SendAction.Image) {
                                        action = SendAction.NONE;
                                        sendText(DataPacketOptions.TextType.Image);
                                    }
                                } else if (msg.arg2 == 1) {
                                    if (action != SendAction.NONE) {
                                        showToast("信道占用中，不能发送");
                                    }
                                }
                                break;
                            case Constants.READ_ERROR:
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
     * create or add a group
     * @param mode if is 0,create group
     *             if is 1 add a group
     */
    private void groupAction(int mode) {
        Intent intent = new Intent(this,GroupActivity.class);
        intent.putExtra(GroupActivity.ACTION_MODE,mode);
        startActivityForResult(intent,REQUEST_GROUP);
    }

    private void sendAsyncWord() {
        if(bluetoothLeService == null || asyncWord == null) return;
        if(!haveSetAsyncWord) {
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
        if(write) {
            if(bluetoothLeService != null) {
                bluetoothLeService.writeInstruction(Constants.RESET);
            }
        }
    }

    private void changeChanel(byte channel) {
        if(bluetoothLeService == null) return;
        Constants.CHANNEL[2] = channel;
        bluetoothLeService.writeInstruction(Constants.CHANNEL);
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
        PoolThreadUtil.getInstance().close();
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

        DBManager.close();

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
                groupAction(1);
                break;
            case R.id.tv_menu_create_group:
                groupAction(0);
                break;
            case R.id.tv_menu_my_device:
                startActivityForResult(new Intent(MainActivity.this,
                        MyDeviceActivity.class),REQUEST_MYDEVICE);
                break;
            case R.id.tv_menu_interphone:
                if(homeFragment != null && homeFragment.isVisible()) return;
                initHomeFragment();
                changeFragment(homeFragment);
                lockerView.toggleMenu();
                break;
            case R.id.tv_menu_setting:
                startActivityForResult(new Intent(MainActivity.this,
                        SettingActivity.class), REQUEST_CHANGECHANNEL);
                break;
            case R.id.tv_menu_exit:
                MainActivity.this.finish();
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
     * call back in HomeFragment
     * @param b
     */
    @Override
    public void setMenuScroll(boolean b) {
        lockerView.openScroll(b);
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
    private void sendText(DataPacketOptions.TextType type) {
        if(sendText != null) {
            textEntity.unpacking(sendText,type);
            homeFragment.sendText(sendText,type);
        }
    }

    private void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
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
        data[Constants.Group_Member_Id_index] = (byte)myId;
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
        if(bindDeviceFragment != null) {
            bindDeviceFragment.deviceConnected();
            initHomeFragment();
            changeFragment(homeFragment);
            saveBoundedDevice();
            bindDeviceFragment = null;
        }
        if(homeFragment != null && homeFragment.isVisible()) {
            //homeFragment.bleLose();
            homeFragment.deviceConnected(connect);
        }
        if(connect && !haveSetAsyncWord) {
            sendAsyncWord();
        }
        deviceBinded = connect;
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
    public void deviceNotWork() {
        showToast("请确保设备正在工作");
    }

    /**
     * callback in BleService
    */
    @Override
    public void serviceNotInit() {
        showToast("连接未初始化，请稍候...");
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
        if(bluetoothLeService == null || deviceBinded) return;
        if(!bluetoothLeService.connect(bindAddress)) {
            if(homeFragment != null) {
                homeFragment.deviceConnected(false);
            }
        }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_MYDEVICE && resultCode ==Activity.RESULT_OK && data != null) {
            /**
             * in MyDeviceActivity,need to rebind or unbind device
             */
            switch (data.getStringExtra(MyDeviceActivity.EXTRA_INMYDEVICE)) {
                case MyDeviceActivity.BINDDEVICE:
                    initBindDeiveFragment();
                    changeFragment(bindDeviceFragment);
                    lockerView.closeMenu();
                    break;
                case MyDeviceActivity.UNBINDDEVICE:
                    if(bluetoothLeService != null && deviceBinded) {
                        bluetoothLeService.disconnect();
                        deviceBinded = false;
                    }
                    break;
            }
        }else if(requestCode == REQUEST_GROUP && resultCode == Activity.RESULT_OK && data != null) {
            /**
             * in GroupActivity,after finish create or add group,send a GroupEntity
             */
            Bundle bundle = data.getExtras();
            if(bundle == null) return;
            GroupEntity groupEntity = bundle.getParcelable(GroupActivity.EXTRA_GROUP);
            asyncWord = groupEntity.getAsyncWord();
            myId = groupEntity.getTempId();
            if(groupEntity == null) return;
            if(homeFragment == null) {
                initHomeFragment();
                changeFragment(homeFragment);
            }
            homeFragment.updateGroup(groupEntity);
            lockerView.closeMenu();
            sendAsyncWord();
            //GroupUtil.recycle(groupEntity.getMembers());
        }else if(requestCode == HomeFragment.REQUEST_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            String address = data.getStringExtra(HomeFragment.EXTRA_LOCATION);
            if(!TextUtils.isEmpty(address))
                send(SendAction.Address,address);
        }else if(requestCode == REQUEST_CHANGECHANNEL && resultCode == Activity.RESULT_OK
                && data != null) {
            int channel = data.getIntExtra(Constants.SELECTED_CHANNEL,-1);
            if(channel == -1) return;
            changeChanel((byte)channel);
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if(lockerView.isMenuOpened()) {
            lockerView.closeMenu();
        }else {
            super.onBackPressed();
        }
    }

}
