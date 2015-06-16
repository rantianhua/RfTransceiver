package com.rftransceiver.util;

import android.net.wifi.ScanResult;

/**
 * Created by rantianhua on 15-6-15.
 */
public class StateScanResult {

    /**
     * searched wifiAp
     */
    private ScanResult scanResult;

    /**
     * note this scanResult have connected at most once
     */
    private boolean haveConnected = false;

    /**
     * note is connecting this scanResult
     */
    private boolean connecting = false;

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    public boolean isHaveConnected() {
        return haveConnected;
    }

    public void setHaveConnected(boolean haveConnected) {
        this.haveConnected = haveConnected;
    }
}
