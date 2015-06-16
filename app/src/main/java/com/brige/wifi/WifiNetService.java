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
import android.text.TextUtils;
import android.util.Log;


import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;

import org.json.JSONArray;
import org.json.JSONObject;

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
        if(callback != null) {
            callback.wifiApConnected(ssid);
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
     * write member info to group owner
     * @param s
     */
    public void writeMemberInfo(final String s) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < works.size(); i++) {
                    WorkRunnabble avliableWork = works.get(i);
                    if (avliableWork.getReading()) {
                        avliableWork.writeMessage(s);
                        break;
                    }
                }
            }
        });
    }

    /**
     * cancel add group , send message to wifiAp
     * @param id
     */
    public void cancelAddGroup(final int id) {
        for(int i = 0; i < works.size();i++) {
            WorkRunnabble workRunnable = works.get(i);
            if(workRunnable.getReading()) {
                workRunnable.writeMessage(GroupUtil.getWriteData(GroupUtil.CANCEL_ADD,
                        id,null));
                workRunnable.cancel();
                break;
            }
        }
    }

    /**
     * cancel create group
     */
    public void cancelCreateGroup() {
        for(int i = 0;i < works.size();i++) {
            WorkRunnabble run = works.get(i);
            if(run.getReading()) {
                run.writeMessage(GroupUtil.getWriteData(GroupUtil.CANCEL_CREATE,
                        SSID,null));
            }
        }
    }

    //send group full members to every member
    public void sendFullGroup(final GroupEntity groupEntity) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                String groupInfo = GroupUtil.getWriteData(GroupUtil.GROUP_FULL_INFO,
                        groupEntity,WifiNetService.this.getApplicationContext());
                if(groupInfo != null) {
                    for(int i = 0;i < works.size();i++ ){
                        WorkRunnabble run = works.get(i);
                        if(run.getReading()) {
                            run.writeMessage(groupInfo);
                        }
                    }
                }
                callback.sendGroupFinished();
            }
        });
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
    public void createClientSocket(String ssid,boolean requestGroupInfo) {
        String request = requestGroupInfo ? "request" : "";
        new ClientConnect().execute(ssid,request);
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
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return intentFilter;
    }

    private class ClientConnect extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            //before connect, close have established connection
            if(works.size() > 0) {
                final WorkRunnabble preWork = works.get(0);
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        preWork.writeMessage(GroupUtil.getWriteData(GroupUtil.CLOSE_SOCKET,
                                null,null));
                        preWork.cancel();
                    }
                });
            }
            String host = getServerIpAdress();
            try {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(host, port));
                final WorkRunnabble work = new WorkRunnabble(socket,-1);
                works.add(work);
                pool.execute(work);
                if(!TextUtils.isEmpty(strings[1])) {
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            work.writeMessage(GroupUtil.getWriteData(GroupUtil.REQUEST_GBI,
                                    null, null));
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "client socket connect error" + e.getMessage(), e);
                if(callback != null){
                    callback.socketConnectFailed(strings[0]);
                }
                return null;
            }
            host = null;
            return strings[0];
        }

        @Override
        protected void onPostExecute(String ssid) {
            if(ssid != null && callback != null) {
                callback.socketCreated(ssid);
            }
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
        private Socket socket = null;
        private boolean reading = false;

        /**
         * every member have a id as an identity
         */
        private int id;

        public WorkRunnabble(Socket socket,int id) {
            this.id = id;
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
            boolean receving = false;
            try {
                int bytes = 0;
                byte[] buff = new byte[1024];
                StringBuilder sb = new StringBuilder();
                while (getReading()) {
                    bytes = in.read(buff);
                    if(bytes > 0) {
                        byte[] data = new byte[bytes];
                        System.arraycopy(buff,0,data,0,bytes);
                        if(!receving && data[0] == Constants.Data_Packet_Head) {
                            //start receiving data
                            receving = true;
                            if(bytes < 1024 && data[data.length-1] == Constants.Data_Packet_Tail) {
                                //callback.readData(sb.toString());
                                //do parse data in other thread , avoid blocking read data
                                receving = false;
                                sb.append(new String(data,1,bytes-2));
                                final String temp = sb.toString();
                                sb.delete(0,sb.length());
                                pool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        parseReadData(temp);
                                    }
                                });
                            }else {
                                sb.append(new String(data,1,data.length-1));
                            }
                        }else if(receving && data[data.length-1] == Constants.Data_Packet_Tail) {
                            //end receiving data
                            receving = false;
                            sb.append(new String(data,0,data.length-1));
                            //callback.readData(sb.toString());
                            //do parse data in other thread , avoid blocking read data
                            final  String temp = sb.toString();
                            pool.execute(new Runnable() {
                                @Override
                                public void run() {
                                    parseReadData(temp);
                                }
                            });
                            sb.delete(0,sb.length());
                        }else {
                            sb.append(new String(data,0,data.length));
                        }
                        data = null;
                    }
                }
            }catch (Exception e) {
                Log.e(TAG,"error in read from socket " + e.getMessage(),e);
            }finally {
                cancel();
            }
        }

        /**
         * @param data received data
         */
        private void parseReadData(String data) {
            JSONObject object = null;
            try{
                object = new JSONObject(data);
                Log.e("readData", "have read some data" + object);
                String type = object.getString("msg");
                switch (type) {
                    case GroupUtil.GROUP_BASEINFO:
                        //received group base info
                        if(callback == null) return;
                        callback.readData(GroupUtil.GROUP_BASEINFO,object);
                        break;
                    case GroupUtil.REQUEST_GBI:
                        //need send group owner info to connected device
                        if(callback == null) return;
                        String info = callback.getGroupInfo(getId());
                        if(info == null) return;
                        writeMessage(info);
                        break;
                    case GroupUtil.CLOSE_SOCKET:
                        cancel();
                        break;
                    case GroupUtil.MEMBER_BASEINFO:
                        if(callback == null) return;
                        object.put(GroupUtil.GROUP_MEMBER_ID,getId());
                        callback.readData(GroupUtil.MEMBER_BASEINFO,object);
                        break;
                    case GroupUtil.CANCEL_ADD:
                        if(callback == null) return;
                        callback.readData(GroupUtil.CANCEL_ADD,object);
                        int closeId = -1;
                        closeId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                        if(closeId != -1) {
                            WorkRunnabble closeRunnable = works.get(closeId-1);
                            if(closeRunnable.getId() == closeId) {
                                closeRunnable.cancel();
                                Log.e("CANCEL_ADD","cancel a member");
                            }
                        }
                        break;
                    case GroupUtil.CANCEL_CREATE:
                        if(callback == null ) return;
                        callback.readData(GroupUtil.CANCEL_CREATE,object);
                        break;
                    default:
                        callback.readData(GroupUtil.GROUP_FULL_INFO,object);
                        break;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(object != null) {
                    object = null;
                }
            }
        }

        public synchronized void writeMessage(String hello) {
            try{
                if(out != null && hello != null) {
                    Log.e("have write",hello);
                    byte[] data = hello.getBytes();
                    byte[] sendData = new byte[data.length+2];
                    sendData[0] = Constants.Data_Packet_Head;
                    sendData[sendData.length-1] = Constants.Data_Packet_Tail;
                    System.arraycopy(data,0,sendData,1,data.length);
                    out.write(sendData,0,sendData.length);
                    out.flush();
                    data = null;
                    sendData = null;
                    hello = null;
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

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
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
                    WorkRunnabble work = new WorkRunnabble(socket,works.size()+1);
                    works.add(work);
                    pool.execute(work);
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
         * @param type the type of the read data
         * @param data read from socket
         */
        void readData(String type,JSONObject data);

        /**
         * the wifi have opened
         */
        void wifiEnabled();

        /**
         *
         * @param ssid note the main thread the socket establish successful or not
         */
        void socketCreated(String ssid);
        /**
         * get group base info
         */
        String getGroupInfo(int memberId);

        /**
         * @param ssid the connected wifiAp's ssid
         */
        void wifiApConnected(String ssid);

        /**
         * call after socket connect failed
         * @param ssid
         */
        void socketConnectFailed(String ssid);

        void sendGroupFinished();
    }

}
