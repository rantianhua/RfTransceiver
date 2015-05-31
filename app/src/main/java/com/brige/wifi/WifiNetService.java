package com.brige.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by rantianhua on 15-5-27.
 */
public class WifiNetService extends Service implements WifiP2pManager.PeerListListener
        ,WifiP2pManager.ConnectionInfoListener,Handler.Callback{

    private final String TAG = getClass().getSimpleName();
    private final int port = 4583;  //the port of socket
    private LocalWifiBinder localWifiBinder = new LocalWifiBinder();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver wifiReceiver;
    private CallBack callback;  //the interface communicate with Activity

    private Handler serverAccept;   //open a server socket to wait to accept a client connect
    //private Handler clientConnect;  //create a client socket to connect
    private Handler work;   //create a work thread for send data
    private Socket clientSocket;    //the socket to read and write
    private ServerSocket serverSocket;  //the server socket waiting to be connected
    private WifiP2pDevice connectDevice;    //the device to connect
    private volatile boolean endAccept  = false;
    private boolean workStart = false;
    private volatile  boolean reading = false;

    private Runnable startServerSocket;
    private Runnable startReadingFromScoket;

    public class LocalWifiBinder extends Binder{
        public WifiNetService getService() {
            return WifiNetService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this,getMainLooper(),null);
        wifiReceiver = new WiFiDirectBroadcastReceiver(this);

        registerReceiver(wifiReceiver,getIntentFilter());

        setEndAccept(false);
        HandlerThread server = new HandlerThread("wifi_server");
        server.start();
        serverAccept = new Handler(server.getLooper(),this);

        //open server socket
        startServerSocket = new OpenServerSocket();
        serverAccept.post(startServerSocket);


    }

    public void setCallBack(CallBack callback) {
        this.callback = callback;
    }

    /**
     * find the available peers
     */
    public void findPeers() {
        manager.discoverPeers(channel,new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG,"discoveryPeers success");
            }

            @Override
            public void onFailure(int i) {
                Log.e(TAG,"discoveryPeers failed " + i);
            }
        });
    }

    /**
     * after find peers success ,call this method to update devices list
     */
    public void getPeersList() {
        manager.requestPeers(channel, this);
    }

    /**
     * connect the peers device
     */
    public void connectDevice(WifiP2pConfig config,WifiP2pDevice device) {
        this.connectDevice = device;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                requestConnection();
            }

            @Override
            public void onFailure(int i) {
                callback.showToastMessage("device connect failed " + i);
            }
        });
    }

    ////////////////////////////////////
    public void createClientSocket(String addres,String name) {
        //close server socket
        setEndAccept(true);
        setReading(false);
        closeServerSocket();
        serverAccept.removeCallbacks(startServerSocket);
        serverAccept.getLooper().quit();
        new ClientConnect().execute(addres,name);
    }
    /////////////////////////////////////////////

    /**
     * call this method after a connect established
     */
    public void requestConnection() {
        manager.requestConnectionInfo(channel, this);
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
        manager.cancelConnect(channel, null);
        if(out != null) {
            try {
                out.close();
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                out = null;
            }
        }

        if(workStart) {
            setReading(false);
            closeClientSocket();
            work.removeCallbacks(startReadingFromScoket);
            work.getLooper().quit();
        }

        if (!getEndAccept()) {
            setEndAccept(true);
            //close Looper Thread
            closeServerSocket();
            serverAccept.removeCallbacks(startServerSocket);
            serverAccept.getLooper().quit();
        }

        wifiReceiver = null;
        channel = null;
        manager = null;
    }

    private synchronized void setReading(boolean b) {
        reading = b;
    }

    private synchronized boolean getReading(){
        return reading;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localWifiBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        localWifiBinder = null;
        Log.e(TAG,"unbind WifiNetService");
        unregisterReceiver(wifiReceiver);
        return super.onUnbind(intent);
    }

    private IntentFilter getIntentFilter() {
        //create an intentfilter for broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return intentFilter;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        if(wifiP2pDeviceList != null) {
            callback.updateDevicesList(wifiP2pDeviceList);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if(wifiP2pInfo.isGroupOwner) {
            callback.showToastMessage("i am group owner");
        }else {
            callback.showToastMessage("i am not group owner");
        }
        createClientConnect(wifiP2pInfo);
    }

    /**
     * to create a client Socket connect
     * @param wifiP2pInfo contain InetSocketAddress
     */
    private void createClientConnect(WifiP2pInfo wifiP2pInfo) {
        //close server socket
//        setEndAccept(true);
//        setReading(false);
//        closeServerSocket();
//        serverAccept.removeCallbacks(startServerSocket);
//        serverAccept.getLooper().quit();
//        new ClientConnect().execute(wifiP2pInfo);
    }

    private class ClientConnect extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String host = strings[0];
            String name = strings[1];
            try {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress(host, port));
                clientSocket = socket;
                openWorkThread();
                writeMessage(name);
            } catch (IOException e) {
                Log.e(TAG,e.getMessage(),e);
                callback.showToastMessage("client socket connect error");
            }
            host = null;
            name = null;
            return null;
        }
    }

    /**
     * after a socket established ,open work thread to read and write data
     */
    private void openWorkThread() {
        if(!workStart) {
            workStart = true;
            HandlerThread workThread = new HandlerThread("workThread");
            workThread.start();
            work = new Handler(workThread.getLooper(),this);
            startReadingFromScoket = new OpenReading();
            work.post(startReadingFromScoket);
        }
    }

    /**
     * @param hello the data to be written to socket
     */
    private OutputStream out;
    private void writeMessage(String hello) {
        try{
            if(out == null) {
                out = clientSocket.getOutputStream();
            }
            out.write(hello.getBytes());
        }catch (Exception e) {
            Log.e(TAG,"error in writeMessage " + e.getMessage(),e);
        }
    }

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

    /**
     * create a runnable to read from socket all the time
     */
    class OpenReading implements Runnable {

        @Override
        public void run() {
            InputStream in = null;
            setReading(true);
            try {
                in = clientSocket.getInputStream();
                int bytes = 0;
                byte[] buff = new byte[1024];
                while (getReading()) {
                    bytes = in.read(buff);
                    byte[] data = new byte[bytes];
                    System.arraycopy(buff,0,data,0,bytes);
                    callback.readData(new String(data));
                }
            }catch (Exception e) {
                Log.e(TAG,"error in read from socket " + e.getMessage(),e);
            }finally {
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        in  = null;
                    }
                }
            }
        }
    }

    private void closeClientSocket() {
        if(clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                clientSocket = null;
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
                    Log.e(TAG,"waiting accept ...");
                    clientSocket = serverSocket.accept();
                    openWorkThread();
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
        void updateDevicesList(WifiP2pDeviceList devicesList);
        void showToastMessage(String message);
        void readData(String data);
    }

    private final int OPEN_SERVER = 0;  //open server socket
    private final int OPEN_READING = 1;  //open work thread to read from socket and write to socket
    private final int CLOSE_SERVER_SOCKET = 2;

}
