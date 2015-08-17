package com.brige.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by rantianhua on 15-5-26.
 */
public class WiFiBroadcastReceiver extends BroadcastReceiver{

    private WifiNetService service;

    public WiFiBroadcastReceiver(WifiNetService service) {
        super();
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiManager.WIFI_STATE_ENABLED) {
                //wifi开启
                service.wifiEnabled();
            }else if(state == WifiManager.WIFI_STATE_DISABLED) {
                //wifi关闭
                service.wifiDisable();
            }
        }else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //Log.e("WiFiBroadcastReceiver", "NETWORK_STATE_CHANGED_ACTION");
            WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if(info != null && info.getSSID() != null && (info.getSSID().startsWith(WifiNetService.WIFI_HOT_HEADER)
                    || info.getSSID().startsWith("\""+WifiNetService.WIFI_HOT_HEADER))) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(networkInfo != null && networkInfo.isConnected()) {
                    String ssid = info.getSSID();
                    if (ssid != null) {
                        ssid = ssid.replaceAll("\"","");
                        service.wifiConnected(ssid);
                    }
                }
            }
        }
    }
}
