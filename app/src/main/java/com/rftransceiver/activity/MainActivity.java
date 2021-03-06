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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
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

import com.baidu.mapapi.SDKInitializer;
import com.brige.blutooth.le.BleService;
import com.rftransceiver.R;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.customviews.LockerView;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.fragments.BindDeviceFragment;
import com.rftransceiver.fragments.ContactsFragment;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.fragments.MyDeviceFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.MessageCacheUtil;
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

    //消息缓存
    private MessageCacheUtil cacheUtil;
    private Bitmap back;
    //接受的消息时长的起始时间
    private long preTime;
    //接受的消息时长的终止时间
    private long curTime;
    //管理蓝牙的服务
    private BleService bleService;
    //接收异步线程的消息
    private static Handler dataExchangeHandler;
    //将文本类信息按数据包协议封包
    private TextEntity textEntity;
    //管理录音的开始和结束
    private Audio_Recorder record;
    //用来管理开启和关闭语音的实时播放
    private Audio_Reciver receiver;
    //初步解析接收到的数据，并进一步将数据发送到最终要解析的地方
    private  ParseFactory parseFactory;
    // 暂存要发送的信息
    private String sendText;
    //绑定设备
    private BindDeviceFragment bindDeviceFragment;
    //对讲机
    private HomeFragment homeFragment;
    //我的设备
    private MyDeviceFragment myDeviceFragment;
    //通讯录
    private ContactsFragment contactsFragment;
    //保存新建的组的引用
    private GroupEntity groupEntity;
    //标识有没有新建的组
    private boolean haveNewGroup = false;

    //用来区分发送信息的动作
    private SendAction action = SendAction.NONE;
    public enum SendAction {
        SOUNDS,
        Words,
        Address,
        Image,
        NONE
    }

    //缓存录音信息，用于保存
    public static final List<byte[]> soundsRecords = new ArrayList<>();
    //已绑定设备的名称
    private String deviceName;
    //已绑定设备的地址
    private String bindAddress;
    //保存一些配置信息
    private SharedPreferences sp;
    //解除绑定的标识
    private boolean unBind = false;
    //我在组里的id
    private int myId;
    public static final int REQUEST_GROUP = 301;    //获取组信息的请求代码，用于GroupActivity
    public static final int REQUEST_SETTING = 306; //设置的请求代码
    //重连设备的标识
    private boolean reconnected = false;
    //区分请求绑定设备的来源
    private BineDeviceFrom bindFrom;
    private enum BineDeviceFrom {
        HF, //请求来自与HomeFragment
        BF //请求来自与BindDeviceFragment
    }


    /**
     * 当BleService绑定成功或者绑定失败的时候回调
     */
    private final ServiceConnection serviceConnectionBle = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            //为BleService添加回调函数
            bleService.setCallback(MainActivity.this);
            if (!bleService.initialize()) {
                showToast("Unable to initialize Bluetooth");
                finish();
            }
        }

        //解绑BleSevice
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            PoolThreadUtil.getInstance().close();
            bleService = null;
        }
    };

    //接收蓝牙状态的广播接收器
    private final BroadcastReceiver bluetoothSate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(bleService != null && unBind) {
                            //重新开启蓝牙
                            bleService.openBluetooth();
                        }else if(homeFragment != null) {
                            if(bleService != null) {
                                bleService.disconnect();
                                bleService.close();
                            }
                            homeFragment.deviceConnected(false);
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //通知蓝牙已开启
                        if(homeFragment != null) {
                            if(reconnected) {
                                reconnected = false;
                                reconnectDevice();
                            }else {
                                homeFragment.bleOpend();
                            }
                        }
                        if(bindDeviceFragment != null) {
                            bindDeviceFragment.bleOpend();
                        }
                        if(unBind && myDeviceFragment != null) {
                            unBind = false;
                            myDeviceFragment.unBindOk();
                        }
                        break;
                }
            }
        }
    };

    public static int CURRENT_CHANNEL = 0;

    //自己的头像
    private Bitmap bitmapHead;
    //自己的昵称
    private String name;
    //用于在onStart中标识有没有进行初始化操作
    private boolean init = false;

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
        initDataExchangeHandler();
        initView();
        initEvent();
        cacheUtil=MessageCacheUtil.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!init) {
            init = true;
            //进行录音和发送信息的初始化工作
            iniInterphone();
            //绑定蓝牙服务
            bindService(new Intent(this, BleService.class), serviceConnectionBle, BIND_AUTO_CREATE);
            //设置媒体音量最大
            maxVolume();
            registerReceiver(bluetoothSate,
                    new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }
    }

    /**
     * 开启录音，并在100毫秒后停止，
     * 目的是提前获取录音权限，避免在录音时弹出获取权限的请求
     */
    private void maxVolume() {
        //设置媒体音量最大
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
    }

    //初始化视图
    private void initView() {

        ButterKnife.inject(this);
        lockerView.setBackground(new BitmapDrawable(back));
        sp = getSharedPreferences(Constants.SP_USER,0);
        //获取用户名
        name = sp.getString(Constants.NICKNAME,"");
        if(!TextUtils.isEmpty(name)) {
            tvName.setText(name);
        }
        final float dentisy = getResources().getDisplayMetrics().density;
        final String photoPath = sp.getString(Constants.PHOTO_PATH,"");
        //获取用户头像,和背景图片
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                //加载背景图片
                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inSampleSize = 4;
                Bitmap bmBack = BitmapFactory.decodeResource(getResources(), R.drawable.lanchuner_bg, op);
                dataExchangeHandler.obtainMessage(Constants.GET_BITMAP, 0, -1, bmBack).sendToTarget();
                //加载用户头像
                if (!TextUtils.isEmpty(photoPath)) {
                    int size = (int) (dentisy * 120 + 0.5f);
                    Bitmap bitmap = ImageUtil.createImageThumbnail(photoPath, size * size);
                    dataExchangeHandler.obtainMessage(Constants.GET_BITMAP, 1, -1, bitmap).sendToTarget();
                }
            }
        });
        //获取已绑定的设备的地址和名称
        bindAddress = sp.getString(Constants.BIND_DEVICE_ADDRESS,null);
        deviceName = sp.getString(Constants.BIND_DEVICE_NAME,null);

        if(TextUtils.isEmpty(bindAddress)) {
            //还未绑定过设备，
            initBindDeiveFragment();
            changeFragment(bindDeviceFragment);
            bindFrom = BineDeviceFrom.BF;
        }else {
            initHomeFragment();
            homeFragment.setNeedConnecAuto(true);
            changeFragment(homeFragment);
            bindFrom = BineDeviceFrom.HF;
        }
    }

    private void  initEvent(){
        imgPhoto.setOnClickListener(this);
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
        if(fragment instanceof MyDeviceFragment || fragment instanceof ContactsFragment
                || (fragment instanceof BindDeviceFragment && homeFragment != null)) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
        transaction = null;
    }

    /**
     * 发送数据前的初始化工作
     */
    private void iniInterphone() {
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
     * 初始化HomeFragment,并设置回调函数
     */
    private void initHomeFragment() {
        if(homeFragment == null) {
            homeFragment = new HomeFragment();
            homeFragment.setCallback(this);
        }
    }

    /**
     * 实例化ContactsFragment,添加回调函数
     */
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
     * 自动连接设备
     */
    public void connectDeviceAuto() {
        if(TextUtils.isEmpty(bindAddress)) return;
        bindFrom = BineDeviceFrom.HF;
        bleService.connect(bindAddress,true);
    }

    /**
     * 初始化BindDeviceFragment,避免忘记设置回调接口
     */
    private void initBindDeiveFragment() {
        if(bindDeviceFragment == null) {
            bindDeviceFragment = new BindDeviceFragment();
            bindDeviceFragment.setCallback(this);
        }
    }

    /**
     * 实例化MyDeviceFragment对象，实例化后为其设置回调接口
     */
    private void initMyDeviceFragment() {
        if(myDeviceFragment == null) {
            myDeviceFragment = new MyDeviceFragment();
        }
        //设置回调接口
        myDeviceFragment.setCallback(new MyDeviceFragment.CallbackInMyDevice() {
            @Override
            public void bindDevice() {
                //绑定设备
                if(homeFragment != null) {
                    homeFragment.setNeedConnecAuto(false);
                }
                getFragmentManager().popBackStackImmediate();
                initBindDeiveFragment();
                changeFragment(bindDeviceFragment);
            }

            @Override
            public void unbindDevice() {
                bindAddress = null;
                deviceName = null;
                //解除绑定
                unBind = true;
                if(bleService != null) {
                    bleService.unBindDevice();
                }
                //清空SharedPreference中保存的历史设备
                updateBoundedDevice("", "");
            }

            /**
             * @param open 决定是否开启LockerView的右滑功能
             */
            @Override
            public void openScrollLockView(boolean open) {
                lockerView.openScroll(open);
            }
        });
    }

    private void initDataExchangeHandler() {
        dataExchangeHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.MESSAGE_READ:
                        switch (msg.arg1) {
                            case Constants.READ_SOUNDS:
                                switch (msg.arg2) {
                                    case 0:
                                        stopSendSounds();
                                        //preTime为接受消息的起始时间，用于计算消息时长
                                        preTime=System.currentTimeMillis();
                                        Log.i("---------preTime------",preTime+"");
                                        if(homeFragment != null){
                                            //是否实时接收语音
                                            if(homeFragment.getRealTimePlay()){
                                                if(!receiver.isReceiving()) {
                                                    receiver.startReceiver();
                                                }
                                            }else {
                                                receiver.stopReceiver();
                                                stopReceiveSounds();
                                            }
                                            homeFragment.receivingData(0,null,(int)msg.obj,0);
                                        }
                                        soundsRecords.clear();
                                        break;
                                    case 1:
                                        //end to receive sounds data
                                        stopReceiveSounds();
                                        //curTime为接受消息的终止时间，用于计算消息时长
                                        curTime=System.currentTimeMillis();
                                        Log.i("--------curTime-------",curTime+"");
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
                                            homeFragment.endReceiveSounds(rcvSounds, (int) (msg.obj),curTime-preTime);
                                            Log.i("----------time---------", curTime-preTime + "");
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
                                if(data == null) return false;
                                if(homeFragment != null) {
                                    homeFragment.receivingData(1,new String(data),msg.arg2,0);
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
                                        homeFragment.receivingData(2,new String(add),msg.arg2,0);
                                    }
                                    addrs = null;
                                    address = null;
                                }
                                break;
                            case Constants.READ_Image:
                                byte[] image = (byte[]) msg.obj;
                                if(image != null) {
                                    if(homeFragment != null) {
                                        homeFragment.receivingData(3,new String(image),msg.arg2,0);
                                    }
                                }
                                break;
                            case Constants.READ_CHANGE_CHANNEL:
                                int channel = msg.arg2;
                                //showToast("信道已修改为"+channel);
                                if(homeFragment != null) {
                                    homeFragment.channelHaveChanged();
                                }
                                CURRENT_CHANNEL = channel;
                                break;
                            case Constants.READ_SETASYNCWORD:
                                if(homeFragment != null) {
                                    homeFragment.asyncOk();
                                }
                                break;
                            case Constants.READ_UNKNOWN:
                                stopReceiveSounds();
                                resetCms(true);
                                break;
                            case Constants.READ_RSSI:
                                showToast("读到rssi值是：" + msg.arg2);
                                break;
                            case Constants.READ_CHANNEL:
                                if (msg.arg2 == 0) {
//                                    if(receiver.isReceiving()) {
//                                        stopReceiveSounds();
//                                    }
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
                        switch (msg.arg1) {
                            case 0:
                                //得到背景图片
                                back = (Bitmap) msg.obj;
                                lockerView.setBackground(new BitmapDrawable(back));
                                break;
                            case 1:
                                // 得到用户头像
                                bitmapHead = (Bitmap) msg.obj;
                                if(bitmapHead  != null) {
                                    Drawable drawable = new CircleImageDrawable(bitmapHead);
                                    imgPhoto.setImageDrawable(drawable);
                                }
                                break;
                        }
                        break;
                    case Constants.HANDLE_SEND:
                        //处理发送数据后的相关事宜
                        boolean end = msg.arg1 == 1;
                        int length = msg.arg2;
                        byte[] data = (byte[]) msg.obj;
                        afterSend(data,end,length);
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 处理发送后的相关事宜，必修要在主线程执行的操作
     */
    private void afterSend(byte[] data,boolean end,int percent) {
        data[Constants.Group_Member_Id_index] = (byte)myId;
        bleService.write(data);
        switch (action) {
            case Address:
            case Words:
                if(end) {
                    if(homeFragment != null && homeFragment.isVisible()) {
                        homeFragment.statesFinished();
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
                    if(homeFragment != null) {
                        homeFragment.sendMessage(sounds,SendAction.SOUNDS);
                    }
                    soundsRecords.clear();
                }
                break;
        }
    }

    /**
     * 发送失败
     */
    private void sendFail(){
        homeFragment.statesFail();
    }

    /**
     * 停止播放语音
     */
    private void stopReceiveSounds() {
        //receiver.stopReceiver();
        if(homeFragment != null) {
            homeFragment.endReceive(0);
        }
    }

    /**
     * 创建或添加一个组
     * @param mode if is 0,建组
     *             if is 1 加组
     */
    private void groupAction(int mode) {
        Intent intent = new Intent(this,GroupActivity.class);
        intent.putExtra(GroupActivity.ACTION_MODE, mode);
        startActivityForResult(intent, REQUEST_GROUP);
    }

    /**
     * 向设备发送同步字,HomeFragment中的回调方法
     */
    @Override
    public void sendAsyncWord(byte[] asyncWord) {
        if(bleService == null || asyncWord == null) return;
        bleService.writeInstruction(asyncWord);
        Log.e("sendAsyncWord","设置同步字");
    }

    //复位硬件设备
    private void resetCms(boolean write) {
        stopReceiveSounds();
        stopSendSounds();
        receiver.clear();
        parseFactory.resetSounds();
        textEntity.close();
        if(homeFragment != null) {
            homeFragment.endReceive(0);
        }
        if(write) {
            if(bleService != null) {
                bleService.writeInstruction(Constants.RESET);
            }
        }
    }

    /**
     * 更改信道
     * @param channel
     */
    private void changeChanel(byte channel) {
        if(bleService == null) return;
        Constants.CHANNEL[2] = channel;
        bleService.writeInstruction(Constants.CHANNEL);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bluetoothSate);
        super.onDestroy();
        record.stopRecording();
        receiver.stopReceiver();
        textEntity.close();
        unbindService(serviceConnectionBle);
        PoolThreadUtil.getInstance().close();
        textEntity.setSendListener(null);
        textEntity.setOptions(null);
        bleService.setCallback(null);
        bleService.disconnect();
        bleService.close();

        DBManager.close();

        bindDeviceFragment = null;
        homeFragment = null;
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
            case R.id.img_menu_photo:
                showPersonal();
                break;
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
                        SettingActivity.class), REQUEST_SETTING);
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

    /**
     * 展示个人中心的方法
     */
    private void showPersonal() {
        Intent intent = new Intent();
        intent.setClass(this, PersonalActivity.class);
        intent.putExtra("name", name);
        if(bitmapHead != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("bitmap",bitmapHead);
            intent.putExtra("bitmap",bundle);
        }
        startActivity(intent);
    }

    /**
     * 展示HomeFragment
     */
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
     * HomeFragment的回调方法,复位单片机
     */
    @Override
    public void resetFromH() {
        resetCms(true);
    }

    /**
     * HomeFragment中的回调方法
     * @param tempId
     */
    @Override
    public void setMyId(int tempId) {
        this.myId = tempId;
    }

    /**
     * 在HomeFragment中回调，打开或关闭左侧菜单
     */
    @Override
    public void toggleMenu() {
        lockerView.toggleMenu();
    }

    /**
     * HomeFragment中回调，打开或关闭lockerView的右拉功能
     */
    @Override
    public void openScroll(boolean open) {
        lockerView.openScroll(open);
    }

    /**
     * HomeFragment中的回调，检查蓝牙是否已经开启
     * @return 0 蓝牙已开启
     *         1 蓝牙未开启
     *         2 服务还未绑定成功
     */
    @Override
    public int isBleOpen() {
        if(bleService == null) return 2;
        if(bleService.isBluetoothEnable()) {
            return 0;
        }
        return 1;
    }

    /**
     * 检查信道状态
     */
    private void chenckChannel() {
        if(bleService != null){
            if( !bleService.writeInstruction(Constants.CHANNEL_STATE)) {
                action = SendAction.NONE;
            }
        }else {
            action = SendAction.NONE;
        }
    }

    /**
     * 将文本类信息打包后发送给ble
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
     * @param data 要发送的数据
     * @param end 标识是否发送完毕
     * @param percent 发送图片的百分比，或是发送数据的长度
     */
    private boolean notifySendImage = false;    //标识是否通知HomeFragment图片开始发送
    @Override
    public void sendPacketedData(final byte[] data, final boolean end, final int percent) {
        if (data == null || bleService == null) return;
        int arg1 = end ? 1 : 0;
        dataExchangeHandler.obtainMessage(Constants.HANDLE_SEND, arg1, percent, data).sendToTarget();
    }

    /**
     *  BleService回调
     * @param data  接收到的数据
     * @param mode  0 表明接收到了正确的数据
     *              2 表明接收到了错误的数据
     */
        @Override
    public void sendUnPacketedData(byte[] data,int mode) {
        switch (mode) {
            case 0:
                parseFactory.sendToRelativeParser(data);
                break;
            case 2:
                showToast(getString(R.string.send_failed));
                break;
        }
    }

    /**
     * BleService中回调，通知ble连接成功或失败
     * @param connect true if ble connect,else false
     */
    @Override
    public void bleConnection(boolean connect) {
        if(bindFrom == BineDeviceFrom.BF && bindDeviceFragment != null) {
            if(bindDeviceFragment.getRequestConnection()) {
                bindDeviceFragment.deviceConnected();
                bindDeviceFragment = null;
                initHomeFragment();
                changeFragment(homeFragment);
            }
        }
        if(homeFragment != null) {
            homeFragment.deviceConnected(connect);
        }
    }

    /**
     * 更新绑定的设备信息
     * @param name 绑定的设备的名称
     * @param address 绑定的设备的ssid
     */
    private void updateBoundedDevice(String name, String address) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Constants.BIND_DEVICE_ADDRESS,address);
        editor.putString(Constants.BIND_DEVICE_NAME,name);
        editor.apply();
    }

    /**
     * 在BleService中检测到设备没有工作
     */
    @Override
    public void deviceNotWork() {
        if(homeFragment != null) {
            homeFragment.deviceConnected(false);
            sendFail();
        }
        showToast("未检测到设备，请确保设备正在工作");
    }

    /**
     * 在BleService中检测到设备已连接，但相应的服务没有找到
    */
    @Override
    public void serviceNotInit() {
        showToast("连接未初始化，请稍候...");
        sendFail();
    }

    /**
     * 在HomeFragment中回调，请求发送信息
     * @param sendAction    发送的信息类别
     * @param text  要发送的信息
     */
    @Override
    public void send(SendAction sendAction,String text) {
//        if(action != SendAction.NONE) {
//            showToast("上一条消息还在发送，请等待！");
//            return;
//        }
        if(sendAction==SendAction.Words){
            Log.i("------Words进入发送队列------", "内容为" + text);
        } else if(sendAction==SendAction.Address){
            Log.i("-----Address进入发送队列-----", "内容为" + text );
        }

        if(!homeFragment.canSend){
            homeFragment.statesFail();
        }
        action = sendAction;
        sendText = text;
        chenckChannel();

    }

    /**
     * 在HomeFragment中回调，请求重新发送信息
     * @param sendAction    发送的信息类别
     * @param text  要发送的信息
     */
    @Override
    public void reSend(SendAction sendAction,String text) {

            if(sendAction==SendAction.Words){
                Log.i("------WordsRe进入发送队列----", "内容为" + text);
            } else if(sendAction==SendAction.Address){
                Log.i("-----AddressRe进入发送队列---", "内容为" + text );
            }

            cacheUtil.addUnCheckCacheContent(text);
            action = sendAction;
            sendText = text;
            chenckChannel();


    }

    /**
     * HomeFragment中测试使用
     * @param channel
     */
    @Override
    public void changeChannel(int channel) {
        if(bleService != null) {
            Constants.CHANNEL[2] = (byte)channel;
            bleService.writeInstruction(Constants.CHANNEL);
        }
    }

    /**
     * HomeFragment中的回调函数，获取新建的组
     * @return
     */
    @Override
    public GroupEntity getNewGroup() {
        if(haveNewGroup) {
            haveNewGroup = false;
            return groupEntity;
        }
        return null;
    }

    /**
     * HomeFragment中的回调方法
     * 停止录音
     */
    @Override
    public void stopSendSounds() {
        record.stopRecording();
    }

    /**
     * HomeFragment中的回调，重新连接设备
     * reconnect device
     */
    @Override
    public void reconnectDevice() {
        bindFrom = BineDeviceFrom.HF;
        if(bindAddress == null) {
            showToast("还未绑定设备！");
            return;
        }
        if(bleService == null) return;
        if(bleService.isBluetoothEnable()) {
            bleService.connect(bindAddress,true);
        }else {
            //先开启蓝牙
            reconnected = true;
            bleService.openBluetooth();
        }
    }

    /**
     * BindDeviceFragment的回调函数
     * @param device 要连接的BLE设备
     */
    @Override
    public void connectDevice(BluetoothDevice device) {
        bindFrom = BineDeviceFrom.BF;
        deviceName = device.getName();
        bindAddress = device.getAddress();
        if(bleService != null) {
            bleService.connect(bindAddress,false);
        }
        updateBoundedDevice(deviceName, bindAddress);
    }

    /**
     * BindDeviceFragment中的回调方法
     * @return 若蓝牙已打开则返回BlutootheAdapter
     */
    @Override
    public BluetoothAdapter requestScanDevice() {
        if(bleService.isBluetoothEnable()) {
            return bleService.getBleAdapter();
        }
        return null;
    }

    /**
     * BindDeviceFragment中的回调方法
     * 检查bleService是否已经绑定完毕
     * @return
     */
    @Override
    public boolean isBleServiceBinded() {
        return bleService != null;
    }

    /**
     * BindDeviceFragment中的回调方法
     * 请求打开蓝牙
     */
    @Override
    public void openBle() {
        bleService.openBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_GROUP && resultCode == Activity.RESULT_OK && data != null) {
            //返回新建的组或新加的组
            Bundle bundle = data.getExtras();
            if(bundle == null) return;
            groupEntity = bundle.getParcelable(GroupActivity.EXTRA_GROUP);
            if(groupEntity == null) return;
            haveNewGroup = true;
            myId = groupEntity.getTempId();
            if(homeFragment == null) {
                initHomeFragment();
                changeFragment(homeFragment);
            }
            lockerView.closeMenu();
        }else if(requestCode == HomeFragment.REQUEST_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            String address = data.getStringExtra(HomeFragment.EXTRA_LOCATION);
            if(!TextUtils.isEmpty(address)){
                homeFragment.sendMessage(address,SendAction.Address);
                if(homeFragment.cacheUtil.getUnCheckNum()==0){
                    send(SendAction.Address,address);
                }
            }
        }else if(requestCode == REQUEST_SETTING && data != null) {
            String newName = data.getStringExtra(Constants.NICKNAME);
            String path = data.getStringExtra(Constants.PHOTO_PATH);
            if(!TextUtils.isEmpty(name)) {//修改昵称
                name = newName;
                tvName.setText(newName);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(Constants.NICKNAME,newName);
                editor.apply();
            }

            if(!TextUtils.isEmpty(path)) {//修改头像
                int size = (int)(100 * getResources().getDisplayMetrics().density + 0.5f);
                size *= size;
                Bitmap bitmap = ImageUtil.createImageThumbnail(path,size);
                if(bitmap != null) {
                    imgPhoto.setImageDrawable(new CircleImageDrawable(bitmap));
                    bitmap = null;

                }
            }
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
            finish();
        }
    }
    private void setPhoto(CircleImageDrawable drawable) {//设置头像
    imgPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
    imgPhoto.setImageDrawable(drawable);
}

}
