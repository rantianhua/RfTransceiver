package com.rftransceiver.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.brige.blutooth.le.BluetoothLeService;
import com.rftransceiver.R;
import com.rftransceiver.activity.MainActivity;
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

/**
 * Created by rantianhua on 15-6-14.
 */
public class HomeFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.listview_conversation)
    ListView listView;
    @InjectView(R.id.et_send_message)
    EditText etSendMessage;
    @InjectView(R.id.btn_send)
    Button btnSend;
    @InjectView(R.id.btn_sounds)
    Button btnSounds;

    /**
     * the reference of callback interface
     */
    private CallbackInHomeFragment callback;

    private ListConversationAdapter conversationAdapter = null; //the adapter of listView

    /**
     *  after characteristic is registered, can send data by ble
     */
    public  boolean writeable = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_content,container,false);
        initView(v);
        initEvent();
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);

        conversationAdapter = new ListConversationAdapter(getActivity());
        listView.setAdapter(conversationAdapter);
    }

    private void initEvent() {
        btnSend.setOnClickListener(this);
        btnSounds.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                if(writeable) {
                    sendText();
                }
                break;
            case R.id.btn_sounds:
                if(btnSounds.getText().equals(getString(R.string.record_sound))) {
                    if(writeable) {
                        sendSounds();
                    }
                }
                else if(btnSounds.getText().equals(getString(R.string.recording_sound))) {
                    if(callback != null) callback.stopSendSounds();
                    btnSounds.setText(getString(R.string.record_sound));
                }
                break;
            default:
                break;
        }
    }

    public void setCallback(CallbackInHomeFragment callback) {
        this.callback = callback;
    }

    /**
     * send text
     */
    private void sendText() {
        String message = etSendMessage.getText().toString();
        if(!TextUtils.isEmpty(message)) {
            if(callback != null) {
                callback.send(MainActivity.SendAction.TEXT,message);
            }
        }
    }

    /**
     * send sounds
     */
    private void sendSounds() {
        if(callback != null){
            callback.send(MainActivity.SendAction.SOUNDS,null);
        }
    }

    /**
     * is receiving sounds or text data
     * @param tye 0 is sounds data
     *            1 is text data
     */
    public void receivingData(int tye,String data) {
        if(tye == 0) {
            btnSounds.setText(getString(R.string.sounds_im));
            btnSounds.setClickable(false);
        }else if(tye == 1 ){
            ConversationData text = new ConversationData();
            text.setContent(data);
            text.setConversationType(ListConversationAdapter.ConversationType.Other);
            conversationAdapter.addData(text);
        }
    }

    /**
     * after reveive all data
     * @param type 0 is sounds data,
     */
    public void endReceive(int type) {
        if(type == 0) {
            btnSounds.setText(getString(R.string.record_sound));
            btnSounds.setClickable(true);
        }
    }

    /**
     * call if can send text by ble
     * @param sendText the text wait to be send
     */
    public void sendText(String sendText) {
        etSendMessage.setText("");
        ConversationData data = new ConversationData();
        data.setContent(sendText);
        data.setConversationType(ListConversationAdapter.ConversationType.Me);
        conversationAdapter.addData(data);
        data = null;
    }

    public void reset() {
        btnSounds.setText(getString(R.string.record_sound));
        btnSounds.setClickable(true);
        btnSend.setClickable(true);
    }

    /**
     * call when starting recording sounds
     */
    public void startSendingSounds() {
        btnSounds.setText(getString(R.string.recording_sound));
    }

    public interface CallbackInHomeFragment {
        /**
         * send text or sound message
         */
        void send(MainActivity.SendAction sendAction,String text);

        /**
         * stop send sounds
         */
        void stopSendSounds();
    }
}
