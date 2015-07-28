package com.rftransceiver.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.rftransceiver.db.ListTest;
import com.baidu.mapapi.SDKInitializer;
import com.brige.blutooth.le.BleService;
import com.rftransceiver.R;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.customviews.LockerView;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.fragments.BindDeviceFragment;
import com.rftransceiver.fragments.ContactsFragment;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.fragments.LoadDialogFragment;
import com.rftransceiver.fragments.MyDeviceFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.DataPacketOptions;
import com.source.SendMessageListener;
import com.source.parse.ParseFactory;
import com.source.sounds.Audio_Reciver;
import com.source.sounds.Audio_Recorder;
import com.source.sounds.SoundsEntity;
import com.source.text.TextEntity;

import java.util.ArrayList;
import java.util.List;

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
    @InjectView(R.id.tv_menu_contacts)
    TextView tvContacts;

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
     * MyDeviceFragment to look,bind or unbind device
     */
    private MyDeviceFragment myDeviceFragment;

    /**
     * show contacts
     */
    private ContactsFragment contactsFragment;

    /**
     * the menu to mark send action
     */
    private SendAction action = SendAction.NONE;
    public enum SendAction {
        SOUNDS,
        Words,
        Address,
        Image,
        NONE
    }

    /**
     * cache all record sounds data
     */
    public static final List<byte[]> soundsRecords = new ArrayList<>();

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

//    /**
//     * cache the receive sounds data
//     */
//    private List<byte[]> receiSounds;
    /**
     * true if open application add have bounded device before
     */
    private boolean needConnectDevice = false;

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


        //测试中保存group信息-----------------------------
       // ListTest.saveGroups(this);

    }

    private void initView() {

        ButterKnife.inject(this);
        sp = getSharedPreferences(Constants.SP_USER,0);
        String name = sp.getString(Constants.NICKNAME,"");
        if(!TextUtils.isEmpty(name)) {
            tvName.setText(name);
        }
        name = null;
        final float dentisy = getResources().getDisplayMetrics().density;
        final String photoPath = sp.getString(Constants.PHOTO_PATH,"");
        if(!TextUtils.isEmpty(photoPath)) {
            PoolThreadUtil.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    int size = (int) (dentisy * 120 + 0.5f);
                    Bitmap bitmap = ImageUtil.createImageThumbnail(photoPath, size * size);
                    dataExchangeHandler.obtainMessage(Constants.GET_BITMAP, -1, -1, bitmap).sendToTarget();
                    bitmap = null;
                }
            });

        }
        bindAddress = sp.getString(Constants.BIND_DEVICE_ADDRESS,null);
        deviceName = sp.getString(Constants.BIND_DEVICE_NAME,null);

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
        tvContacts.setOnClickListener(this);
    }

    /**
     *
     * @param fragment needed display
     */
    private void changeFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_content, fragment);
        if(fragment instanceof MyDeviceFragment || fragment instanceof ContactsFragment) {
            transaction.addToBackStack(null);
        }
        transaction.commitAllowingStateLoss();
        transaction = null;
    }

    private void iniInterphone() {
        //initial the receiver
        receiver = Audio_Reciver.getInstance();
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

    private void initContacsFragment() {
        if(contactsFragment == null) {
            contactsFragment = new ContactsFragment();
            contactsFragment.setCallback(new ContactsFragment.CallbackInContacts() {
                @Override
                public void changeGroup(int gid) {
                    getFragmentManager().popBackStackImmediate();
                    contactsFragment = null;
                    if(homeFragment != null) {
                        homeFragment.changeGroup(gid);
                    }
                }

                @Override
                public void openScorll(boolean open) {
                    lockerView.openScroll(open);
                }
            });
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

    private void initMyDeviceFragment() {
        if(myDeviceFragment == null) {
            myDeviceFragment = new MyDeviceFragment();
        }
        myDeviceFragment.setCallback(new MyDeviceFragment.CallbackInMyDevice() {
            @Override
            public void bindDevice() {
                //call to bind device
                getFragmentManager().popBackStackImmediate();
                initBindDeiveFragment();
                changeFragment(bindDeviceFragment);
            }

            @Override
            public void unbindDevice() {
                //call to unbind device
                if(bluetoothLeService != null && deviceBinded) {
                    bluetoothLeService.disconnect();
                }
                saveBoundedDevice("","");
                deviceBinded = false;
                bindAddress = null;
                deviceName = null;
            }

            /**
             * @param open decide to open or close scroll for lockview
             */
            @Override
            public void openScrollLockView(boolean open) {
                lockerView.openScroll(open);
            }
        });
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
                                        if(homeFragment != null){
                                            //是否实时接收语音
                                            if(homeFragment.getRealTimePlay()){
                                                receiver.startReceiver();
                                            }else {
                                                receiver.stopReceiver();
                                            }
                                        }
                                        if(homeFragment != null) {
                                            homeFragment.receivingData(0,null,(int)msg.obj);
                                        }
                                        soundsRecords.clear();
                                        break;
                                    case 1:
                                        //end to receive sounds data
                                        stopReceiveSounds();
                                        byte[] receSounds = new byte[soundsRecords.size() * Constants.Small_Sounds_Packet_Length];
                                        int index = 0;
                                        for(byte[] s : soundsRecords) {
                                            System.arraycopy(s,0,receSounds,index,s.length);
                                            index += s.length;
                                        }
                                        soundsRecords.clear();
                                        String rcvSounds = Base64.encodeToString(receSounds,Base64.DEFAULT);
                                        receSounds = null;
                                        if(homeFragment != null) {
                                            homeFragment.endReceiveSounds(rcvSounds, (int) (msg.obj));
                                        }
                                        break;
                                    case 2:
                                        //cache the received sounds data
                                        byte[] sound = (byte[]) msg.obj;
                                        soundsRecords.add(sound);
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
                                String address = null;
                                if(add != null) {
                                    address = new String(add);
                                }
                                if(address != null) {
                                    String[] addrs = address.split("\\|");
                                    if(addrs.length == 2 && homeFragment != null) {
                                        homeFragment.receivingData(2,new String(add),msg.arg2);
                                    }
                                    addrs = null;
                                    address = null;
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
                                        record.startRecording();
                                        if(homeFragment != null) {
                                            homeFragment.startSendingSounds();
                                        }
                                    } else if (action == SendAction.Words) {
                                        sendText(DataPacketOptions.TextType.Words);
                                    }else if(action == SendAction.Address) {
                                        sendText(DataPacketOptions.TextType.Address);
                                    }else if(action == SendAction.Image) {
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
                    case Constants.GET_BITMAP:
                        Bitmap bitmap = (Bitmap) msg.obj;
                        if(bitmap != null) {
                            imgPhoto.setImageDrawable(new CircleImageDrawable(
                                    bitmap));
                        }
                        break;
                }
            }
        };
    }

    private void stopReceiveSounds() {
        if(receiver.isReceiving()) {
            receiver.stopReceiver();
        }
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
        startActivityForResult(intent, REQUEST_GROUP);
    }

    private void sendAsyncWord() {
        if(bluetoothLeService == null || asyncWord == null) return;
        if(!haveSetAsyncWord) {
            bluetoothLeService.writeInstruction(asyncWord);
        }
    }

    //reset the cms
    private void resetCms(boolean write) {
        receiver.stopReceiver();
        record.stopRecording();
        receiver.clear();
        parseFactory.resetSounds();
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

        record.stopRecording();
        receiver.stopReceiver();
        textEntity.setSendListener(null);
        textEntity.setOptions(null);

        unbindService(serviceConnectionBle);
        bluetoothLeService.setCallback(null);
        bluetoothLeService.disconnect();
        bluetoothLeService.close();

        DBManager.close();

        bindDeviceFragment = null;
        homeFragment = null;
        loadDialogFragment = null;
        myDeviceFragment = null;
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
                initMyDeviceFragment();
                changeFragment(myDeviceFragment);
                lockerView.closeMenu();
                break;
            case R.id.tv_menu_interphone:
                showInterphone();
                break;
            case R.id.tv_menu_setting:
                startActivityForResult(new Intent(MainActivity.this,
                        SettingActivity.class), REQUEST_CHANGECHANNEL);
                break;
            case R.id.tv_menu_exit:
                MainActivity.this.finish();
                break;
            case R.id.tv_menu_contacts:
                initContacsFragment();
                changeFragment(contactsFragment);
                lockerView.closeMenu();
                break;
        }
    }

    private void showInterphone() {
        if(deviceName == null || bindAddress == null) {
            new AlertDialog.Builder(this).setMessage("绑定设备后才能进入对讲机开始聊天！")
                    .setPositiveButton(R.string.sure, null)
                    .show();
            return;
        }
        lockerView.toggleMenu();
        if(homeFragment != null && homeFragment.isVisible())
            return;
        initHomeFragment();
        changeFragment(homeFragment);
    }

    /**
     * callback in HomeFragment
     */
    @Override
    public void toggleMenu() {
        lockerView.toggleMenu();
    }

    /**
     * callback in HomeFragment
     */
    @Override
    public void openScroll(boolean open) {
        lockerView.openScroll(open);
    }

    /**
     * get channel state , if busy ,can't send message
     */
    private void chenckChannel() {
        if(bluetoothLeService != null){
            if( !bluetoothLeService.writeInstruction(Constants.CHANNEL_STATE)) {
                action = SendAction.NONE;
            }
        }else {
            action = SendAction.NONE;
        }
    }

    /**
     * send text message to ble
     */
    private void sendText(DataPacketOptions.TextType type) {
        if(sendText != null) {
            textEntity.unpacking(sendText, type);
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
     *
     * @param data the data to be sent
     * @param end send all the data or not
     * @param percent the sent data's percent of whole data
     *                or the length of send data
     */
    private boolean notifySendImage = false;
    @Override
    public void sendPacketedData(final byte[] data, final boolean end, final int percent) {
        if(data == null || bluetoothLeService == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                data[Constants.Group_Member_Id_index] = (byte)myId;
                bluetoothLeService.write(data);
                switch (action) {
                    case Address:
                    case Words:
                        if(end) {
                            if(homeFragment != null && homeFragment.isVisible()) {
                                homeFragment.sendMessage(sendText, action);
                            }
                            action = SendAction.NONE;
                        }
                        break;
                    case Image:
                        if(homeFragment == null) return;
                        if(!notifySendImage) {
                            homeFragment.sendMessage(sendText,action);
                            notifySendImage = true;
                        }else {
                            homeFragment.upteImageProgress(percent);
                        }
                        if(end) {
                            action = SendAction.NONE;
                            notifySendImage = false;
                        }
                        break;
                    case SOUNDS:
                        if(end) {
                            action = SendAction.NONE;
                            byte[] sendSounds = new byte[MainActivity.soundsRecords.size() * Constants.Small_Sounds_Packet_Length];
                            int index = 0;
                            for(byte[] ss : soundsRecords) {
                                System.arraycopy(ss,0,sendSounds,index,ss.length);
                                index += ss.length;
                            }
                            String sounds = Base64.encodeToString(sendSounds,Base64.DEFAULT);
                            if(homeFragment != null && homeFragment.isVisible()) {
                                homeFragment.sendMessage(sounds,SendAction.SOUNDS);
                            }
                            soundsRecords.clear();
                        }
                        break;
                }
            }
        });
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
            bindDeviceFragment = null;
        }
        if(homeFragment != null) {
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
    private void saveBoundedDevice(String name,String address) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Constants.BIND_DEVICE_ADDRESS,address);
        editor.putString(Constants.BIND_DEVICE_NAME,name);
        editor.apply();
    }

    /**
     * callback in BleService
     * tell activity the characteristic have found and register
     */
    @Override
    public void deviceNotWork() {
        if(homeFragment != null) {
            homeFragment.deviceConnected(false);
        }
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
        if(action != SendAction.NONE) {
            showToast("上一条消息还在发送，请等待！");
            return;
        }
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

    @Override
    public void setMyId(int tempId) {
        this.myId = tempId;
    }

    /**
     * call in HomeFragment
     * reconnect device
     */
    @Override
    public void reconnectDevice() {
        if(bindAddress == null) {
            showToast("还未绑定设备！");
            return;
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
        saveBoundedDevice(deviceName, bindAddress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_GROUP && resultCode == Activity.RESULT_OK && data != null) {
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
            int channel = data.getIntExtra(Constants.SELECTED_CHANNEL, -1);
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
