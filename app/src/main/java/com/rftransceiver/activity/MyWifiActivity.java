package com.rftransceiver.activity;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.Constants;

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
import java.util.Random;

/**
 * Created by rantianhua on 15-5-26.
 */
public class MyWifiActivity extends ListActivity{

    private final String TAG = getClass().getSimpleName();

    private WifiDevicesAdapter adapter;

    public static WifiNetService service;
    private byte[] asyncWord;
    GroupEntity entity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        String name = getIntent().getStringExtra("name");
        setTitle(name);

        adapter = new WifiDevicesAdapter();
        setListAdapter(adapter);

        entity = new GroupEntity(name, getAsyncWord());

        try {
            setTitle(getLocalIPAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取本地局域网IP
    private String getLocalIPAddress()
    {
        String ipaddress;
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                NetworkInterface nif = en.nextElement();// 得到每一个网络接口绑定的所有ip
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                // 遍历每一个接口绑定的所有ip
                while (inet.hasMoreElements())
                {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress()&& InetAddressUtils.isIPv4Address(ip.getHostAddress()))
                    {
                        ipaddress=ip.getHostAddress();
                        return ipaddress;
                    }
                }
            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
        adapter.clear();
        adapter = null;
        setListAdapter(null);
        service = null;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
//        WifiP2pDevice device = adapter.getDevice(position);
//        if(device == null) return;
//        WifiP2pConfig config = new WifiP2pConfig();
//        config.deviceAddress = device.deviceAddress;
//        wifiNetService.connectDevice(config,device);
//        config = null;
    }

    public byte[] getAsyncWord() {
        Random random = new Random(System.currentTimeMillis());
        byte[] word = new byte[2];
        random.nextBytes(word);

        random = null;
        return word;
    }


    // Adapter for holding devices found through discovering.
    private class WifiDevicesAdapter extends BaseAdapter {
        private ArrayList<WifiP2pDevice> devices;
        private LayoutInflater mInflator;

        public WifiDevicesAdapter() {
            super();
            devices = new ArrayList<>();
            mInflator = MyWifiActivity.this.getLayoutInflater();
        }

        public void addDevice(WifiP2pDevice device) {
            Log.e("WifiDevicesAdapter","addDevice");
            if(!devices.contains(device)) {
                devices.add(device);
            }
        }

        public WifiP2pDevice getDevice(int position) {
            return devices.get(position);
        }

        public void clear() {
            devices.clear();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            WifiP2pDevice device = devices.get(i);
            final String deviceName = device.deviceName;
            if (!TextUtils.isEmpty(deviceName))
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.deviceAddress);

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
