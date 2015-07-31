package com.brige.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
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
import android.view.WindowManager;


import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;
import com.rftransceiver.util.PoolThreadUtil;

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
    private final int port = 4583;  //tsocket的端口
    public static final String WIFI_HOT_HEADER = "interphone_";    //wifi热点名的前缀
    private String SSID;    //wifi的热点名
    private final String PSD = "interphone_psd";    //wifi热点密码
    private LocalWifiBinder localWifiBinder = new LocalWifiBinder();    //用来传递WifiNetService实例
    private WifiManager manager;    //wifi管理者
    private BroadcastReceiver wifiReceiver; //wifi状态的广播接收器
    private CallBack callback;  //回调接口

    private Handler serverAccept;   //开启工作线程用来监听socket连接
    private ServerSocket serverSocket;  //socket服务端
    private Runnable startServerSocket; //开启socket服务端的runnable对象
    private boolean endAccept = false;  //接收客户端连接的标识
    private PoolThreadUtil poolThreadUtil;  //线程池
    private List<WorkRunnabble> works;  //记录所有socket的工作线程
    private boolean needStartAp = false;    //标识需要开启wifi热点的

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

        poolThreadUtil = PoolThreadUtil.getInstance();
        works = new ArrayList<>();
    }

    /**
     * wifi关闭
     */
    public void wifiDisable() {
        Log.e("wifiDisable", "有连接关闭le");
        if(needStartAp) {
            needStartAp = false;
            //重新开启wifi热点
            startWifiAp(false);
        }
    }
    /**
     * 开启wifi热点
     * @param checkWifi 标识要不要检查wifi
     */
    public void startWifiAp(boolean checkWifi) {
        if(checkWifi) {
            //如果wifi开着，先关闭wifi
            if(manager.isWifiEnabled()){
                needStartAp = true;
                manager.setWifiEnabled(false);
                return;
            }
        }

        if(isWifiApEnabled()) {
            String openSSID = getWifiApName();
            if(openSSID != null && compareTwoSsid(openSSID,SSID)) {
                //热点已开启
                return;
            }
            //关闭重新配置热点
            closeWifiAp();
            if(Constants.DEBUG) {
                Log.e("startWifiAp","重新开启热点");
            }
            try {
                Thread.sleep(100);
            }catch (Exception e) {
                e.fillInStackTrace();
            }
        }
        //通过反射开启wifi
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

        //检查wifi热点是否开启
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isWifiApEnabled()) {
                    timer.cancel();
                    if (callback != null) {
                        callback.wifiApCreated();
                    }
                }
            }
        }, 0, 1000);
    }

    /**
     * 比较两个热点名是否相同
     * @param ssid1
     * @param ssid2
     * @return
     */
    public static boolean compareTwoSsid(String ssid1,String ssid2) {
        //有的设备有双引号
        ssid1 = ssid1.replaceAll("\"","");
        ssid2 = ssid2.replaceAll("\"", "");
        if(ssid1.equalsIgnoreCase(ssid2)) {
            return true;
        }
        return false;
    }

    /**
     * 检查ssid的有效性
     * @param ssid
     * @return
     */
    public static boolean isSSIDValid(String ssid) {
        ssid = ssid.replaceAll("\"", "");
        ssid = ssid.toLowerCase();
        if(ssid.startsWith(WIFI_HOT_HEADER)) {
            return true;
        }
        return false;
    }

    //检查热点是否已经开启
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

    //获取当前热点的名称
    public String getWifiApName() {
        try {
            Method method = manager.getClass().getMethod(
                    "getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration)method.invoke(manager);
            if(configuration != null) {
                return configuration.SSID;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //开启wifi
    public boolean startWifi() {
        //如果热点是开着的，先关闭热点
        if(isWifiApEnabled()) {
            closeWifiAp();
            try {
                Thread.sleep(50);
            }catch (Exception e) {

            }
        }
        if(isWifiEnable()) {
            return false;
        }else {
            manager.setWifiEnabled(true);
            return true;
        }
    }

    public void closeWifi() {
        if(manager.isWifiEnabled()) {
            manager.setWifiEnabled(false);
        }
    }

    //设置接口
    public void setCallBack(CallBack callback) {
        this.callback = callback;
    }

    //检查wifi是否已开启
    public boolean isWifiEnable() {
        return manager.isWifiEnabled();
    }

    //开启wifi
    public void enableWifi() {
        manager.setWifiEnabled(true);
    }

    //wifi已经开启
    public void wifiEnabled() {
        if(callback != null) {
            //通知接口调用方
           callback.wifiEnabled();
        }
    }

    public void wifiConnected(String ssid) {
        if(callback != null) {
            callback.wifiApConnected(ssid);
        }
    }

    /**
     * 连接wifi热点
     * @param ssid 热点名称
     * @return
     */
    public boolean connectAction(String ssid) {
        WifiConfiguration configuration = null;
        //检查是否已经有连接过该热点
        WifiConfiguration tempConfig = isExist(SSID);
        if(tempConfig != null) {
            //连接过，直接连接
            if(Constants.DEBUG) {
                Log.e("connectAction","之前连接过");
            }
            configuration = tempConfig;
        }else {
            configuration = setUpWifiConfig(ssid);
        }
        if(configuration == null) {
            return false;
        }
        //检查是否已有wifi连接上，有则断开连接，
        isWifiConneted(true);
        int netId = manager.addNetwork(configuration);
        manager.enableNetwork(netId, true);
        manager.reassociate();
        return true;
    }

    /**
     *
     * @param ssid 要连接的wifi热点的名称
     * @return true 如果检查出该热点已经连接上了
     */
    public boolean ssidConnected(String ssid) {
        String connectedSSID = isWifiConneted(false);
        if(!TextUtils.isEmpty(connectedSSID)) {
            return compareTwoSsid(ssid,connectedSSID);
        }
        return false;
    }

    /**
     * 检查wifi是否已有连接
     * @param disConnect    //是否断开该连接
     * @return
     */
    private String isWifiConneted(boolean disConnect) {
        WifiInfo info = manager.getConnectionInfo();
        if(info != null) {
            String ssid = info.getSSID();
            if(disConnect) {
                //关闭该连接
                List<WifiConfiguration> configurations = manager.getConfiguredNetworks();
                for(WifiConfiguration con : configurations) {
                    if(compareTwoSsid(ssid,con.SSID)) {
                        //关闭该wifi
                        int netId = con.networkId;
                        manager.disableNetwork(netId);
                        manager.disconnect();
                        break;
                    }
                }
                return null;
            }else {
                return ssid;
            }
        }else {
            return null;
        }
    }

    /**
     * @param ssid 热点名称
     * @return 存在的热点配置
     */
    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> existingConfigs = manager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (compareTwoSsid(existingConfig.SSID,ssid)) {
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
     * 开启服务端socket等待客户端连接
     */
    public void openServer() {
        //开启一个新得线程监听连接
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
        poolThreadUtil.addTask(new Runnable() {
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
     * 取消建组
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

    //向所有的成员发送组的详细信息
    private int activeMembers = 0;  //记录向多少个成员发送了信息
    public void sendFullGroup(final GroupEntity groupEntity) {
        String groupInfo = GroupUtil.getWriteData(GroupUtil.GROUP_FULL_INFO,
                groupEntity,WifiNetService.this.getApplicationContext());
        if(groupInfo != null) {
            for(int i = 0;i < works.size();i++ ){
                WorkRunnabble run = works.get(i);
                if(run.getReading()) {
                    run.writeMessage(groupInfo);
                    activeMembers++;
                }
            }
        }
        if(Constants.DEBUG) {
            Log.e("sendFullGroup","向"+activeMembers+"个成员发送了信息");
        }
        callback.sendGroupFinished(activeMembers);
        activeMembers = 0;
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
     *  建立一个socket连接与指定的设备
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

        poolThreadUtil = null;
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
            //连接前先关闭已有的连接，否则端口被占用
            if(works.size() > 0) {
                final WorkRunnabble preWork = works.get(0);
                //通知服务端关闭对应的socket和work thread
                preWork.writeMessage(GroupUtil.getWriteData(GroupUtil.CLOSE_SOCKET,
                        null,null));
                preWork.cancel();
            }
            String host = getServerIpAdress();
            if(Constants.DEBUG) {
                Log.e("ClientConnect","服务端ip 是"+host);
            }
            try {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(host, port));
                Log.e("ClientConnect", "客户端连接成功");
                final WorkRunnabble work = new WorkRunnabble(socket,-1);
                works.add(work);
                poolThreadUtil.addTask(work);
                if(!TextUtils.isEmpty(strings[1])) {
                    //请求群主的基本信息
                    work.writeMessage(GroupUtil.getWriteData(GroupUtil.REQUEST_GBI,
                            null, null));
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
         * @return 得到服务端的ip地址
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

    //关闭wifi热点
    public void closeWifiAp() {
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
        for(int i  = 0;i < works.size(); i ++) {
            works.get(i).cancel();
        }
        works.clear();
        works = null;
    }

    /**
     * socket连接建立后的工作线程
     */
    class WorkRunnabble implements Runnable {

        private OutputStream out = null;
        private InputStream in = null;
        private Socket socket = null;
        private boolean reading = false;

        /**
         * 每一个组成员的id
         */
        private int id;

        public WorkRunnabble(Socket socket,int id) {
            setId(id);
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
            boolean receving = false;   //用于标识开始接收一段数据
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
                            //开始接收一段数据
                            receving = true;
                            if(bytes < 1024 && data[data.length-1] == Constants.Data_Packet_Tail) {
                                //数据接收完毕
                                receving = false;
                                //得到收到的字符转，转化前去掉数据包的头和尾
                                sb.append(new String(data,1,bytes-2));
                                final String temp = sb.toString();
                                sb.delete(0,sb.length());
                                poolThreadUtil.addTask(new Runnable() {
                                    @Override
                                    public void run() {
                                        //解析接收到的数据
                                        parseReadData(temp);
                                    }
                                });
                            }else {
                                //数据未接收完毕，继续缓存
                                sb.append(new String(data,1,data.length-1));
                            }
                        }else if(receving && data[data.length-1] == Constants.Data_Packet_Tail) {
                            //数据接收完毕
                            receving = false;
                            sb.append(new String(data,0,data.length-1));
                            //解析数据
                            final  String temp = sb.toString();
                            poolThreadUtil.addTask(new Runnable() {
                                @Override
                                public void run() {
                                    parseReadData(temp);
                                }
                            });
                            sb.delete(0,sb.length());
                        }else {
                            //继续缓存数据
                            sb.append(new String(data,0,data.length));
                        }
                        data = null;
                    }
                }
            }catch (Exception e) {
                Log.e(TAG, "error in read from socket " + e.getMessage(), e);
            }finally {
                cancel();
            }
        }

        /**
         * 根据收到的数据判断接下来的动作
         * @param data received data
         */
        private void parseReadData(String data) {
            JSONObject object = null;
            if(callback == null) return;
            try{
                //将字符串转换为Json
                object = new JSONObject(data);
                if(Constants.DEBUG) {
                    Log.e("readData", "have read some data" + object);
                }
                String type = object.getString("msg");
                switch (type) {
                    case GroupUtil.GROUP_BASEINFO:
                        //组员接收到一个群主的信息
                        callback.readData(GroupUtil.GROUP_BASEINFO,object);
                        break;
                    case GroupUtil.REQUEST_GBI:
                        //群主接收到一个组员的请求，要发送自己的信息给他
                        String info = callback.getGroupInfo(getId());
                        if(info == null) return;
                        //发送自己的信息
                        writeMessage(info);
                        break;
                    case GroupUtil.CLOSE_SOCKET:
                        //建组完毕或者群主取消了建组，则关闭所有的socket
                        cancel();
                        break;
                    case GroupUtil.MEMBER_BASEINFO:
                        //群主接收到一个组员的信息
                        object.put(GroupUtil.GROUP_MEMBER_ID,getId());
                        callback.readData(GroupUtil.MEMBER_BASEINFO,object);
                        break;
                    case GroupUtil.CANCEL_ADD:
                        //群主收到一个组成员取消加组的请求
                        callback.readData(GroupUtil.CANCEL_ADD,object);
                        int closeId = -1;   //该成员的id
                        closeId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                        if(closeId != -1) {
                            WorkRunnabble closeRunnable = works.get(closeId-1);
                            if(closeRunnable.getId() == closeId) {
                                closeRunnable.cancel();
                                if(Constants.DEBUG) {
                                    Log.e("CANCEL_ADD","cancel a member");
                                }
                            }
                        }
                        break;
                    case GroupUtil.CANCEL_CREATE:
                        //群成员接收到群主撤销建组的请求
                        callback.readData(GroupUtil.CANCEL_CREATE,object);
                        break;
                    case GroupUtil.RECEIVE_GROUP:
                        //接收到一个成员收到整个群组消息的反馈
                        callback.readData(GroupUtil.RECEIVE_GROUP,object);
                        break;
                    default:
                        //告诉群组已收到整个组的信息
                        int mid = callback.getIdInCurrentGroup();
                        writeMessage(GroupUtil.getWriteData(GroupUtil.RECEIVE_GROUP,mid,null));
                        //群成员接收到群主发送的所有组成员的信息
                        callback.readData(GroupUtil.GROUP_FULL_INFO, object);
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

        public synchronized int getId() {
            return id;
        }

        public synchronized void setId(int id) {
            this.id = id;
        }
    }

    /**
     *  开启服务端socket
     */
    class OpenServerSocket implements Runnable {

        @Override
        public void run() {
            try{
                serverSocket = new ServerSocket(port);
                while (!getEndAccept()) {
                    Socket socket  = serverSocket.accept();
                    WorkRunnabble work = new WorkRunnabble(socket,works.size()+1);
                    works.add(work);
                    poolThreadUtil.addTask(work);
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

        /**
         * 通知接口调用者群组信息已发送完毕
         * @param counts 发送给了多少个成员
         */
        void sendGroupFinished(int counts);

        /**
         * 组员取得自己在当前组的id
         * @return
         */
        int getIdInCurrentGroup();

    }

}
