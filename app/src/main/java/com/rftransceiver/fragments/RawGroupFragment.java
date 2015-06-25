package com.rftransceiver.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.wifi.WifiNetService;
import com.rftransceiver.R;
import com.rftransceiver.activity.GroupActivity;
import com.rftransceiver.customviews.ArcView;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;
import com.rftransceiver.util.ImageUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-15.
 */
public class RawGroupFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.tv_top_raw_group)
    TextView tvTitle;
    @InjectView(R.id.tv_tip_info_raw_group)
    TextView tvTip;
    @InjectView(R.id.member_layout)
    RelativeLayout memberLayout;
    @InjectView(R.id.img_photo_raw_group)
    ImageView imgPhoto;
    @InjectView(R.id.btn_sure_raw_group)
    Button btnSure;
    @InjectView(R.id.btn_cancel_raw_group)
    Button btnCancel;
    @InjectView(R.id.arcview1)
    ArcView arcView1;
    @InjectView(R.id.arcview2)
    ArcView arcView2;
    @InjectView(R.id.arcview3)
    ArcView arcView3;

    /**
     * CREATE indicate is creating group
     * ADD indicate is adding group
     */
    private GroupActivity.GroupAction action;

    /**
     * the group's name
     */
    private String groupName;

    /**
     * the timer to get searched wifi device
     */
    private Timer resultTimer;

    private Handler handler;

    /**
     * save searched result
     */
    private List<ScanResult> scanResults;

    /**
     * the callback interface
     */
    private CallBackInRawGroup callback;

    /**
     * count have connected wifiAp
     */
    private int count;

    /**
     * true if is connecting wifiAp
     */
    private boolean isConnecting = false;

    private LayoutInflater inflater;

    /**
     * the width and height of searched group owner's view show in memberLayout
     */
    private int memberWidth;
    private int memberHeight;

    /**
     * the range of x and y to create a left margin of searched info to show in member layout
     */
    private int randomRangeX;
    private int randomRangeY;

    /**
     * to create random left margin and right margin for searched group or member info to show in memberLayout
     */
    private Random random = new Random();

    /**
     * during create group,group owner need to distribute is for every member
     */
    private int memberId = 1;

    /**
     * save member views in memberLayout
     */
    private List<View> memberViews;

    /**
     * have connected wifiAp's ssid
     */
    private String connectedSsid;

    /**
     * current connected socket wifiAp's ssid
     */
    private String socketedSsid;

    /**
     * true if need write member info to group owner,but connection has closed,
     * need to reconnect
     */
    private boolean needWriteMember = false;

    private boolean stopScan = false;

    private HashMap<String,Integer> subIds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        action = getArguments().getInt(ACTION_MODE) == 0 ?
                GroupActivity.GroupAction.CREATE : GroupActivity.GroupAction.ADD;
        if(action == GroupActivity.GroupAction.CREATE) {
            groupName = getArguments().getString(GROUP_NAME);
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case ADD_DATA:
                        //add a wifi hot to list
                        ScanResult result = (ScanResult)msg.obj;
                        if(result == null) return;
                        scanResults.add(result);
                        connectWifi();
                        break;
                    case RECEIVED_A_GROUP:
                        isConnecting = false;
                        addGroupInUI((JSONObject)msg.obj);
                        if(!getStopScan()) {
                            count ++;
                            connectWifi();
                        }
                        break;
                    case CANCEL_ADD_GROUP:
                        int id = msg.arg1;
                        for(int i = 0;i < memberViews.size();i++) {
                            if((int)memberViews.get(i).getTag() == id) {
                                memberLayout.removeView(memberViews.get(i));
                                break;
                            }
                        }
                        break;
                    case CANCEL_CREATE_GROUP:
                        String ssid = (String)msg.obj;
                        if(ssid != null && ssid.equals(socketedSsid)) {
                            Toast.makeText(getActivity(),"群主已取消",Toast.LENGTH_SHORT).show();
                            getActivity().finish();
                        }
                        break;
                }
            }
        };
        memberViews = new ArrayList<>();
        scanResults = new ArrayList<>();
        subIds = new HashMap<>();
        memberWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                50,getResources().getDisplayMetrics());
        memberHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                70,getResources().getDisplayMetrics());
    }

    /**
     * connect the wifiAp
     */
    private void connectWifi() {
        if(count < scanResults.size()) {
            ScanResult result = scanResults.get(count);
            if(!isConnecting) {
                isConnecting = true;
                if(callback != null) callback.connectWifiAp(result.SSID);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rawgroup,container,false);
        initView(view);
        initEvent();
        this.inflater = inflater;
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this,view);
        if(action == GroupActivity.GroupAction.CREATE) {
            //creating group
            tvTitle.setText(getString(R.string.wait_gruop_member_in));
            tvTip.setText(getString(R.string.tip_member_in,groupName));
        }else {
            //adding group
            tvTitle.setText(getString(R.string.search_nearby_group));
            tvTip.setText(getString(R.string.choose_group_in));
            btnSure.setVisibility(View.GONE);
        }

        String path = getActivity().getSharedPreferences(Constants.SP_USER,0).getString(Constants.PHOTO_PATH,"");
        if(!TextUtils.isEmpty(path)) {
            int size = (int)(80 * getResources().getDisplayMetrics().density + 0.5f);
            Bitmap bitmap = ImageUtil.createImageThumbnail(path,size * size);
            imgPhoto.setImageDrawable(new CircleImageDrawable(bitmap));

        }
        /**
         * get the random range
         */
        if(randomRangeX == 0 && randomRangeY == 0)  {
            ViewTreeObserver observer = memberLayout.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    randomRangeX = memberLayout.getWidth()-memberWidth-11;
                    randomRangeY = memberLayout.getHeight()-memberHeight-11;
                    return true;
                }
            });
        }
    }

    private void initEvent() {
        btnCancel.setOnClickListener(this);
        btnSure.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(action == GroupActivity.GroupAction.ADD) {
            if(!getStopScan()) {
                initTimer();
            }
        }
        arcView1.startRipple(0);
        arcView2.startRipple(1300);
        arcView3.startRipple(2600);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(action == GroupActivity.GroupAction.ADD) {
            if(resultTimer != null) {
                resultTimer.cancel();
                resultTimer = null;
            }
        }
        arcView1.stopRipple();
        arcView2.stopRipple();
        arcView3.stopRipple();
    }

    /**
     * the timer get scan result per 2 second
     */
    private void initTimer() {
        if(resultTimer == null) {
            resultTimer = new Timer();
            setStopScan(false);
            resultTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(callback != null) {
                        List<ScanResult> scans = callback.getScanResult();
                        if(scans == null) return;
                        for (ScanResult result : scans) {
                            if((result.SSID.startsWith(WifiNetService.WIFI_HOT_HEADER)
                                    || result.SSID.startsWith("\""+WifiNetService.WIFI_HOT_HEADER))
                                    && !containResult(result)) {
                                if(!getStopScan()) {
                                    handler.obtainMessage(ADD_DATA, -1, -1, result).sendToTarget();
                                }
                            }
                        }
                    }
                }
            },100,8000);
        }
    }

    /**
     * @param result
     * @return true if the result is contained in scanResults
     */
    private boolean containResult(ScanResult result) {
        for(int i = 0;i < scanResults.size();i++) {
            if(scanResults.get(i).SSID.equalsIgnoreCase(result.SSID)) {
                return true;
            }
        }
        return false;
    }

    public void setCallback(CallBackInRawGroup callback) {
        this.callback = callback;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setCallback(null);
        scanResults = null;
        memberViews = null;
    }

    /**
     * the wifiAp has connected
     * @param ssid
     */
    public void wifiApConnected(String ssid) {
        connectedSsid = ssid;
        if(needWriteMember) {
            callback.getSocketConnect(ssid,false);
        }else {
            int size = scanResults.size();
            if(size <= 0 || count >= size) return;
            ScanResult connectResult = scanResults.get(count);
            if(ssid.equals(connectResult.SSID)) {
                //to establish socket connect
                if(callback == null) return;
                callback.getSocketConnect(ssid,true);
            }
        }
    }

    /**
     *
     * @param object JsonObject saved group base info
     */
    public void showGroupBaseInfo(JSONObject object) {
        handler.obtainMessage(RECEIVED_A_GROUP,-1,-1,object).sendToTarget();
    }

    /**
     * to show group in Ui
     * @param object JsonObject saved group base info
     */
    private void addGroupInUI(JSONObject object) {
        try {
            /**
             * init the view to show searched info
             */
            View view = inflater.inflate(R.layout.member_view, null);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(memberWidth,memberHeight);
            layoutParams.leftMargin = random.nextInt(randomRangeX) + 5;
            layoutParams.topMargin = random.nextInt(randomRangeY) + 5;
            view.setLayoutParams(layoutParams);
            TextView tvName = (TextView) view.findViewById(R.id.tv_name);
            ImageView imageView = (ImageView) view.findViewById(R.id.img_photo);

            final String name = object.getString(GroupUtil.NAME);
            byte[] photo = Base64.decode(object.getString(GroupUtil.PIC),Base64.DEFAULT);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(photo,0,photo.length);

            tvName.setText(name);
            imageView.setImageDrawable(new CircleImageDrawable(bitmap));

            if(action == GroupActivity.GroupAction.ADD) {
                view.setTag(connectedSsid);

                memberId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                subIds.put(connectedSsid,memberId);

                byte[] asyncWord = Base64.decode(object.getString(GroupUtil.GROUP_ASYNC_WORD),Base64.DEFAULT);

                final ImageView chooseView = (ImageView) view.findViewById(R.id.img_member_choose);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setStopScan(true);
                        //stop scan wifiAp
                        if(resultTimer != null) {
                            resultTimer.cancel();
                            resultTimer = null;
                        }
                        chooseView.setVisibility(View.VISIBLE);
                        hideOtherViews();
                        finalConnect((String)view.getTag());
                        tvTip.setText(getString(R.string.wait_group_owner_sure));
                        callback.addMember(new GroupMember(name,0,bitmap));
                    }
                });
            }else {
                memberId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                view.setTag(memberId);
                callback.addMember(new GroupMember(name,memberId,bitmap));
            }
            memberViews.add(view);
            memberLayout.addView(view);
        }catch (Exception e) {
            Log.e("addGroupInUi", "error in get base info from received JsonObject", e);
        }finally {
            object = null;
        }

    }

    private synchronized void setStopScan(boolean scan) {
        this.stopScan = scan;
    }

    private synchronized boolean getStopScan() {
        return this.stopScan;
    }

    /**
     *
     * @param ssid socket established under this ssid
     */
    public void socketConnected(String ssid) {
        socketedSsid = ssid;
        if(needWriteMember) {
            needWriteMember = false;
            callback.writeMemberInfo();
        }
    }

    /**
     * have chosed group to add
     * @param ssid
     */
    private void finalConnect(String ssid) {
        if(callback == null) return;
        if(ssid.equals(connectedSsid)) {
            //Ap has connected
            if(ssid.equals(socketedSsid)) {
                //pre socket is what needed, no need to reconnect
                callback.writeMemberInfo();
            }else {
                needWriteMember = true;
                callback.getSocketConnect(ssid,false);
            }
        }else {
            //need reconnect wifiAp
            callback.connectWifiAp(ssid);
            needWriteMember = true;
        }
    }

    /**
     *
     */
    public void cancelAddGroup(int closeId) {
        handler.obtainMessage(CANCEL_ADD_GROUP,closeId,-1,null).sendToTarget();
    }

    public void cancelCreate(JSONObject object) {
        try {
            String ssid = object.getString(GroupUtil.GROUP_SSID);
            handler.obtainMessage(CANCEL_CREATE_GROUP,-1,-1,ssid).sendToTarget();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hide unused view in memberLayout
     */
    private void hideOtherViews() {
        for(int i = 0;i < memberViews.size();i++) {
            View view = memberViews.get(i);
            ImageView chooseView = (ImageView)view.findViewById(R.id.img_member_choose);
            if(chooseView.getVisibility() == View.INVISIBLE) {
                memberLayout.removeView(view);
            }
        }
    }

    /**
     * socket connect failed
     * @param ssid
     */
    public void scoketConnectFailed(String ssid) {
        if(ssid.equals(scanResults.get(count).SSID)) {
            Log.e("scoketConnectFailed"," in ssid " + ssid);
            connectWifi();
        }
    }


    /**
     *
     * @param action 0 indicates is creating group
     *               1 indicates is adding group
      * @param name group's name
     * @return the instance of RawGroupFragment
     */
    public static RawGroupFragment getInstance(int action,String name) {
        Bundle bundle = new Bundle();
        if(name != null) {
            bundle.putString(GROUP_NAME,name);
        }
        bundle.putInt(ACTION_MODE,action);
        RawGroupFragment fragment = new RawGroupFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_sure_raw_group:
                if(memberViews.size() == 0) {
                    Toast.makeText(getActivity(),"请等待成员加入或取消建组",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(callback == null) return;
                callback.finishCreateGruop();
                break;
            case R.id.btn_cancel_raw_group:
                cancelEstablish();
                break;
        }
    }

    /**
     * cancel establish group
     */
    private void cancelEstablish() {
        if(callback == null) return;
        if(action == GroupActivity.GroupAction.ADD) {
            if(getStopScan()) {
                callback.cancelAddGroup(subIds.get(connectedSsid));
            }else {
                setStopScan(true);
                if(resultTimer != null) {
                    resultTimer.cancel();
                    resultTimer = null;
                }
            }
        }else {
            callback.cancelCreateGroup();
        }
        getActivity().finish();
    }

    public static final String ACTION_MODE = "action";
    public static final String GROUP_NAME = "name";
    private final int ADD_DATA = 0;   //notify that have receive a wifi device
    private final int RECEIVED_A_GROUP = 1;   //notify that have receive a group info
    private final int CANCEL_ADD_GROUP = 2;   //notify that have receive a group info
    private final int CANCEL_CREATE_GROUP = 3;

    public interface CallBackInRawGroup {
        /**
         * @return the searched wifi devices
         */
        List<ScanResult> getScanResult();
        /**
         * call to establish socket connection with the wifiAP
         */
        void getSocketConnect(String ssid,boolean requestGroupInfo);

        /**
         * call to connect wifiAp
         */
        void connectWifiAp(String ssid);

        /**
         * call to add a member for a group
         * @param member
         */
        void addMember(GroupMember member);

        /**
         * call to write own info to group owner
         */
        void writeMemberInfo();

        /**
         * call to cancel add group
         * @param id
         */
        void cancelAddGroup(int id);

        /**
         * call to cancel create group
         */
        void cancelCreateGroup();

        /**
         * call after all members in
         */
        void finishCreateGruop();
    }
}
