package com.rftransceiver.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brige.wifi.WifiNetService;
import com.rftransceiver.R;
import com.rftransceiver.activity.GroupActivity;
import com.rftransceiver.customviews.ArcView;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.customviews.CommonAdapter;
import com.rftransceiver.customviews.CommonViewHolder;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.GroupUtil;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.PoolThreadUtil;

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
public class RawGroupFragment extends Fragment implements View.OnClickListener,Handler.Callback{

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
    @InjectView(R.id.img_group_owner)
    ImageView imgOwner;
    @InjectView(R.id.grid_members_raw)
    GridView gridMembers;

    //标识建组和加组的过程
    private GroupActivity.GroupAction action;

    //组的名字
    private String groupName;

    //按时循环扫描的timer
    private Timer scanTimer;

    //按时获取扫描结果的timer
    private Timer resultTimer;

    //保存搜索的结果
    private List<ScanResult> scanResults;

    //回调接口
    private CallBackInRawGroup callback;

//    /**
//     * count have connected wifiAp
//     */
//    private int count;

    //标识是否正在搜索wifi热点
    private boolean isConnecting = false;

    private LayoutInflater inflater;

    //显示群主图片的宽度和高度
    private int memberWidth;
    private int memberHeight;

    //群主信息显示的坐标
    private int randomRangeX;
    private int randomRangeY;

    //生成随机数
    private Random random = new Random();

    //成员id
    private int memberId = 1;

    //已连接的热点的名称
    private String connectedSsid;

    //已经建立socket连接的热点名称
    private String socketedSsid;

    //标识是否需要将自己的信息发出去
    //private boolean needWriteMember = false;

    //记录成员信息
    private List<GroupMember> listMembers;

    //记录自己在各个组中的id
    private HashMap<String,Integer> subIds;

    //与异步线程交互
    private static Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper(),RawGroupFragment.this);
        //区分建组和加组
        action = getArguments().getInt(ACTION_MODE) == 0 ?
                GroupActivity.GroupAction.CREATE : GroupActivity.GroupAction.ADD;
        if(action == GroupActivity.GroupAction.CREATE) {
            groupName = getArguments().getString(GROUP_NAME);
        }
        //实例化各容器
        listMembers = new ArrayList<>();
        scanResults = new ArrayList<>();
        subIds = new HashMap<>();
        memberWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                50,getResources().getDisplayMetrics());
        memberHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                70,getResources().getDisplayMetrics());
    }

    /**
     * 初始化timers
     */
    private void initTimers() {
        scanTimer = new Timer();
        //每隔1秒扫描一次热点
        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (callback != null) callback.startScan();
            }
        },0, 1000);
        resultTimer = new Timer();
        //每隔1.5秒获取一次扫描结果
        resultTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                refreshWifiAp();
            }
        },0,1500);
    }

    /**
     * 获取扫描结果
     */
    private void refreshWifiAp() {
        if(callback != null) {
            List<ScanResult> scans = callback.getScanResult();
            if(scans == null) return;
            //根据wifi信号的强度排序
            for(int i=0;i<scans.size();i++) {
                for(int j=1;j<scans.size();j++)
                {
                    if(scans.get(i).level<scans.get(j).level)    //level属性即为强度
                    {
                        ScanResult temp = null;
                        temp = scans.get(i);
                        scans.set(i, scans.get(j));
                        scans.set(j, temp);
                    }
                }
            }
            for(int i = 0; i < scans.size();i++) {
                ScanResult result = scans.get(i);
                String ssid = result.SSID.toLowerCase();
                if(WifiNetService.isSSIDValid(ssid)) {
                    handler.obtainMessage(ADD_DATA, -1, -1, result).sendToTarget();
                }
            }
        }
    }


    /**
     * 连接wifi热点
     */
    private void connectWifi(ScanResult result) {
        if(!isConnecting && callback != null) {
            isConnecting = true;
            callback.connectWifiAp(result.SSID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rawgroup, container, false);
        initView(view);
        initEvent();
        this.inflater = inflater;
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this, view);
        final String path = getActivity().getSharedPreferences(Constants.SP_USER, 0).getString(Constants.PHOTO_PATH, "");
        if(!TextUtils.isEmpty(path)) {
            PoolThreadUtil.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    int size = (int) (80 * getResources().getDisplayMetrics().density + 0.5f);
                    Bitmap bitmap = ImageUtil.createImageThumbnail(path, size * size);
                    if(bitmap != null) {
                        handler.obtainMessage(GET_BITMAP,-1,-1,bitmap).sendToTarget();
                        bitmap = null;
                    }
                }
            });
        }
        if(action == GroupActivity.GroupAction.CREATE) {
            //creating group
            tvTitle.setText(getString(R.string.wait_gruop_member_in));
            tvTip.setText(getString(R.string.tip_member_in,groupName));
            gridMembers.setAdapter(new CommonAdapter<GroupMember>(getActivity(),listMembers,
                    R.layout.grid_item_members) {
                @Override
                public void convert(CommonViewHolder helper, GroupMember item) {
                    String name = item.getName();
                    if(!TextUtils.isEmpty(name)) {
                        helper.setText(R.id.tv_member_name,name);
                        name = null;
                    }
                    Drawable drawable = item.getDrawable();
                    if(drawable != null) {
                        helper.setImageDrawable(R.id.img_member_photo,drawable);
                        drawable = null;
                    }
                }
            });
        }else {
            //adding group
            btnSure.setVisibility(View.GONE);
            imgOwner.setVisibility(View.GONE);
            gridMembers.setVisibility(View.GONE);
            tvTitle.setText(getString(R.string.search_nearby_group));
            tvTip.setText(getString(R.string.choose_group_in));

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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(action == GroupActivity.GroupAction.ADD) {
            initTimers();
        }
    }

    private void initEvent() {
        btnCancel.setOnClickListener(this);
        btnSure.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        arcView1.startRipple(0);
        arcView2.startRipple(1300);
        arcView3.startRipple(2600);
    }

    @Override
    public void onPause() {
        super.onPause();
        arcView1.stopRipple();
        arcView2.stopRipple();
        arcView3.stopRipple();
    }

    /**
     * @param result
     * @return true if the result is contained in scanResults
     */
    private boolean containResult(ScanResult result) {
        int size = scanResults.size();
        for(int i = 0;i < size;i++) {
            if(WifiNetService.compareTwoSsid(scanResults.get(i).SSID,result.SSID)) {
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
        stopScanWifiAp();
        super.onDestroy();
        setCallback(null);
        scanResults = null;
        listMembers = null;
    }

    /**
     * 已有设备连接到热点
     * @param ssid
     */
    public void wifiApConnected(String ssid) {
        if(connectedSsid != null && WifiNetService.compareTwoSsid(ssid,connectedSsid)) return;
        connectedSsid = ssid;
        //建立socket连接
        if(callback == null) return;
        callback.getSocketConnect(ssid,true);
//        if(needWriteMember) {
//            callback.getSocketConnect(ssid,false);
//        }else {
////            int size = scanResults.size();
////            if(size <= 0 || count >= size) return;
////            ScanResult connectResult = scanResults.get(count);
//            //建立socket连接
//            if(callback == null) return;
//            callback.getSocketConnect(ssid,true);
//        }
    }

    /**
     *
     * @param object JsonObject 保存组的基本信息
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
            final String name = object.getString(GroupUtil.NAME);
            byte[] photo = null;
            try {
                photo = Base64.decode(object.getString(GroupUtil.PIC),Base64.DEFAULT);
            }catch (Exception e) {
                e.printStackTrace();
            }
            final Bitmap bitmap;
            if(photo != null) {
                bitmap = BitmapFactory.decodeByteArray(photo,0,photo.length);
            }else {
                bitmap = null;
            }

            if(action == GroupActivity.GroupAction.ADD) {
                View view = inflater.inflate(R.layout.member_view, null);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(memberWidth,memberHeight);
                layoutParams.leftMargin = random.nextInt(randomRangeX) + 5;
                layoutParams.topMargin = random.nextInt(randomRangeY) + 10;
                view.setLayoutParams(layoutParams);
                TextView tvName = (TextView) view.findViewById(R.id.tv_name);
                ImageView imageView = (ImageView) view.findViewById(R.id.img_photo);

                tvName.setText(name);
                view.setTag(connectedSsid);

                Drawable drawable = null;
                if(bitmap != null) {
                    drawable = new CircleImageDrawable(bitmap);
                }
                if(drawable != null) {
                    imageView.setImageDrawable(drawable);
                    drawable = null;
                }

                memberId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                subIds.put(connectedSsid,memberId);

                byte[] asyncWord = Base64.decode(object.getString(GroupUtil.GROUP_ASYNC_WORD),Base64.DEFAULT);

                if(callback != null) callback.setGroupBaseINfo(name,asyncWord);

                final ImageView chooseView = (ImageView) view.findViewById(R.id.img_member_choose);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(chooseView.getVisibility() == View.INVISIBLE) {
//                            //停止扫描wifi热点
//                            stopScanWifiAp();
                            chooseView.setVisibility(View.VISIBLE);
                            hideOtherViews();
                            String idKey = (String)view.getTag();
                            memberId = subIds.get(idKey);
                            finalConnect(idKey);
                            tvTip.setText(getString(R.string.wait_group_owner_sure));
                        }
                    }
                });
                memberLayout.addView(view);
            }else {
                memberId = object.getInt(GroupUtil.GROUP_MEMBER_ID);
                GroupMember member = new GroupMember(name,memberId,bitmap);
                listMembers.add(member);

                CommonAdapter adapter = (CommonAdapter)gridMembers.getAdapter();
                adapter.notifyDataSetChanged();

                if(callback != null) {
                    callback.addMember(member);
                }
            }
        }catch (Exception e) {
            Log.e("addGroupInUi", "error in get base info from received JsonObject", e);
        }finally {
            object = null;
        }

    }

    /**
     * 停止查找和更新wifi热点
     */
    private void stopScanWifiAp() {
        if(scanTimer != null) {
            scanTimer.cancel();
            scanTimer = null;
        }
        if(resultTimer != null) {
            resultTimer.cancel();
            resultTimer = null;
        }
    }

    /**
     *
     * @return 我在组里的id
     */
    public int getMyId() {
        if(action == GroupActivity.GroupAction.CREATE) {
            return 0;
        }
        return memberId;
    }

    /**
     *
     * @param ssid socket连接已经建立
     */
    public void socketConnected(String ssid) {
        socketedSsid = ssid;
//        if(needWriteMember) {
//            needWriteMember = false;
//            callback.writeMemberInfo();
//        }
    }

    /**
     * have chosed group to add
     * @param ssid
     */
    private void finalConnect(String ssid) {
        if(callback == null) return;
        //当前的socket就是最终要连的socket
        callback.writeMemberInfo();
//        if(WifiNetService.compareTwoSsid(ssid, connectedSsid)) {
//            //热点已连接
//            if(WifiNetService.compareTwoSsid(ssid, socketedSsid)) {
//                //当前的socket就是最终要连的socket
//                callback.writeMemberInfo();
//            }else {
//                needWriteMember = true;
//                callback.getSocketConnect(ssid,false);
//            }
//        }else {
//            //重连wifi热点
//            callback.connectWifiAp(ssid);
//            needWriteMember = true;
//        }
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
        for(int i = 0;i < memberLayout.getChildCount();i ++) {
            View view = memberLayout.getChildAt(i);
            ImageView chooseView = (ImageView)view.findViewById(R.id.img_member_choose);
            if(chooseView.getVisibility() == View.INVISIBLE) {
                memberLayout.removeView(view);
            }
        }
    }

    /**
     * socket连接建立失败
     * @param ssid
     */
    public void scoketConnectFailed(String ssid) {
        handler.obtainMessage(SOCKET_FAILED, -1, -1, ssid).sendToTarget();
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
                if(listMembers.size() == 0) {
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
     * 得到自己在当前组的id
     * @return
     */
    public int getCurrentId() {
        try {
            return subIds.get(connectedSsid);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    /**
     * 取消建组或者加组
     */
    private void cancelEstablish() {
        if(callback == null) return;
        if(action == GroupActivity.GroupAction.ADD && subIds.size() > 0 && connectedSsid != null) {
            callback.cancelAddGroup(getCurrentId());
        }else {
            callback.cancelCreateGroup();
        }
        getActivity().finish();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case ADD_DATA:
                //搜索到一个可用的wifi热点
                stopScanWifiAp();
                ScanResult result = (ScanResult)message.obj;
                if(!containResult(result)) {
                    if(Constants.DEBUG) {
                        Log.e("refreshWifiAp","扫描到" + result.SSID);
                    }
                    scanResults.add(result);
                    connectWifi(result);
                }
                break;
            case RECEIVED_A_GROUP:
                addGroupInUI((JSONObject)message.obj);
                isConnecting = false;
//                if(scanTimer != null) {
//                    count ++;
//                    connectWifi();
//                }
                break;
            case CANCEL_ADD_GROUP:
                int id = message.arg1;
                for(int i = 0;i < listMembers.size();i++) {
                    if((int)listMembers.get(i).getId() == id) {
                        listMembers.remove(i);
                        break;
                    }
                }
                CommonAdapter adapter = (CommonAdapter)gridMembers.getAdapter();
                adapter.notifyDataSetChanged();
                break;
            case CANCEL_CREATE_GROUP:
                String ssid = (String)message.obj;
                if(ssid != null && WifiNetService.compareTwoSsid(ssid, socketedSsid)) {
                    Toast.makeText(getActivity(),"群主已取消",Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            case GET_BITMAP:
                Bitmap bitmap = (Bitmap)message.obj;
                if(bitmap != null) {
                    Drawable drawable = new CircleImageDrawable(bitmap);
                    imgPhoto.setImageDrawable(drawable);
                    if(action == GroupActivity.GroupAction.CREATE && callback != null) {
                        imgOwner.setImageDrawable(drawable);
                        SharedPreferences sp = getActivity().getSharedPreferences(Constants.SP_USER,0);
                        String name = sp.getString(Constants.NICKNAME, "");
                        GroupMember member = new GroupMember(name,0,bitmap);
                        callback.addMember(member);
                        name = null;
                        sp = null;
                    }
                    drawable = null;
                    bitmap = null;
                }
                break;
            case SOCKET_FAILED:
                Toast.makeText(getActivity(),"与" + (String)message.obj + "通信失败，请取消重新加组！",Toast.LENGTH_SHORT).show();
                break;
            default:
                return false;
        }
        return true;
    }

    public static final String ACTION_MODE = "action";
    public static final String GROUP_NAME = "name";
    private final int ADD_DATA = 0;   //添加一个扫描到的wifi热点
    private final int RECEIVED_A_GROUP = 1;   //接收到一个组的信息
    private final int CANCEL_ADD_GROUP = 2;   //取消加组
    private final int CANCEL_CREATE_GROUP = 3;  //取消建组
    private final int GET_BITMAP = 4;   //获取到图片
    private final int SOCKET_FAILED = 5;    //socket连接失败


    public interface CallBackInRawGroup {
        /**
         * 扫描热点
         */
        void startScan();
        /**
         * @return 搜索到的wifi热点
         */
        List<ScanResult> getScanResult();
        /**
         * 和指定的热点建立socket连接
         */
        void getSocketConnect(String ssid,boolean requestGroupInfo);

        /**
         * 连接wifi热点
         */
        void connectWifiAp(String ssid);

        /**
         * 添加一个组员
         * @param member
         */
        void addMember(GroupMember member);

        /**
         * 向群主发送自己的信息
         */
        void writeMemberInfo();

        /**
         *取消加组
         * @param id 要取消成员的id
         */
        void cancelAddGroup(int id);

        /**
         * 取消建组
         */
        void cancelCreateGroup();

        /**
         * 完成建组
         */
        void finishCreateGruop();


        /**
         * 设置组的名字和同步字
         * @param name
         * @param asyncWord
         */
        void setGroupBaseINfo(String name,byte[] asyncWord);
    }
}