package com.brige.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by rantianhua on 15-5-27.
 */
public class WifiNetService extends Service implements Handler.Callback{

    private final String TAG = getClass().getSimpleName();
    private final int port = 4583;  //the port of socket
    public static final String WIFI_HOT_HEADER = "interphone_";
    private String SSID;
    private final String PSD = "interphone_psd";
    private LocalWifiBinder localWifiBinder = new LocalWifiBinder();
    private WifiManager manager;
    private BroadcastReceiver wifiReceiver;
    private CallBack callback;  //the interface communicate with Activity

    private Handler serverAccept;   //open a server socket to wait to accept a client connect
    private ServerSocket serverSocket;  //the serverSocket wait to be connected
    private Runnable startServerSocket;
    private boolean endAccept = false;
    private String connectSsid;

    /**
     * the thread pool to execute mutiClientSocket
     */
    private ExecutorService pool;
    private List<WorkRunnabble> works;

    public class LocalWifiBinder extends Binder{
        public WifiNetService getService() {
            return WifiNetService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        wifiReceiver = new WiFiBroadcastReceiver(this);
        registerReceiver(wifiReceiver,getIntentFilter());

        SSID = WIFI_HOT_HEADER+Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        pool = Executors.newFixedThreadPool(10);
        works = new ArrayList<>();

    }

    /**
     * create wifi hot
     */
    public void startWifiAp() {
        //first disable wifi
        if(manager.isWifiEnabled()){
            manager.setWifiEnabled(false);
        }

        Method method = null;
        try {
            method = manager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            WifiConfiguration configuration = new WifiConfiguration();
            configuration.SSID = SSID;
            configuration.preSharedKey = PSD;

            configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

            method.invoke(manager, configuration, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //check wifiAp is enable or not
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(isWifiApEnabled()) {
                    timer.cancel();
                    callback.wifiApCreated();
                }
            }
        },10,1000);
    }

    public boolean isWifiApEnabled() {
        try {
            Method method = manager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(manager);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void startWifi() {
        if(!manager.isWifiEnabled()) {
            manager.setWifiEnabled(true);
        }
    }

    public void closeWifi() {
        if(manager.isWifiEnabled()) {
            manager.setWifiEnabled(false);
        }
    }

    public void setCallBack(CallBack callback) {
        this.callback = callback;
    }

    public boolean isWifiEnable() {
        return manager.isWifiEnabled();
    }

    public void enableWifi() {
        manager.setWifiEnabled(true);
    }

    public void wifiEnabled() {
        if(callback != null) {
           callback.wifiEnabled();
        }
    }

    public void wifiConnected(String ssid) {
        Log.e("wifiConnected","ssid is" + ssid);
        if(connectSsid != null && connectSsid.equals(ssid)) {
            createClientSocket(ssid);
            connectSsid = null;
        }
    }

    /**
     * connect wifi hot
     * @param ssid the wifiAp's name
     * @return
     */
    public boolean connectAction(String ssid) {
        WifiConfiguration configuration = setUpWifiConfig(ssid);
        if(configuration == null) {
            Log.e("connectWifi","setupWifiConfig failed");
            return false;
        }

        WifiConfiguration tempConfig = isExist(SSID);
        if(tempConfig != null) {
            manager.removeNetwork(tempConfig.networkId);
        }

        int netId = manager.addNetwork(configuration);
        //disconnect exist connect
        manager.disconnect();
        manager.enableNetwork(netId,true);
        connectSsid = ssid;
        return manager.reconnect();
    }

    /**
     *
     * @param ssid the wifiAp's name
     * @return true if the hot have connected,else false
     */
    public boolean ssidConnected(String ssid) {
        WifiInfo info = manager.getConnectionInfo();
        if(info != null) {
            String connectSsid = info.getSSID();
            if(connectSsid != null && (ssid.equalsIgnoreCase(connectSsid)
                    || connectSsid.equalsIgnoreCase("\""+ssid+"\""))) {
                Log.e(TAG,"已连接的ssid是"+ info.getSSID());
                return true;
            }
        }
        return false;
    }

    /**
     * @param ssid the wifiAp's name
     * @return the existed WifiConfiguration
     */
    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> existingConfigs = manager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * @param ssid  the wifiAp's name
     * @return WifiConfiguration
     */
    private WifiConfiguration setUpWifiConfig(String ssid) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();
        configuration.SSID = "\"" + ssid + "\"";

        configuration.preSharedKey = "\"" + PSD + "\"";
        configuration.hiddenSSID = true;
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        return configuration;
    }

    /**
     * open a serverSocket to wait connect
     */
    public void openServer() {
        HandlerThread server = new HandlerThread("server");
        server.start();
        serverAccept = new Handler(server.getLooper(),this);
        startServerSocket = new OpenServerSocket();
        serverAccept.post(startServerSocket);
    }

    /**
     * statrt scan wifi device
     */
    public void startScan() {
        manager.startScan();
    }

    public List<ScanResult> getScanResults() {
        return manager.getScanResults();
    }


    /**
     * create client socket to exchange data
     */
    public void createClientSocket(String ssid) {
        new ClientConnect().execute(ssid);
    }


    private synchronized void setEndAccept(boolean end) {
        endAccept = end;
    }

    private synchronized boolean getEndAccept() {
        return endAccept;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver);

        if(out != null) {
            try {
                out.close();
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                out = null;
            }
        }

        closeWorks();

        if(serverAccept != null) {
            setEndAccept(true);
            //close Looper Thread
            closeServerSocket();
            serverAccept.removeCallbacks(startServerSocket);
            serverAccept.getLooper().quit();
        }

        pool.shutdown();
        try {
            if(!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            pool.shutdownNow();
        }finally {
            pool = null;
        }
        wifiReceiver = null;
        manager = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localWifiBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        localWifiBinder = null;
        return super.onUnbind(intent);
    }

    private IntentFilter getIntentFilter() {
        //create an intentfilter for broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        return intentFilter;
    }

    private class ClientConnect extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String host = getServerIpAdress();
            try {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(host, port));
                WorkRunnabble work = new WorkRunnabble(socket,strings[0]);
                works.add(work);
                pool.execute(work);
            } catch (IOException e) {
                Log.e(TAG, "client socket connect error" + e.getMessage(), e);
                return null;
            }
            host = null;
            return strings[0];
        }

        @Override
        protected void onPostExecute(String ssid) {
            callback.socketCreated(ssid);
        }

        /**
         *
         * @return the ipAddress of WifiAp
         */
        private String getServerIpAdress() {
            DhcpInfo info = manager.getDhcpInfo();
            return intToStringIp(info.serverAddress);
        }

        private String intToStringIp(int i) {
            return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
                    + ((i >> 24) & 0xFF);
        }
    }

    public void closeWifiAp() {
        if (isWifiApEnabled()) {
            try {
                Method method = manager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(manager);
                Method method2 = manager.getClass().getMethod("setWifiApEnabled",
                        WifiConfiguration.class, boolean.class);
                method2.invoke(manager, config, false);
            }
           catch (Exception e) {
               e.printStackTrace();
           }
        }
    }

    /**
     * @param hello the data to be written to socket
     */
    private OutputStream out;


    /**
     *
     * @param message
     * @return
     * this callback is not run in main thread
     */
    @Override
    public boolean handleMessage(Message message) {
       switch (message.what) {
       }
        message.recycle();
        return true;
    }


    private void closeWorks() {
        Log.e(TAG,"close works");
        for(int i  = 0;i < works.size(); i ++) {
            works.get(i).cancel();
        }
        works.clear();
        works = null;
    }

    /**
     * create a runnable to read and write from socket
     */
    class WorkRunnabble implements Runnable {

        private OutputStream out = null;
        private InputStream in = null;
        private String ssid;
        private Socket socket = null;
        private boolean reading = false;

        public WorkRunnabble(Socket socket,String ssid) {
            this.ssid = ssid;
            this.socket = socket;
            try {
                out = socket.getOutputStream();
                in = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            setReading(true);
            try {
                int bytes = 0;
                byte[] buff = new byte[1024];
                while (getReading()) {
                    bytes = in.read(buff);
                    if(bytes > 0) {
                        byte[] data = new byte[bytes];
                        System.arraycopy(buff,0,data,0,bytes);
                        callback.readData(new String(data));
                    }
                }
            }catch (Exception e) {
                Log.e(TAG,"error in read from socket " + e.getMessage(),e);
            }finally {
                cancel();
            }
        }

        public void writeMessage(String hello) {
            try{
                if(out != null) {
                    out.write(hello.getBytes());
                    out.flush();
                }
            }catch (Exception e) {
                Log.e(TAG,"error in writeMessage " + e.getMessage(),e);
                cancel();
            }
        }

        public void cancel() {
            setReading(false);
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    in  = null;
                }
            }
            if(out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    out  = null;
                }
            }
            if(socket != null) {
                try {
                    socket.close();
                    socket = null;
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //cleanWorks();
        }

        public synchronized void setReading(boolean reading) {
            this.reading = reading;
        }

        public synchronized boolean getReading(){
            return this.reading;
        }

        public String getSsid() {
            return this.ssid;
        }
    }

    /**
     * clean the dead runnable in works
     */
    private void cleanWorks() {
        for(int i= 0;i < works.size();i ++) {
            if(!works.get(i).reading) {
                works.remove(i);
                break;
            }
        }
    }

    /**
     *  create and open a server socket
     */
    class OpenServerSocket implements Runnable {

        @Override
        public void run() {
            try{
                serverSocket = new ServerSocket(port);
                while (!getEndAccept()) {
                    Log.e(TAG, "waiting accept ...");
                    Socket socket  = serverSocket.accept();
                    Log.e(TAG, "connected  ...");
                    WorkRunnabble work = new WorkRunnabble(socket,null);
                    works.add(work);
                    pool.execute(work);
                    work.writeMessage("hello");
                }
            }catch (Exception e) {
                Log.e(TAG,"error in openServerSocket" + e.getMessage(),e);
            }finally {
                closeServerSocket();
            }
        }
    }

    private void closeServerSocket() {
        if(serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                serverSocket = null;
            }
        }
    }

    public interface CallBack {
        /**
         * the wifiAp have created
         */
        void wifiApCreated();

        /**
         *
         * @param data read from socket
         */
        void readData(String data);

        /**
         * the wifi have opened
         */
        void wifiEnabled();

        /**
         *
         * @param ssid note the main thread the socket establish successful or not
         */
        void socketCreated(String ssid);
    }

}
