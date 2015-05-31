package com.brige.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.rftransceiver.activity.MyWifiActivity;

/**
 * Created by rantianhua on 15-5-26.
 */
public class WiFiDirectBroadcastReceiver  extends BroadcastReceiver{

    private WifiNetService service;

    public WiFiDirectBroadcastReceiver (WifiNetService service) {
        super();
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            //check to see wifi is enable and notify appropriate
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //wifiP2p is enable

            }else {
                //wifiP2p is not enable
            }
        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //can call requestPeers() to get a list of current peers
            service.getPeersList();
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //have a new connection or disconnections
            NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            String name = info.getState().name();
            if(info.isConnected()) {
                service.requestConnection();
            }
            info = null;
        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //response to this device's wifi state changing
            int i = 0;
        }
    }
}
