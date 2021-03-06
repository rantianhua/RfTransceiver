package com.rftransceiver.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.Pools;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.brige.wifi.WifiNetService;
import com.rftransceiver.R;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.fragments.RawGroupFragment;
import com.rftransceiver.fragments.SetGroupNameFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;
import com.rftransceiver.util.PoolThreadUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by rantianhua on 15-6-15.
 */
public class GroupActivity extends Activity implements SetGroupNameFragment.OnGroupNameSet,
        ServiceConnection,WifiNetService.CallBack,RawGroupFragment.CallBackInRawGroup {


    /**
     * 枚举类用来区分是加组还是建组
     */
    public enum GroupAction {
        CREATE,
        ADD
    }
    private GroupAction groupAction = GroupAction.ADD;

    /**
     * 获取组名的Fragment
     */
    private SetGroupNameFragment setGroupNameFragment;

    /**
     *  显示加组和建组的界面
     */
    private RawGroupFragment rawGroupFragment;

    /**
     * 管理wifi的服务
     */
    private WifiNetService service;

    /**
     * 组的描述类
     */
    private GroupEntity groupEntity;

    //用来和非UI线程交互
    private static Handler mainHandler;

    private boolean isWifiOpenPre = false;  //标识在加组之前wifi是否开启

    private int callbaclMembers;    //记录有多少个组成员已经收到整个组的信息
    private int memberCounts;   //组成员的总数
    private String addFailedMessage;    //加组失败的提示

    //发送组的全部信息后，过一段时间检查是否所有成员都收到l
    private final Runnable checkCreateComplete = new Runnable() {
        @Override
        public void run() {
            if(groupEntity.getMembers().size() == 0) return;
            if(callbaclMembers < memberCounts) {
                //有成员未收到组信息
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < groupEntity.getMembers().size();i++) {
                    if(!groupEntity.getMembers().get(i).isAddSucceed()) {
                        builder.append(groupEntity.getMembers().get(i).getName());
                        builder.append(",");
                    }
                }
                if(builder.length() > 1) {
                    builder.delete(builder.length()-2,builder.length()-1);
                    builder.append(memberCounts - callbaclMembers);
                    builder.append("加组失败");
                    addFailedMessage = builder.toString();
                    builder = null;
                    mainHandler.sendEmptyMessage(0);
                }
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        //绑定服务
        bindService(new Intent(this, WifiNetService.class),
                this, BIND_AUTO_CREATE);

        initHandler();

        //得到是加组还是建组
        int action = getIntent().getIntExtra(ACTION_MODE,0);
        groupAction = action == 0 ? GroupAction.CREATE : GroupAction.ADD;

        //实例化一个组的描述类
        groupEntity = new GroupEntity();
        if(groupAction == GroupAction.CREATE) {//建组
            //为组设置一个同步字
            groupEntity.setAsyncWord(GroupUtil.createAsynWord());
            if(setGroupNameFragment == null) initSGF();
            //获取用户输入的组名
            changeFragment(setGroupNameFragment);
        }else {
            //加组
            initRGF(GroupAction.ADD.ordinal(), null);
            changeFragment(rawGroupFragment);
        }

    }

    //初始化Handler
    private void initHandler () {
        mainHandler = new Handler(Looper.myLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        //加组或建组完成
                        if(Constants.DEBUG) {
                            Log.e("handleMessage","加组或建组即将完成");
                        }
                        finishGroup();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
    }

    /**
     * @param fragment 显示指定的Fragment
     */
    private void changeFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.frame_content_group,fragment).commit();
    }

    /**
     * 初始化setGroupNameFragment
     */
    private void initSGF() {
        if(setGroupNameFragment == null) {
            setGroupNameFragment = new SetGroupNameFragment();
            setGroupNameFragment.setOnGroupNameSetCallback(this);
        }
    }

    /**
     * 初始化 rawGroupFragment
     */
    private void initRGF(int action,String name) {
        if(rawGroupFragment == null) {
            rawGroupFragment = RawGroupFragment.getInstance(action,name);
            rawGroupFragment.setCallback(this);
        }
    }

    /**
     * SetGroupNameFragment的接口函数，传送组名
     * @param name the group's name
     */
    @Override
    public void getGroupName(String name) {
        //设置组名
        groupEntity.setName(name);
        setGroupNameFragment.setOnGroupNameSetCallback(null);
        setGroupNameFragment = null;
        initRGF(GroupAction.CREATE.ordinal(),name);
        changeFragment(rawGroupFragment);
        //开启wifi热点
        if(service != null) {
            service.startWifiAp(true);
        }
    }

    /**
     * wifiNetService服务绑定成功
     * @param componentName
     * @param iBinder
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if(iBinder != null) {
            WifiNetService.LocalWifiBinder binder = (WifiNetService.LocalWifiBinder)iBinder;
            service = binder.getService();
            service.setCallBack(this);
            if(groupAction == GroupAction.ADD) {
                //如果是加组，就开启wifi
                if(service.startWifi()) {
                    isWifiOpenPre = false;
                }else {
                    isWifiOpenPre = true;
                }
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service.setCallBack(null);
        service = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(groupAction == GroupAction.CREATE) {
            service.closeWifiAp();
        }else if(groupAction == GroupAction.ADD) {
            if(!isWifiOpenPre) {
                service.closeWifi();
            }
        }
        unbindService(this);
    }

    /**
     * WifiNetService中的回调函数
     * 通知wifi热点已经开启
     */
    @Override
    public void wifiApCreated() {
        //开启服务端socket
        service.openServer();
    }

    /**
     * callback in WifiNetService
     * @param object read from socket
     */
    @Override
    public void readData(String type,final JSONObject object) {
        if(rawGroupFragment == null) return;
        switch (type) {
            case GroupUtil.GROUP_BASEINFO:
                //收到一个群主的基本信息
                rawGroupFragment.showGroupBaseInfo(object);
                break;
            case GroupUtil.MEMBER_BASEINFO:
                //收到一个组成员的基本信息
                rawGroupFragment.showGroupBaseInfo(object);
                break;
            case GroupUtil.CANCEL_ADD:
                //群成员取消加组
                int cancelId = -1;
                try {
                    cancelId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                    if(cancelId != -1) {
                        //删除该组成员
                        rawGroupFragment.cancelAddGroup(cancelId);
                        groupEntity.removeMemberById(cancelId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case GroupUtil.CANCEL_CREATE:
                //群主取消建组
                rawGroupFragment.cancelCreate(object);
                break;
            case GroupUtil.RECEIVE_GROUP:
                //收到一个组成员的de反馈
                callbaclMembers++;
                if(callbaclMembers == memberCounts) {
                    //收到了所有组成员的反馈
                    //可以结束建组了
                    mainHandler.removeCallbacks(checkCreateComplete);
                    mainHandler.sendEmptyMessage(0);
                    callbaclMembers = 0;
                    memberCounts = 0;
                }else {
                    //继续等待其他成员
                    try {
                        //该组员的id
                        int id = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                        int size = groupEntity.getMembers().size();
                        for(int i = 0; i < size;i++) {
                            if(groupEntity.getMembers().get(i).getId() == id) {
                                //设置该成员加组成功
                                groupEntity.getMembers().get(i).setAddSucceed(true);
                                break;
                            }
                        }
                    }catch (Exception e) {

                    }
                }
                break;
            case GroupUtil.GROUP_FULL_INFO:
                Log.e("add group","finish");
                //整个组的信息
                try {
                    JSONArray array = (JSONArray)object.get("msg");
                    //解析处出整个组的信息
                    for(int i = 0;i < array.length();i ++) {
                        JSONObject o = (JSONObject)array.get(i);
                        byte[] photo = null;
                        Bitmap bitmap = null;
                        try{
                            photo = Base64.decode(o.getString(GroupUtil.PIC),Base64.DEFAULT);
                            bitmap = BitmapFactory.decodeByteArray(photo,
                                    0,photo.length);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                        GroupMember member = new GroupMember(o.getString(GroupUtil.NAME),
                                o.getInt(GroupUtil.GROUP_MEMBER_ID),
                                bitmap);
                        groupEntity.getMembers().add(member);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
                mainHandler.sendEmptyMessage(0);
                break;
        }
    }

    /**
     * 建组加组流程完成
     */
    private void finishGroup() {
        String message = null;
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        groupEntity.setTempId(rawGroupFragment.getMyId());
        bundle.putParcelable(EXTRA_GROUP, groupEntity);
        intent.putExtras(bundle);
        setResult(Activity.RESULT_OK,intent);
        if(groupAction == GroupAction.ADD) {
            message = "加组完成";
        }else if(groupAction == GroupAction.CREATE) {
            message = addFailedMessage == null ? "建组完成" : addFailedMessage;
        }
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        saveGroup();
        finish();
    }

    /**
     * 保存群组信息到数据库
     */
    private void saveGroup() {
        final DBManager dbManager = DBManager.getInstance(this);
        try {
            PoolThreadUtil.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    dbManager.saveGroup(groupEntity);
                }
            });
        }catch (Exception e) {
            Log.e("saveGroup","error in saveGroup",e);
        }
    }

    /**
     * WifiNetService中的回调方法
     */
    @Override
    public void wifiEnabled() {

    }

    /**
     * callback in WifiNetService
     * after wifiAp is connected
     */
    @Override
    public void wifiApConnected(String ssid) {
        if(rawGroupFragment != null) {
            rawGroupFragment.wifiApConnected(ssid);
        }
    }

    /**
     * callback in WifiNerService after socket has established
     * @param ssid note the main thread the socket establish successful or not
     */
    @Override
    public void socketCreated(String ssid) {
        if(rawGroupFragment == null) return;
        rawGroupFragment.socketConnected(ssid);
    }

    /**
     * callback in WifiNetService
     * to get group base info to send
     */
    @Override
    public String getGroupInfo(int memberId) {
        groupEntity.setTempId(memberId);
        return GroupUtil.getWriteData(GroupUtil.GROUP_BASEINFO,
                groupEntity,this);
    }

    /**
     * call in WifiNetService
     * @param counts 标识成员的数量
     */
    @Override
    public void sendGroupFinished(int counts) {
        memberCounts = counts;
        //5秒后检查
        mainHandler.postDelayed(checkCreateComplete, 5000);
    }

    /**
     * callback in WifiNetService
     * @param ssid
     */
    @Override
    public void socketConnectFailed(String ssid) {
        if(rawGroupFragment != null) rawGroupFragment.scoketConnectFailed(ssid);
    }

    /**
     * RawGroupFragment的回调函数
     * 得到扫描的wifi热点
     * @return
     */
    @Override
    public List<ScanResult> getScanResult() {
        if(service != null) {
            return service.getScanResults();
        }
        return null;
    }

    /**
     * RawGroupFragment的回调函数
     * 扫描热点
     */
    @Override
    public void startScan() {
        if (service != null) {
            service.startScan();
        }
    }

    /**
     * callback in RawGroupFragment
     * @return
     */
    @Override
    public void cancelAddGroup(int id) {
        if(service == null) return;
        service.cancelAddGroup(id);
    }

    /**
     * RawGroupFragment的回调方法
     * 取得自己在当前组的id
     * @return
     */
    @Override
    public int getIdInCurrentGroup() {
        return rawGroupFragment.getCurrentId();
    }

    /**
     * callback in RawGroupFragment
     * @return
     */
    @Override
    public void cancelCreateGroup() {
        if(service == null) return;
        service.cancelCreateGroup();
    }

    /**
     * RawGroupFragment的回调方法
     * @param ssid 和指定的热点建立socket连接
     * @param requestGroupInfo 标识连接后是否请求获取群主的信息
     */
    @Override
    public void getSocketConnect(String ssid,boolean requestGroupInfo) {
        if(service == null) return;
        if(Constants.DEBUG) {
            Log.e("getSocketConnect","准备和"+ssid + "建立socket连接");
        }
        service.createClientSocket(ssid,requestGroupInfo);
    }

    /**
     * RawGroupFragment中的回调函数，连接指定的热点
     * @param ssid the wifiAp to be connected
     */
    @Override
    public void connectWifiAp(String ssid) {
        if(service != null) {
            if(Constants.DEBUG) {
                Log.e("connectWifiAp","将要连接"+ssid);
            }
            if(service.ssidConnected(ssid)) {
                if(Constants.DEBUG) {
                    Log.e("connectWifiAp","已连接到"+ssid);
                }
                if(rawGroupFragment != null) {
                    rawGroupFragment.wifiApConnected(ssid);
                }
            }else {
                service.connectAction(ssid);
            }
        }
    }

    /**
     * call in RawGroupFragment
     */
    @Override
    public void finishCreateGruop() {
        if(service == null) return;
        service.sendFullGroup(groupEntity);
    }

    /**
     * callback in RawGroupFragment
     */
    @Override
    public void writeMemberInfo() {
        if(service == null) return;
        service.writeMemberInfo(GroupUtil.getWriteData(GroupUtil.MEMBER_BASEINFO,
                null,this));
    }

    /**
     * callback in RawGroupFragment
     * @param name
     * @param asyncWord
     */
    @Override
    public void setGroupBaseINfo(String name, byte[] asyncWord) {
        if(groupEntity != null ) {
            groupEntity.setName(name);
            groupEntity.setAsyncWord(asyncWord);
        }
    }

    /**
     *
     * @param member
     */
    @Override
    public void addMember(GroupMember member) {
        if(groupEntity != null) {
            groupEntity.getMembers().add(member);
        }
    }

    /**
     * the key of ActionGroup
     */
    public static final String ACTION_MODE = "action_mode";

    public static final String EXTRA_GROUP = "groupentity";

}
