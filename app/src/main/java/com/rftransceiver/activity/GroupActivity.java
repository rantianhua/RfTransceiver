package com.rftransceiver.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.brige.wifi.WifiNetService;
import com.rftransceiver.R;
import com.rftransceiver.fragments.RawGroupFragment;
import com.rftransceiver.fragments.SetGroupNameFragment;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;

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
     * the enum to distinguish create group or add group
     */
    public enum GroupAction {
        CREATE,
        ADD
    }
    private GroupAction groupAction = GroupAction.ADD;

    /**
     * the fragment to set group name
     */
    private SetGroupNameFragment setGroupNameFragment;

    /**
     *  add or create group in this fragment
     */
    private RawGroupFragment rawGroupFragment;

    /**
     * manage wifi action
     */
    private WifiNetService service;

    /**
     * mark is scanning device or not
     */
    private boolean scanning = false;

    /**
     * the entity of group
     */
    private GroupEntity groupEntity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        bindService(new Intent(this, WifiNetService.class),
                this, BIND_AUTO_CREATE);

        int action = getIntent().getIntExtra(ACTION_MODE,0);
        groupAction = action == 0 ? GroupAction.CREATE : GroupAction.ADD;

        groupEntity = new GroupEntity();
        if(groupAction == GroupAction.CREATE) {
            //create a group
            groupEntity.setAsyncWord(GroupUtil.createAsynWord());
            SharedPreferences sp = getSharedPreferences(Constants.SP_USER,0);
            groupEntity.setPicFilePath(sp.getString(Constants.PHOTO_PATH,""));

            if(setGroupNameFragment == null) initSGF();
            changeFragment(setGroupNameFragment);
        }else {
            //add a group
            initRGF(GroupAction.ADD.ordinal(), null);
            changeFragment(rawGroupFragment);
        }
    }

    /**
     * @param fragment next to be show
     */
    private void changeFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.frame_content_group,fragment).commit();
    }

    /**
     * init setGroupNameFragment
     */
    private void initSGF() {
        if(setGroupNameFragment == null) {
            setGroupNameFragment = new SetGroupNameFragment();
            setGroupNameFragment.setOnGroupNameSetCallback(this);
        }
    }

    /**
     * init rawGroupFragment
     */
    private void initRGF(int action,String name) {
        if(rawGroupFragment == null) {
            rawGroupFragment = RawGroupFragment.getInstance(action,name);
            rawGroupFragment.setCallback(this);
        }
    }

    /**
     * callback in SetGroupNameFragment
     * @param name the group's name
     */
    @Override
    public void getGroupName(String name) {
        groupEntity.setName(name);
        setGroupNameFragment.setOnGroupNameSetCallback(null);
        setGroupNameFragment = null;
        initRGF(GroupAction.CREATE.ordinal(),name);
        changeFragment(rawGroupFragment);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if(iBinder != null) {
            WifiNetService.LocalWifiBinder binder = (WifiNetService.LocalWifiBinder)iBinder;
            service = binder.getService();
            service.setCallBack(this);
            if(groupAction == GroupAction.CREATE) {
                service.startWifiAp();
            }else if(groupAction == GroupAction.ADD) {
                service.startWifi();
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
            service.closeWifi();
        }
        unbindService(this);
    }

    /**
     * callback in WifiNetService
     * after wifiAp is created
     */
    @Override
    public void wifiApCreated() {
        service.openServer();
    }

    /**
     * callback in WifiNetService
     * @param object read from socket
     */
    @Override
    public void readData(String type,JSONObject object) {
        if(rawGroupFragment == null) return;
        switch (type) {
            case GroupUtil.GROUP_BASEINFO:
                //received group base info
                rawGroupFragment.showGroupBaseInfo(object);
                break;
            case GroupUtil.MEMBER_BASEINFO:
                //show info in RawGroupFragment
                rawGroupFragment.showGroupBaseInfo(object);
                break;
            case GroupUtil.CANCEL_ADD:
                int cancelId = -1;
                try {
                    cancelId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                    if(cancelId != -1) {
                        rawGroupFragment.cancelAddGroup(cancelId);
                        groupEntity.removeMemberById(cancelId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case GroupUtil.CANCEL_CREATE:
                rawGroupFragment.cancelCreate(object);
                break;
            default:
                try {
                    JSONArray array = (JSONArray)object.get("msg");
                    for(int i = 0;i < array.length();i ++) {
                        JSONObject o = (JSONObject)array.get(i);
                        Log.e("full data",o.toString());
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
     * callback in WifiNetService
     * after wifi is opened
     */
    @Override
    public void wifiEnabled() {
        if(groupAction == GroupAction.ADD) {
            if(!scanning) {
                service.startScan();
                scanning = true;
            }
        }
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
     */
    @Override
    public void sendGroupFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GroupActivity.this,"建组完成",Toast.LENGTH_SHORT).show();
                finish();
            }
        });
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
     * callback in RawGroupFragment
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
     * callback in RawGroupFragment
     * @return
     */
    @Override
    public void cancelAddGroup(int id) {
        if(service == null) return;
        service.cancelAddGroup(id);
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
     * callback in RawGroupFragment
     * @param ssid the wifiAp to be connected
     */
    @Override
    public void getSocketConnect(String ssid,boolean requestGroupInfo) {
        if(service == null) return;
        service.createClientSocket(ssid,requestGroupInfo);
    }

    /**
     * callback in RawGroupFragment to connect wifiAp
     * @param ssid the wifiAp to be connected
     */
    @Override
    public void connectWifiAp(String ssid) {
        if(service != null) {
            if(service.ssidConnected(ssid)) {
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

}
