package com.rftransceiver.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.brige.blutooth.BluetoothFactory;
import com.my_interface.SendMessageListener;
import com.rftransceiver.R;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.fragments.BluetoothConnectFragment;
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
        BluetoothConnectFragment.Connectlistener,SendMessageListener{

    @InjectView(R.id.listview_conversation)
    ListView listView;
    @InjectView(R.id.et_send_message)
    EditText etSendMessage;
    @InjectView(R.id.btn_send)
    Button btnSend;
    @InjectView(R.id.btn_sounds)
    Button btnSounds;

    private Handler dataExchangeHandler =  null;

    private final String TAG = getClass().getSimpleName();


    private BluetoothFactory bluetoothFactory = null;

    private ListConversationAdapter conversationAdapter = null;

    private TextEntity textEntity;

    private Audio_Recorder record;

    private Audio_Reciver receiver;

    private byte[] reset = new byte[66];

    private  ParseFactory parseFactory;

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
        bluetoothFactory = new BluetoothFactory(dataExchangeHandler);

        receiver = new Audio_Reciver();

        DataPacketOptions soundsOptions = new DataPacketOptions(DataPacketOptions.Data_Type_InOptions.sounds,
                5);
        SoundsEntity soundsEntity = new SoundsEntity();
        soundsEntity.setOptions(soundsOptions);
        soundsEntity.setSendListener(this);
        record = new Audio_Recorder();
        record.setSoundsEntity(soundsEntity);

        textEntity = new TextEntity();
        DataPacketOptions textOptions = new DataPacketOptions(DataPacketOptions.Data_Type_InOptions.text,
                3);
        textEntity.setOptions(textOptions);
        textEntity.setSendListener(this);

        parseFactory = new ParseFactory();
        parseFactory.setHandler(dataExchangeHandler);
        parseFactory.setSoundsOptions(soundsOptions);
        parseFactory.setTextOptions(textOptions);
        bluetoothFactory.setParseFactory(parseFactory);

        soundsOptions = null;
        textOptions = null;

        reset[0] = (byte) 0xfe;
        reset[1] = reset[0];
        reset[2] = reset[0];
        reset[3] = reset[0];

        startReceive();

        getSupportActionBar().setSubtitle(getString(R.string.channel_1));
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
                                setTitle("无连接");
                                break;
                            case 1:
                                setTitle("正在监听...");
                                break;
                            case 2:
                                setTitle("正在连接...");
                                break;
                            case 3:
                                setTitle("已连接设备");
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        switch (msg.arg1) {
                            case 0:
                                switch (msg.arg2) {
                                    case 0:
                                        receiver.startReceiver();
                                        btnSounds.setText(getString(R.string.sounds_im));
                                        break;
                                    case 1:
                                        receiver.stopReceiver();
                                        btnSounds.setText(getString(R.string.record_sound));
                                        break;
                                    case 2:
                                        //receive sounds data
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
                                        getSupportActionBar().setSubtitle(getString(R.string.channel_1));
                                        break;
                                    case 1:
                                        getSupportActionBar().setSubtitle(getString(R.string.channel_2));
                                        break;
                                    case 2:
                                        getSupportActionBar().setSubtitle(getString(R.string.channel_3));
                                        break;
                                    case 3:
                                        getSupportActionBar().setSubtitle(getString(R.string.channel_4));
                                        break;
                                }
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
                bluetoothFactory.stop();
                return true;
            case R.id.action_connect_device:
                //open a dialogFragment to discover and pair a bluetooth device
                BluetoothConnectFragment.getInstance(this).show(getSupportFragmentManager(),"connect_bluetooth");
                return true;
            case R.id.action_clear:
                conversationAdapter.clear();
                return true;
            case R.id.action_reset_scm:
                bluetoothFactory.write(reset,true);
                receiver.stopReceiver();
                record.stopRecording();
                receiver.clear();
                parseFactory.resetSounds();
                btnSounds.setText(getString(R.string.record_sound));
                getSupportActionBar().setSubtitle(getString(R.string.channel_1));
                return true;
            case R.id.action_channel_1:
                changeChanel((byte) 0x00);
                return true;
            case R.id.action_channel_2:
                changeChanel((byte) 0x01);
                return true;
            case R.id.action_channel_3:
                changeChanel((byte) 0x02);
                return true;
            case R.id.action_channel_4:
                changeChanel((byte) 0x03);
                return true;
        }
        return false;
    }

    private void changeChanel(byte tail) {
        byte[] chanel = new byte[Constants.Packet_Length];
        chanel[0] = (byte) 0x01;
        chanel[Constants.Packet_Length-2] = tail;
        chanel[Constants.Packet_Length-1] = (byte) 0x07;
        bluetoothFactory.write(chanel,false);
        chanel = null;
    }

    private final BroadcastReceiver bluetoothSate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        showToast("蓝牙已关闭");
                        bluetoothFactory.stop();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothFactory.start();
                        break;
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //register a broadcast to listen the state of bluetooth
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(bluetoothSate,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregister the broadcast
        this.unregisterReceiver(bluetoothSate);
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
    }

    @Override
    public void sendText(byte[] temp, boolean end) {
        bluetoothFactory.write(temp,end);
    }

    //open the bluetooth
    private void openBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //check this device weather support bluetooth or not
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.enable()) {
                bluetoothAdapter.enable();
                Intent discoverableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1000);
                startActivity(discoverableIntent);
            }
        }else {
            showToast("手机不支持蓝牙");
            finish();
        }
    }

    private void initViews() {
        ButterKnife.inject(this);
        btnSend.setOnClickListener(this);
        conversationAdapter = new ListConversationAdapter(this);
        listView.setAdapter(conversationAdapter);
        btnSounds.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
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
                    //start record
                    receiver.stopReceiver();
                    startRecord();
                    btnSounds.setText(getString(R.string.recording_sound));
                }
                else if(btnSounds.getText().equals(getString(R.string.recording_sound))) {
                    //stop recording
                    record.stopRecording();
                    btnSounds.setText(getString(R.string.record_sound));
                    startReceive();
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

    private void startRecord() {
        //
        receiver.stopReceiver();
        record.startRecording();
    }

    private void startReceive() {
        record.stopRecording();
        receiver.startReceiver();
    }

    @Override
    public void startConnect(BluetoothDevice device) {
       if(bluetoothFactory != null) {
           bluetoothFactory.connect(device);
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
    public void sendSound (byte[] data,boolean end){
        if(bluetoothFactory != null) {
            bluetoothFactory.write(data,end);
        }
    }

}
