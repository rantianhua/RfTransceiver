package com.rftransceiver.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
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
import com.brige.wifi.WifiNetService;
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


public class MainActivity extends Activity implements View.OnClickListener,
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

    private ListConversationAdapter conversationAdapter = null; //the adapter of listView

    private TextEntity textEntity;  //the text entity to decide how packet text data to be sent

    private Audio_Recorder record;  //the recorder to record sounds and send

    private Audio_Reciver receiver; //handle the received sounds data---to play theme

    private  ParseFactory parseFactory; //manage how to parse received packeted data

    private BluetoothLeService bluetoothLeService; //ble connection controller

    private String deviceName = null;

    private volatile boolean isReceiving = false;

    private boolean findWriteCharac = false;

    private ACTION action = ACTION.NONE;

    private enum ACTION {
        TEXT,
        SOUNDS,
        NONE
    }

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
                setTitle(getString(R.string.connected, deviceName));
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setTitle(getString(R.string.disconnected, ""));
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                btnSounds.setClickable(true);
                btnSend.setClickable(true);
                findWriteCharac = true;
            }
        }
    };


    private final ServiceConnection serviceConnectionBle = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            bluetoothLeService.setSendMessageListener(MainActivity.this);
            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService.setSendMessageListener(null);
            bluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initUsb();
        System.loadLibrary("speex");
        initViews();

        //open bluetooth and start bluetooth sever
        openBluetooth();

        initDataExchangeHandler();

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

        //bind the ble service
        bindService(new Intent(this, BluetoothLeService.class), serviceConnectionBle, BIND_AUTO_CREATE);
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
                                break;
                            case 1:
                                setTitle(getString(R.string.bluetooth_listening));
                                break;
                            case 2:
                                setTitle(getString(R.string.bluetooth_connecting));
                                break;
                            case 3:
                                setTitle(getString(R.string.bluetooth_connected));
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        switch (msg.arg1) {
                            case 0:
                                switch (msg.arg2) {
                                    case 0:
                                        //start to receive sounds data
                                        isReceiving = true;
                                        receiver.startReceiver();
                                        btnSounds.setText(getString(R.string.sounds_im));
                                        btnSounds.setClickable(false);
                                        break;
                                    case 1:
                                        //end to receive sounds data
                                        //stopReceiveSounds();
                                        break;
                                    case 2:
                                        //cache the received sounds data
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
                                showToast("设置同步字成功");
                                break;
                            case 4:
                                showToast("读到rssi值是："+msg.arg2);
                                break;
                            case 5:
                                if(msg.arg2 == 0) {
                                    isReceiving = false;
                                    stopReceiveSounds();
                                    if(action == ACTION.SOUNDS) {
                                        //start record
                                        action = ACTION.NONE;
                                        record.startRecording();
                                        btnSounds.setText(getString(R.string.recording_sound));
                                    }else if(action == ACTION.TEXT) {
                                        action = ACTION.NONE;
                                        sendText();
                                    }
                                }else if(msg.arg2 == 1) {
                                    if(action != ACTION.NONE) {
                                        showToast("信道占用中，不能发送");
                                    }
                                }
                                tvStyle.setText(getString(R.string.style_label,msg.obj));
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
        btnSounds.setText(getString(R.string.record_sound));
        btnSounds.setClickable(true);
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
                startActivity(new Intent(MainActivity.this,SearchActivity.class));
                return true;
            case R.id.action_clear:
                conversationAdapter.clear();
                return true;
            case R.id.action_reset_scm:
                resetCms(true);
                return true;
            case R.id.action_choose_channel:
                chooseChannel();
                return true;
            case R.id.action_create_group:
                createGroup();
                return true;
            case R.id.action_add_group:
                addGroup();
                return true;
            case R.id.action_asyn_word:
                sendAsyncWord();
                return true;
            case R.id.action_rssi:
                if(!isReceiving && findWriteCharac) {
                    bluetoothLeService.writeInstruction(Constants.RSSI);
                }
                return true;
            case R.id.action_channel_state:
                if(!isReceiving && findWriteCharac) {
                    bluetoothLeService.writeInstruction(Constants.CHANNEL_STATE);
                }
                return true;
        }
        return false;
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
        btnSounds.setText(getString(R.string.record_sound));
        btnSounds.setClickable(true);
        btnSend.setClickable(true);
        isReceiving = false;
        if(write) {
            if(!isReceiving && findWriteCharac) {
                bluetoothLeService.writeInstruction(Constants.RESET);
                updateChannel(getString(R.string.channel_1));
            }
        }
    }

    //close a bluetooth connection
    private void closeConnection() {
        if(bluetoothLeService != null) {
            bluetoothLeService.disconnect();
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

    private void changeChanel(byte channel) {
        if(!isReceiving && findWriteCharac) {
            Constants.CHANNEL[2] = channel;
            bluetoothLeService.writeInstruction(Constants.CHANNEL);
        }
    }

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
                        break;
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(bluetoothSate,
                new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
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
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }else {
            showToast(getString(R.string.device_bluetooth_not_support));
            //finish();
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
                0));
        btnSounds.setClickable(false);
        btnSend.setClickable(false);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                if(!isReceiving && findWriteCharac) {
                    action = ACTION.TEXT;
                    chenckChannel();
                }else {
                    showToast(getString(R.string.cannot_send));
                }
                break;
            case R.id.btn_sounds:
                if(btnSounds.getText().equals(getString(R.string.record_sound))) {
                    if(!isReceiving && findWriteCharac) {
                        action = ACTION.SOUNDS;
                        chenckChannel();
                    }else {
                        showToast(getString(R.string.cannot_send));
                    }
                }
                else if(btnSounds.getText().equals(getString(R.string.recording_sound))) {
                    //stop recording
                    record.stopRecording();
                    btnSounds.setText(getString(R.string.record_sound));
                }
                break;
            default:
                break;
        }
    }

    private void chenckChannel() {
        bluetoothLeService.writeInstruction(Constants.CHANNEL_STATE);
    }

    private void sendText() {
        btnSend.setClickable(false);
        String sendContent = etSendMessage.getText().toString();
        etSendMessage.setText("");
        if(TextUtils.isEmpty(sendContent)) {
            return;
        }
        textEntity.unpacking(sendContent);
        hideInputSoft();
        ConversationData data = new ConversationData();
        data.setContent(sendContent);
        data.setConversationType(ListConversationAdapter.ConversationType.Me);
        conversationAdapter.addData(data);
        data = null;
        btnSend.setClickable(true);
    }

    private void hideInputSoft() {
        //InputMethodManager im = getSystemService(INPUT_METHOD_SERVICE);
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
        bluetoothLeService.write(data);
        data = null;
    }

    @Override
    public void sendUnPacketedData(byte[] data,int mode) {
        switch (mode) {
            case 0:
                //correct data
                if(data != null) {
                    Log.e("sendUnPacketedData","content " + data[1]);
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
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
