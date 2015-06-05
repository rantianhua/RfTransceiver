package com.rftransceiver.activity;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.wifi.WifiNetService;
import com.rftransceiver.R;
import com.rftransceiver.fragments.AddGroupFragment;
import com.rftransceiver.fragments.CreateGroupFragment;
import com.rftransceiver.group.BaseGroupFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by rantianhua on 15-5-26.
 */
public class MyWifiActivity extends Activity implements ServiceConnection,WifiNetService.CallBack
       ,BaseGroupFragment.Callback{

    private final String TAG = getClass().getSimpleName();
    public WifiNetService service;
    private byte[] asyncWord;

    private String action;

    public static final String CREATE_GROUP = "create_group";
    public static final String ADD_GROUP = "add_group";
    public static final String ACTION = "action";

    private String ssid = null;
    private AddGroupFragment addGroupFragment;
    private CreateGroupFragment createGroupFragment;
    private GroupEntity groupEntity;

    private boolean scanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        action = getIntent().getStringExtra(ACTION);
        if(action.equals(ADD_GROUP)) {
            addGroupFragment = new AddGroupFragment();
            addGroupFragment.setCallback(this);
        }else {
            createGroupFragment = new CreateGroupFragment();
            createGroupFragment.setCallback(this);
        }
        getFragmentManager().beginTransaction()
                .add(R.id.rl_container_group,
                        addGroupFragment == null ? createGroupFragment : addGroupFragment)
                .commit();

        groupEntity = new GroupEntity();
        bindService(new Intent(MyWifiActivity.this, WifiNetService.class),
                MyWifiActivity.this, BIND_AUTO_CREATE);
//        if(action.equals(ADD_GROUP)) {
//            bindService(new Intent(MyWifiActivity.this, WifiNetService.class),
//                    MyWifiActivity.this, BIND_AUTO_CREATE);
//        }else {
//            setGroupName();
//        }
    }

    private void setGroupName() {
        final EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("set group name")
                .setView(editText)
                .setPositiveButton("sure", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = editText.getText().toString();
                        if (!TextUtils.isEmpty(name)) {
                            groupEntity.setName(name);
                            groupEntity.setAsyncWord(GroupUtil.createAsynWord());
                            bindService(new Intent(MyWifiActivity.this, WifiNetService.class),
                                    MyWifiActivity.this, BIND_AUTO_CREATE);
                        } else {
                            //dialogInterface.wait();
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }

    public void showTost(final String message) {
       runOnUiThread(new Runnable() {
           @Override
           public void run() {
               Toast.makeText(MyWifiActivity.this,message,Toast.LENGTH_SHORT).show();
           }
       });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_wifi,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(action.equals(CREATE_GROUP)) {
            service.closeWifiAp();
        }else if(action.equals(ADD_GROUP)) {
            service.closeWifi();
        }
        unbindService(this);
    }

    @Override
    public List<ScanResult> getScanResults() {
        service.startScan();
        return service.getScanResults();
    }

    /**
     *
     * @param result the wifiAp
     *  as long as discovery a hot ,connect it and establish socket
     *               then get the group data from socket
     */
    @Override
    public void getGroupData(ScanResult result) {
        if(service.isWifiEnable()) {
            if(service.ssidConnected(result.SSID)) {
                service.createClientSocket(result.SSID);
            }else {
                service.connectAction(result.SSID);
            }
        }else {
            ssid = result.SSID;
            service.enableWifi();
        }
    }

    public byte[] getAsyncWord() {
        Random random = new Random(System.currentTimeMillis());
        byte[] word = new byte[2];
        random.nextBytes(word);

        random = null;
        return word;
    }

    @Override
    public void wifiApCreated() {
        service.openServer();
    }

    @Override
    public void wifiEnabled() {
        if(!scanning) {
            service.startScan();
            scanning = true;
            addGroupFragment.startSearch();
            return;
        }
        if(ssid != null) {
            service.connectAction(ssid);
            ssid = null;
        }
    }

    @Override
    public void readData(String data) {
        showTost(data);
        if(data.equals("hello")) {
            addGroupFragment.getAgroupInfo();
        }
    }

    @Override
    public void socketCreated(String ssid) {
        if(ssid != null) {
            showTost("can write and read data");
            //addGroupFragment.socketCreated(ssid);
        }else {
            showTost("socket establish failed");
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if(iBinder != null) {
            WifiNetService.LocalWifiBinder binder = (WifiNetService.LocalWifiBinder)iBinder;
            service = binder.getService();
            service.setCallBack(this);
            if(action.equals(CREATE_GROUP)) {
                service.startWifiAp();
            }else if(action.equals(ADD_GROUP)) {
                service.startWifi();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service.setCallBack(null);
        service = null;
    }


}
