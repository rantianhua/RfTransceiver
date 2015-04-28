package com.rftransceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adapter.ListConversationAdapter;
import com.blutooth.BluetoothFactory;
import com.datasets.ConversationData;
import com.fragments.BluetoothConnectFragment;
import com.util.Constants;

import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity implements View.OnClickListener,
        BluetoothConnectFragment.Connectlistener,AudioSender.TestSoundWithBluetooth {

    @InjectView(R.id.textView1)
    TextView tView;
    @InjectView(R.id.button1)
    Button connectbtn;
    @InjectView(R.id.button2)
    Button changenbtn;
    @InjectView(R.id.button3)
    Button stopbtn;
    @InjectView(R.id.tv_send_counts)
    TextView tvSendCounts;
    @InjectView(R.id.tv_receive_counts)
    TextView tvReceiveCounts;
    @InjectView(R.id.listview_conversation)
    ListView listView;
    @InjectView(R.id.et_send_message)
    EditText etSendMessage;
    @InjectView(R.id.btn_send)
    Button btnSend;
    @InjectView(R.id.btn_record_sound)
    Button btnRecordSounds;
    @InjectView(R.id.btn_play_sounds)
    Button btnPlaySounds;
    @InjectView(R.id.btn_clear_counts)
    Button btnClearCounts;

    public Usb_Device usb_Device;
    public boolean usb_connectresult;
    static int datalength = 0;
    private Audio_Recorder recorder;
    private Audio_Reciver reciver;
    private TimerTask task;

    private BluetoothAdapter bluetoothAdapter = null;

    //与BluetoothFactory交互的Handler
    private Handler bluetoothHandler =  null;

    private final String TAG = getClass().getSimpleName();

    private SendMode sendMode = SendMode.TEXT;    //标志发送模式,默认为文本发送

    /**
     *
     */
    StringBuilder sb = new StringBuilder();

    private int sumRec,sumSend;

    //蓝牙管理工厂
    private BluetoothFactory bluetoothFactory = null;

    private enum SendMode {
        TEXT,
        SOUND
    }

    private ListConversationAdapter conversationAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initUsb();
        System.loadLibrary("speex");
        initViews();

        //开启蓝牙并开启蓝牙连接服务端
        openBluetooth();
        //实例化蓝牙工厂并开启蓝牙服务
        if(bluetoothFactory == null) {
            if(bluetoothHandler == null) {
                initBluetoothHandler();
            }
            bluetoothFactory = new BluetoothFactory(bluetoothHandler);
        }
        bluetoothFactory.start();
        //接收器实例
        reciver = new Audio_Reciver();
        //开启解码
        reciver.startReceiver();
    }

    private void initBluetoothHandler() {
        bluetoothHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constants.MSG_WHAT_STATE:
                        switch (msg.arg1) {
                            case BluetoothFactory.STATE_CONNECTED:
                                setTitle("已连接设备");
                                break;
                            case BluetoothFactory.STATE_CONNECTING:
                                setTitle("正在连接...");
                                break;
                            case BluetoothFactory.STATE_LISTENING:
                                setTitle("正在监听...");
                                break;
                            case BluetoothFactory.STATE_NONE:
                                setTitle("无连接");
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        switch (msg.arg2) {
                            case 0:
                                //接收到的为语音消息
                                reciver.cacheData((byte[])msg.obj,msg.arg1);
                                sumRec += msg.arg1;
                                tvReceiveCounts.setText(getString(R.string.receive_counts, sumRec));
                                break;
                            case 1:
                                //接收到的为文本消息
                                sb.append(new String((byte[])msg.obj));
                                Log.e("receive message is ",sb.toString());
                                break;
                            case 2:
                                //接收完成
                                showToast("接收完成");
                                break;
                            case 3:
                                //接收到最后一个文本信息包
                                sb.append(new String((byte[])msg.obj));
                                //此处更新UI，显示收到的文本信息
                                ConversationData conversationData = new ConversationData();
                                conversationData.setContent(sb.toString());
                                conversationData.setConversationType(ListConversationAdapter.ConversationType.Other);
                                sb.delete(0,sb.length());
                                break;
                        }
                        reciver.cacheData((byte[])msg.obj,msg.arg1);
                        break;
                    case Constants.MESSAGE_WRITE:
                        sumSend += msg.arg1;
                        tvSendCounts.setText(getString(R.string.send_counts,sumSend));
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
                //关闭连接
                bluetoothFactory.stop();
                return false;
            case R.id.action_connect_device:
                //连接设备,打开蓝牙连接对话框
                BluetoothConnectFragment.getInstance(this).show(getSupportFragmentManager(),"connect_bluetooth");
                return false;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothFactory != null) {
            bluetoothFactory.stop();
        }
    }

    private void initUsb() {
//        usb_Device = new Usb_Device();
//        usb_Device.Usb_GetPermisson(MainActivity.this);
//        final StringBuilder sb = new StringBuilder();
//        //主线程获取并设置handler，what为线程标识 obj为缓冲区   缓冲区前两位为buffer总长度
//
//        usb_Device.usb_handle = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                if (msg.what == 0x1234) {
//                    DataPack temDataPack = new DataPack();
//                    temDataPack = (DataPack) msg.obj;
//                    datalength += temDataPack.length;
//                    tvSendCounts.setText(datalength + "");
//                    sb.setLength(0);
//                    for (int i = 0; i < temDataPack.length; i++) {
//                        sb.append(String.format("%#x ", temDataPack.Inbuffer[i]));
//                    }
//                    tv.setText(sb);
//                }
//            }
//        };

    }

    //打开蓝牙
    private void openBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //检查是否支持蓝牙
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.enable()) {
                //直接打开，不通知用户
                bluetoothAdapter.enable();
                //打开设备的可见性
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
        btnPlaySounds.setOnClickListener(this);
        btnRecordSounds.setOnClickListener(this);
        btnClearCounts.setOnClickListener(this);

        //为发送实体帮
        AudioSender.sendListener = this;
        btnRecordSounds.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getAction()) {
                    sendMode = SendMode.SOUND;
                    stopReceiver();
                    startRecord();
                    return true;
                }
                if (MotionEvent.ACTION_UP == event.getAction()) {
                    stopRecord();
                    startReceive();
                    return true;
                }
                return false;
            }
        });

        conversationAdapter = new ListConversationAdapter(this);
        listView.setAdapter(conversationAdapter);

        //usbActions();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                //发送文本
                sendMode = SendMode.TEXT;
                String content = etSendMessage.getText().toString();
                if(!TextUtils.isEmpty(content)) {
                    sendMessage(content.getBytes(),content.getBytes().length);
                    //更新UI
                    ConversationData conversationData = new ConversationData();
                    conversationData.setContent(content);
                    conversationData.setConversationType(
                            ListConversationAdapter.ConversationType.Me
                    );
                    conversationAdapter.addData(conversationData);
                }
                break;
            case R.id.btn_clear_counts:
                //清空计数器
                sumRec = 0;
                tvSendCounts.setText(getString(R.string.send_counts,0));
                tvReceiveCounts.setText(getString(R.string.receive_counts, sumRec));
                break;
            case R.id.btn_play_sounds:
                //播放语音
                break;
            case R.id.btn_record_sound:
                sendMode = SendMode.SOUND;
//                stopReceiver();
//                startRecord();
                //录音
                break;
            default:
                break;
        }
    }

    boolean noty = false;

    //发送信息
    private void sendMessage(Object content,int size) {
        /**
         * 每次发送的包为66字节
         * 帧头帧尾固定为0x01
         * 帧尾表示数据是用0x04,表示结束包用0x07
         * 每次传输第一个包表示要发送语音还是文本
         * 语音是用64个0x03表示
         * 文本是用64个0x02表示
         */
        if (SendMode.TEXT == sendMode) {
            //发送文本，告知对方本次传输文本
            byte[] buff = new byte[66];
            buff[0] = (byte) 0x01;
            buff[65] = (byte) 0x04;
            for (int i = 1; i < 65; i++) {
                buff[i] = (byte) 0x02;
            }
            sendToDst(buff);
        } else {
            if(!noty) {
                //发送语音，先通知对方
                byte[] note = new byte[66];
                note[0] = (byte) 0x01;
                note[65] = (byte) 0x04;
                for (int i = 1; i < 65; i++) {
                    note[i] = (byte) 0x02;
                }
                sendToDst(note);
                noty = true;
            }
            byte[] sounds =new byte[66];
            sounds[0] = (byte) 0x01;
            sounds[65] = (byte) 0x04;
            byte[] data = (byte[]) content;
            System.arraycopy(data,0,sounds,1,size);
            if(size < 64) {
                sounds[65] = (byte)0x07;    //表示结束包
                sounds[64] = (byte) size;   //原数据段的最后一位表示实际数据的长度
                noty = false;
            }
            sendToDst(sounds);
            data = null;
        }
    }

    private void sendToDst(byte[] buff) {
        if(bluetoothFactory != null) {
            bluetoothFactory.write(buff);
        }
    }

    @Override
    public void startConnect(BluetoothDevice device) {
        //客户端连接
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

    private void startReceive() {
        //开始接收
        if(reciver == null) {
            reciver = new Audio_Reciver();
        }
        reciver.startReceiver();
    }

    private void stopReceiver() {
        if(reciver != null ) {
            reciver.stopReceiver();
        }
    }

    //开始录音
    private void startRecord() {
        if(recorder == null) {
            recorder=new Audio_Recorder();
        }
        AudioSender.end = false;
        recorder.startRecording();//启动录制
    }

    //停止录音
    private void stopRecord() {
        if(recorder !=null && recorder.isRecording())
        {
            AudioSender.end = true;
            recorder.stopRecording();
        }
    }

        @Override
        public void sendSound (Object data,int size){
            sendMessage(data,size);
        }

    private void usbActions() {
        connectbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if (usb_Device.usb_permisson)//如果取得权限则进行连接
                {
                    if (usb_Device.Usb_Connect())
                        tView.setText("连接成功");

                } else
                    tView.setText("无权限");
            }
        });

        changenbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (usb_Device.usb_state) {
                    //buffer为命令内容
                    byte buffer[] = new byte[64];
                    buffer[0] = (byte) 0x01;
				/*byte[] text=editText.getText().toString().getBytes();

				for(int i=0;i<text.length;i++)
					buffer[i+1]=text[i];*/
                    buffer[1] = (byte) 0x02;
                    buffer[63] = (byte) 0x58;
                    int length = buffer.length;


                    if (usb_Device.Usb_Transfer(buffer, length))
                        tView.setText("命令发送成功");

                    else
                        tView.setText("命令发送失败");
                } else
                    tView.setText("不存在设备");
            }


        });


        stopbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (usb_Device.usb_state) {
                    //buffer为命令内容
                    byte buffer[] = new byte[64];
                    buffer[0] = (byte) 0x02;

                    int length = buffer.length;


                    if (usb_Device.Usb_Transfer(buffer, length))
                        tView.setText("命令发送成功");

                    else
                        tView.setText("命令发送失败");
                } else
                    tView.setText("不存在设备");
            }


        });
    }
}
