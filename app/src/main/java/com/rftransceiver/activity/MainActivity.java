package com.rftransceiver.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.blutooth.le.BluetoothLeService;
import com.brige.blutooth.normal.BluetoothFactory;
import com.my_interface.SendMessageListener;
import com.rftransceiver.R;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.util.Constants;
import com.source.DataPacketOptions;
import com.source.parse.ParseFactory;
import com.source.sounds.Audio_Reciver;
import com.source.sounds.Audio_Recorder;
import com.source.sounds.SoundsEntity;
import com.source.text.TextEntity;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity implements View.OnClickListener,
        SearchActivity.Connectlistener,SendMessageListener{

    @InjectView(R.id.listview_conversation)
    ListView listView;
    @InjectView(R.id.et_send_message)
    EditText etSendMessage;
    @InjectView(R.id.btn_send)
    Button btnSend;
    @InjectView(R.id.btn_sounds)
    Button btnSounds;
    @InjectView(R.id.tv_current_channel)
    TextView tvChannel;
    @InjectView(R.id.tv_current_style)
    TextView tvStyle;

    private Handler dataExchangeHandler =  null;    //exchange data with work thread

    private final String TAG = getClass().getSimpleName();

    private BluetoothFactory bluetoothFactory = null;   //manage normal bluetooth

    private ListConversationAdapter conversationAdapter = null; //the adapter of listView

    private TextEntity textEntity;  //the text entity to decide how packet text data to be sent

    private Audio_Recorder record;  //the recorder to record sounds and send

    private Audio_Reciver receiver; //handle the received sounds data---to play theme

    private  ParseFactory parseFactory; //manage how to parse received packeted data

    private boolean receiveError = false;   //mark have received a error packet or not

    public Blue_Style blueStyle = Blue_Style.Ble;  //the default style is ble

    /**
     * the enum to mark bluetooth style
     */
    public enum Blue_Style {
        Normal,
        Ble
    }

    private boolean supportBle = false; //mark device support ble or not

    private BluetoothLeService bluetoothLeService; //ble connection controller

    private boolean leServiceBind = false;  //mark weather bind leService or not

    private ServiceConnection mServiceConnection = null;    // Code to manage Service lifecycle.

    private boolean bleConnected = false;

    private boolean bluetoothConnected = false;

    private String deviceName = null;

    private BluetoothGattCharacteristic writeCharacter = null;

    private BluetoothGattCharacteristic notifyCharacter = null;

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
                bleConnected = true;
                setTitle(getString(R.string.connected, deviceName));
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                bleConnected = false;
                setTitle(getString(R.string.disconnected, ""));
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                if(writeCharacter == null) {
                    writeCharacter = bluetoothLeService.getWriteCharac();
                    final int charaProp = writeCharacter.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (notifyCharacter != null) {
                            bluetoothLeService.setCharacteristicNotification(
                                    notifyCharacter, false);
                            notifyCharacter = null;
                        }
                        bluetoothLeService.readCharacteristic(writeCharacter);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        notifyCharacter = writeCharacter;
                        bluetoothLeService.setCharacteristicNotification(
                                writeCharacter, true);
                    }
                }
            }
//            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
//            }
        }
    };

    public volatile static boolean changeChannel = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initUsb();
        System.loadLibrary("speex");
        initViews();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            supportBle = true;
        }
        //open bluetooth and start bluetooth sever
        openBluetooth();

        initDataExchangeHandler();

        //initial the receiver
        receiver = new Audio_Reciver();
        DataPacketOptions soundsOptions = new DataPacketOptions(DataPacketOptions.Data_Type_InOptions.sounds,
                5);
        SoundsEntity soundsEntity = new SoundsEntity();
        soundsEntity.setOptions(soundsOptions);
        soundsEntity.setSendListener(this);

        //initial the recorder
        record = new Audio_Recorder();
        record.setSoundsEntity(soundsEntity);

        //initial the text entity
        textEntity = new TextEntity();
        DataPacketOptions textOptions = new DataPacketOptions(DataPacketOptions.Data_Type_InOptions.text,
                5);
        textEntity.setOptions(textOptions);
        textEntity.setSendListener(this);

        //initial parseFactory
        parseFactory = new ParseFactory();
        parseFactory.setHandler(dataExchangeHandler);
        parseFactory.setSoundsOptions(soundsOptions);
        parseFactory.setTextOptions(textOptions);

        soundsOptions = null;
        textOptions = null;

        //startReceive();

        updateChannel(getString(R.string.channel_1));

        if(blueStyle == Blue_Style.Ble) {
            initBLETool();
        }else {
            initNormalBluetoothTool();
        }
    }

    private void initDataExchangeHandler() {
        dataExchangeHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constants.MSG_WHAT_STATE:
                        switch (msg.arg1) {
                            case 0:
                                setTitle(getString(R.string.bluetooth_none_connection));
                                bluetoothConnected = false;
                                break;
                            case 1:
                                setTitle(getString(R.string.bluetooth_listening));
                                break;
                            case 2:
                                setTitle(getString(R.string.bluetooth_connecting));
                                break;
                            case 3:
                                setTitle(getString(R.string.bluetooth_connected));
                                bluetoothConnected = true;
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        switch (msg.arg1) {
                            case 0:
                                switch (msg.arg2) {
                                    case 0:
                                        //start to receive sounds data
                                        receiver.startReceiver();
                                        btnSounds.setText(getString(R.string.sounds_im));
                                        break;
                                    case 1:
                                        //end to receive sounds data
                                        receiver.stopReceiver();
                                        btnSounds.setText(getString(R.string.record_sound));
                                        break;
                                    case 2:
                                        //cache the received sounds data
                                        receiveError = false;
                                        byte[] sound = (byte[]) msg.obj;
                                        receiver.cacheData(sound,Constants.Small_Sounds_Packet_Length);
                                        break;
                                }
                                break;
                            case 1:
                                //receive text data
                                byte[] data = (byte[]) msg.obj;
                                ConversationData text = new ConversationData();
                                text.setContent(new String(data));
                                text.setConversationType(ListConversationAdapter.ConversationType.Other);
                                conversationAdapter.addData(text);
                                break;
                            case 2:
                                switch (msg.arg2) {
                                    case 0:
                                        updateChannel(getString(R.string.channel_1));
                                        break;
                                    case 1:
                                        updateChannel(getString(R.string.channel_2));
                                        break;
                                    case 2:
                                        updateChannel(getString(R.string.channel_3));
                                        break;
                                    case 3:
                                        updateChannel(getString(R.string.channel_4));
                                        break;
                                }
                                break;
                            case 3:
                                if(!receiveError) {
                                    showToast(getString(R.string.received_error_packet));
                                }
                                //the packet is wrong
                                receiveError = true;
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

    private void initNormalBluetoothTool() {
        if(bluetoothFactory == null) {
            bluetoothFactory = new BluetoothFactory(dataExchangeHandler);
            bluetoothFactory.setParseFactory(parseFactory);
        }
    }

    private void initBLETool() {
        if(mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                    bluetoothLeService.setSendMessageListener(MainActivity.this);
                    if (!bluetoothLeService.initialize()) {
                        Log.e(TAG, "Unable to initialize Bluetooth");
                        finish();
                    }
                    leServiceBind = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    bluetoothLeService.setSendMessageListener(null);
                    bluetoothLeService = null;
                    leServiceBind = false;
                }
            };
        }
        if(!leServiceBind) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    //update the channel in UI
    private void updateChannel(String channel) {
        try {
            tvChannel.setText(getString(R.string.channel_label,channel));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close_device:
                //close connection
                closeConnection();
                return true;
            case R.id.action_connect_device:
                //open a dialogFragment to discover and pair a bluetooth device
                SearchActivity.connectlistener = MainActivity.this;
                SearchActivity.style = blueStyle;
                startActivity(new Intent(MainActivity.this,SearchActivity.class));
                return true;
            case R.id.action_clear:
                conversationAdapter.clear();
                return true;
            case R.id.action_reset_scm:
                if(blueStyle == Blue_Style.Ble) {
                    bluetoothLeService.write(Constants.Reset,false);
                }else {
                    //bluetoothFactory.write(Constants.Reset,true);
                }
                receiver.stopReceiver();
                record.stopRecording();
                receiver.clear();
                parseFactory.resetSounds();
                btnSounds.setText(getString(R.string.record_sound));
                updateChannel(getString(R.string.channel_1));
                return true;
            case R.id.action_choose_channel:
                chooseChannel();
                return true;
            case R.id.action_choose_bluetooth_style:
                chooseBluetoothStyle();
                return true;
        }
        return false;
    }

    private void chooseBluetoothStyle() {
//        new AlertDialog.Builder(this)
//                .setTitle(getString(R.string.choose_bluetooth_style))
//                .setIcon(android.R.drawable.ic_dialog_info)
//                .setSingleChoiceItems(new String[]{getString(R.string.bluetoothNormal),
//                                getString(R.string.bluetooth_ble),
//                        }, 0,
//                        new DialogInterface.OnClickListener() {
//
//                            public void onClick(DialogInterface dialog, int which) {
//                                receiver.stopReceiver();
//                                if (which == 0) {
//                                    blueStyle = Blue_Style.Normal;
//                                    if (leServiceBind) {
//                                        unbindService(mServiceConnection);
//                                    }
//                                    initNormalBluetoothTool();
//                                } else if (which == 1) {
//                                    blueStyle = Blue_Style.Ble;
//                                    if (bluetoothFactory != null) {
//                                        bluetoothFactory.stop();
//                                        bluetoothFactory.setParseFactory(null);
//                                        bluetoothFactory = null;
//                                    }
//                                    initBLETool();
//                                }
//                                dialog.dismiss();
//                            }
//                        }
//                )
//                .setNegativeButton("取消", null)
//                .show();
    }

    //close a bluetooth connection
    private void closeConnection() {
        if(blueStyle == Blue_Style.Ble) {
            bluetoothLeService.disconnect();
        }else {
            bluetoothFactory.stop();
        }
    }

    //choose or change channel
    private void chooseChannel() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_channel))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setSingleChoiceItems(new String[] {getString(R.string.channel_1),
                                getString(R.string.channel_2),
                                getString(R.string.channel_3),
                                getString(R.string.channel_4)}, 0,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                changeChanel((byte)which);
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton("取消", null)
                .show();
    }

    private void changeChanel(byte tail) {
        byte[] chanel = new byte[Constants.Packet_Length];
        chanel[0] = (byte) 0x01;
        chanel[Constants.Packet_Length-2] = tail;
        chanel[Constants.Packet_Length-1] = (byte) 0x07;
        if(blueStyle == Blue_Style.Ble) {
            bluetoothLeService.write(chanel,false);
            changeChannel = true;
        }else {
            //bluetoothFactory.write(chanel,false);
        }
        chanel = null;
    }

    private final BroadcastReceiver bluetoothSate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        showToast(getString(R.string.bluetooth_closed));
                        if(bluetoothFactory != null) {
                            bluetoothFactory.stop();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //bluetoothFactory.start();
                        break;
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //register a broadcast to listen the state of bluetooth
        if(blueStyle == Blue_Style.Ble) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }else {
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            this.registerReceiver(bluetoothSate,intentFilter);
            receiver.startReceiver();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregister the broadcast
        if(blueStyle == Blue_Style.Ble) {
            unregisterReceiver(mGattUpdateReceiver);
        }else {
            this.unregisterReceiver(bluetoothSate);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        parseFactory = null;

        record.stopRecording();
        receiver.stopReceiver();
        textEntity.setSendListener(null);
        textEntity.setOptions(null);

        if(bluetoothFactory != null) {
            bluetoothFactory.stop();
            bluetoothFactory.setParseFactory(null);
            bluetoothFactory = null;
        }

        record = null;
        receiver = null;
        textEntity = null;

        if(leServiceBind) {
            unbindService(mServiceConnection);
            bluetoothLeService = null;
        }
    }

    //open the bluetooth
    private void openBluetooth() {
        final BluetoothAdapter bluetoothAdapter;
        if(supportBle) {
            // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
            // BluetoothAdapter through BluetoothManager.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        //check this device weather support bluetooth or not
        //if support then enable the bluetooth
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.enable()) {
                bluetoothAdapter.enable();
            }
        }else {
            showToast(getString(R.string.device_bluetooth_not_support));
            finish();
        }
    }

    private void initViews() {
        ButterKnife.inject(this);
        btnSend.setOnClickListener(this);
        conversationAdapter = new ListConversationAdapter(this);
        listView.setAdapter(conversationAdapter);
        btnSounds.setOnClickListener(this);
        tvChannel.setText(getString(R.string.channel_label,
                getString(R.string.channel_1)));
        tvStyle.setText(getString(R.string.style_label,
                getString(R.string.blue_sytle_ble)));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                if(!bluetoothConnected && !bleConnected){
                    showToast(getString(R.string.not_connect_device));
                    return;
                }
                btnSend.setClickable(false);
                String sendContent = etSendMessage.getText().toString();
                etSendMessage.setText("");
                if(!TextUtils.isEmpty(sendContent)) {
                    textEntity.unpacking(sendContent);
                    hideInputSoft();
                }
                ConversationData data = new ConversationData();
                data.setContent(sendContent);
                data.setConversationType(ListConversationAdapter.ConversationType.Me);
                conversationAdapter.addData(data);
                data = null;
                btnSend.setClickable(true);
                break;
            case R.id.btn_sounds:
                if(btnSounds.getText().equals(getString(R.string.record_sound))) {
                    if(!bluetoothConnected && !bleConnected){
                        showToast(getString(R.string.not_connect_device));
                        return;
                    }
                    //start record
                    receiver.stopReceiver();
                    record.startRecording();
                    btnSounds.setText(getString(R.string.recording_sound));
                }
                else if(btnSounds.getText().equals(getString(R.string.recording_sound))) {
                    //stop recording
                    record.stopRecording();
                    btnSounds.setText(getString(R.string.record_sound));
                    receiver.startReceiver();
                }
                else if(btnSounds.getText().equals(getString(R.string.sounds_im))) {
                    receiver.stopReceiver();
                    btnSounds.setText(getString(R.string.record_sound));
                }
                break;
            default:
                break;
        }
    }

    private void hideInputSoft() {
        //InputMethodManager im = getSystemService(INPUT_METHOD_SERVICE);
    }

    @Override
    public void startConnect(BluetoothDevice device) {
        deviceName = device.getName();
        if(blueStyle == Blue_Style.Ble) {
            bluetoothLeService.connect(device.getAddress());
        }else {
            if(bluetoothFactory != null) {
                bluetoothFactory.connect(device);
            }
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
        if(blueStyle == Blue_Style.Ble) {
            if(bluetoothLeService != null) {
                bluetoothLeService.write(data,true);
            }
        }
        else {
            if(bluetoothFactory != null) {
                //bluetoothFactory.write(data,end);
            }
        }
        data = null;
    }

    @Override
    public void sendUnPacketedData(byte[] data,int mode) {
        switch (mode) {
            case 0:
                receiveError = false;
                //correct data
                if(data != null) {
                    parseFactory.sendToRelativeParser(data);
                }
                return;
            case 1:
                //wrong data
                if(!receiveError) {
                    showToast(getString(R.string.received_error_packet));
                }
                receiveError = true;
                return;
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
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
