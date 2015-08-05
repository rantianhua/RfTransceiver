package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.LocationActivity;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.customviews.ContextPopMenu;
import com.rftransceiver.customviews.MyListView;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.group.GroupMember;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ExpressionUtil;
import com.rftransceiver.util.GroupUtil;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.PoolThreadUtil;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-14.
 */
public class HomeFragment extends Fragment implements View.OnClickListener,MyListView.ILoadingListener{

    @InjectView(R.id.listview_conversation)
    MyListView listView;
    @InjectView(R.id.et_send_message)
    EditText etSendMessage;
    @InjectView(R.id.btn_send)
    Button btnSend;
    @InjectView(R.id.btn_sounds)
    ImageView btnSounds;
    @InjectView(R.id.img_home_troggle)
    ImageView imgTroggle;
    @InjectView(R.id.img_home_hide)
    ImageView imgHomeHide;
    @InjectView(R.id.tv_tip_home)
    TextView tvTip;
    @InjectView(R.id.img_sounds_text)
    ImageView imgMessageType;
    @InjectView(R.id.img_other)
    ImageView imgAdd;
    @InjectView(R.id.img_home_shoot)
    ImageView imgShoot;
    @InjectView(R.id.img_home_picture)
    ImageView imgPicture;
    @InjectView(R.id.img_home_address)
    ImageView imgAddress;
    @InjectView(R.id.rl_home_imgs_address)
    LinearLayout llOthersData;
    @InjectView(R.id.vp_home_expression)
    ViewPager vp;
    @InjectView(R.id.ll_dots_home)
    LinearLayout llDots;
    @InjectView(R.id.tv_title_content)
    TextView tvTitle;
    @InjectView(R.id.rl_top_home)
    RelativeLayout top;
    @InjectView(R.id.img_face)
    ImageView imgFace;
    //按压button时所显示的图片
    private Bitmap press;
    //抬起button时所显示的图片
    private Bitmap up;
    //聊天背景图片
    private Bitmap backGroud;
    //计算发送语音的时长
    private long curTime;
    private long preTime;
    private long seconds;

    //SoundsTextView中回调函数
    private SoundsTimeCallbacks timeCallbacks;
    //本类的回调函数
    private CallbackInHomeFragment callback;
    //MyListView的适配器
    private ListConversationAdapter conversationAdapter;
    //适配器的数据源
    private List<ConversationData> dataLists;
    //保存所有表情的GridView
    private List<GridView> expressions;
    //当前展示的表情的GridView在vp中的索引
    private int currentEpIndex;
    //GridView的指示器，指示当前在第几页表情
    private List<ImageView> imgDots;
    //从文字中解析表情
    private Html.ImageGetter imgageGetter;
    //当前所在的组
    private GroupEntity groupEntity;

    private Editable.Factory editableFactory = Editable.Factory.getInstance();
    //记录控件所在的区域
    private Rect rect = new Rect();
    //发送图片的大小
    private int imgSendSize;
    //资源文件中定义好的文字资源
    private String tipConnectLose,tipReconnecting,tipConnecSuccess,tipSendSounds,tipStopSounds;
    //标志是否发送语音
    private boolean sendSounds = false;
    //数据库管理
    private DBManager dbManager;
//    //当前视图的标题
//    private String homeTitle;
    //处理异步消息
    private static Handler mainHandler;
    //播放音效
    private SoundPool soundPool;
    //要播放的声音资源的id
    private int soundsId;
    //我在组里的id
    private int myId = -1;
    //当前组在数据库中的id
    private int currentGroupId = -1;
    //要发送的图片的存储地址
    private String sendImgagePath;
    //顶部菜单栏的弹出菜单
    private ContextPopMenu popMenu;

    private Drawable drawableDef;   //默认头像
    //显示正在处理一些事情
    private ProgressDialog pd;
    //标识是否需要自动连接设备
    private boolean needConnecAuto = false;
    //正在发送图片的数据源
    private ConversationData conversationData;
    //标识有没有执行过打开蓝牙的操作
    private boolean openBle = false;
    //标识有没有设置同步字
    private boolean sendAsy = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHandler();
        //得到最新组的id
        currentGroupId = getCurrentGroupId();

        expressions = new ArrayList<>();
        imgDots = new ArrayList<>();
        initImageGetter();

        imgSendSize = 100 * 120;
        tipConnectLose = getResources().getString(R.string.connection_lose);
        tipReconnecting = getResources().getString(R.string.reconnecting);
        tipConnecSuccess = getResources().getString(R.string.connect_success);
        tipSendSounds = getString(R.string.touch_send_sounds);
        tipStopSounds = getString(R.string.loose_stop_sounds);
        dataLists = new ArrayList<>();
        dbManager = DBManager.getInstance(getActivity());

        soundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM,1);
        soundsId = soundPool.load(getActivity(), R.raw.btn_down, 1);
        pd = new ProgressDialog(getActivity());
        //加载Bitmap资源
        loadBitmap();
    }


    @Override
    public void onStart() {
        super.onStart();
        if(!openBle) {
            openBle = true;
            //打开蓝牙
            openBle();
        }
        //先获取有没有新建的组
        if(callback != null) {
            GroupEntity newGroup = callback.getNewGroup();
            if(newGroup != null) {
                dataLists.clear();
                conversationAdapter.updateData(dataLists);
                updateGroup(newGroup);
                return;
            }
        }
        //加载已有的组
        if(groupEntity == null && currentGroupId != -1) {
            loadGroup(currentGroupId);
            Constants.GROUPID = currentGroupId;
        }else if(groupEntity != null){
            showGroupTitle();
            conversationAdapter.updateData(dataLists);
        }else {
            //没有任何组，处于公共频道
            tvTitle.setText("公共频道");
        }
    }

    /**
     * 加载Bitmap资源
     */
    private void loadBitmap() {
        BitmapFactory.Options op1 = new BitmapFactory.Options();
        op1.inSampleSize = 2;
        press = BitmapFactory.decodeResource(getResources(), R.drawable.press, op1);

        BitmapFactory.Options op2 = new BitmapFactory.Options();
        op2.inSampleSize = 2;
        up = BitmapFactory.decodeResource(getResources(),R.drawable.up,op2);

        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 4;
        backGroud = BitmapFactory.decodeResource(getResources(),R.drawable.chatbackground,op);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.photo);
        drawableDef = new CircleImageDrawable(bitmap);
    }

    public void setNeedConnecAuto(boolean needConnecAuto) {
        this.needConnecAuto = needConnecAuto;
    }

    private void openBle() {
        if(callback != null ) {
            switch (callback.isBleOpen()) {
                case 0:
                    //蓝牙已开启
                    if(callback != null && needConnecAuto) callback.connectDeviceAuto();
                    break;
                case 1:
                    //蓝牙为开其：
                    pd.setMessage("正在打开蓝牙");
                    pd.show();
                    callback.openBle();
                    break;
                case 2:
                    //服务还未绑定,100毫秒后再去访问
                    if(mainHandler != null) {
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                openBle();
                            }
                        },100);
                    }
                    break;
            }
        }
    }

    /**
     * 蓝牙已打开
     */
    public void bleOpend() {
        if(pd != null && pd.isShowing()) {
            pd.dismiss();
            if(callback != null && needConnecAuto) {
                //连接绑定过的设备
                callback.connectDeviceAuto();
            }
        }
    }

    private void initHandler() {
        mainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case LOAD_GROUP:
                        //加载了一个组
                        GroupEntity ge = (GroupEntity)msg.obj;
                        updateGroup(ge);
                        PoolThreadUtil.getInstance().addTask(new Runnable() {
                            @Override
                            public void run() {
                                loadConverationData(-1, 20, false);
                            }
                        });
                        break;
                    case LOAD_CONVERDATA:
                        //加载到聊天信息
                        List<ConversationData> dataList = (List<ConversationData>)msg.obj;
                        dataLists.addAll(0,dataList);
                        conversationAdapter.updateData(dataLists);
                        listView.setSelection(dataLists.size()-1);
                        break;
                    case LOAD_COMPELET:
                        //加载完毕
                        listView.loadComplete();
                        break;
                    case CHANGE_GROUP:
                        //改变当前的组
                        int gid = msg.arg1;
                        currentGroupId = gid;
                        dataLists.clear();
                        sendAsy = false;
                        conversationAdapter.updateData(dataLists);
                        groupEntity = null;
                        loadGroup(gid);
                        break;
                    case UPDATE_IMAGE:
                        //更新发送图片的进度
                        int percent = msg.arg1;
                        if(conversationData != null) {
                            conversationData.setPercent(percent);
                            conversationAdapter.notifyDataSetChanged();
                        }
                        break;
                    case UPDATE_BLESTATE:
                        //根据蓝牙连接的状态更新UI
                        if(tvTip == null) return false;
                        boolean connect = (boolean)msg.obj;
                        String text = tvTip.getText().toString();
                        if (connect) {
                            //sendAsync();
                            if (tvTip.getVisibility() ==
                                    View.VISIBLE && text.equals(tipReconnecting)) {
                                tvTip.setText(tipConnecSuccess);
                                tvTip.setVisibility(View.GONE);
                            }
                        } else {
                            if (!text.equals(tipReconnecting)) {
                                tvTip.setText(tipConnectLose);
                                tvTip.setVisibility(View.VISIBLE);
                            }
                        }
                        break;
                    case SEND_IMG:
                        //发送图片
                        if(callback != null) callback.send(MainActivity.SendAction.Image, (String)msg.obj);
                        break;
                }
                return true;
            }
        });

    }

    /**
     * 初始化表情
     * @param inflater
     */
    private void initExpressions(LayoutInflater inflater) {
        for(int i = 0; i < ExpressionUtil.epDatas.size();i ++) {
            GridView gridView = (GridView)inflater.inflate(R.layout.grid_expressiona,null);
            gridView.setAdapter(new CommonAdapter<Integer>(getActivity(),
                    ExpressionUtil.epDatas.get(i),R.layout.grid_expressions_item) {
                @Override
                public void convert(CommonViewHolder helper, Integer item) {
                    helper.setImageResource(R.id.img_expression,item);
                }
            });
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //get expression's id
                    if(i==20)
                        deleteBack(etSendMessage);
                    else{
                        int epId = ExpressionUtil.epDatas.get(currentEpIndex).get(i);
                        insertExpression(epId,etSendMessage);
                    }
                    if(btnSounds.getVisibility() == View.VISIBLE) {
                        btnSounds.setVisibility(View.INVISIBLE);
                        etSendMessage.setVisibility(View.VISIBLE);
                    }
                }
            });
            expressions.add(gridView);
            ImageView imgDot = (ImageView)inflater.inflate(R.layout.img_dot,null);
            if(i == 0) {
                imgDot.setSelected(true);
            }

            llDots.addView(imgDot);
            imgDots.add(imgDot);
        }
        vp.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return expressions.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(expressions.get(position));
                return expressions.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(expressions.get(position));
            }
        });
        vp.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                imgDots.get(currentEpIndex).setSelected(false);
                currentEpIndex = position;
                imgDots.get(position).setSelected(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

        });
        vp.setCurrentItem(0);
    }


    /**
     * 将表情插入到文本
     * @param drawableId
     */
    private void insertExpression(int drawableId,View view) {
        String source = "<img src='" + drawableId+ "'/>";
        CharSequence cs = Html.fromHtml(source,imgageGetter,null);
        if(view instanceof EditText) {
            EditText editText = (EditText)view;
            editText.append(cs);
        }else if(view instanceof TextView) {
            TextView tv = (TextView)view;
            tv.append(cs);
        }
    }

    /**
     * editText内容回删
     * @param view
     */
    private void deleteBack(View view) {
        if(view instanceof EditText) {
            EditText editText = (EditText)view;
            int len = editText.length();
            if(len!=0) editText.getText().delete(len - 1, len);
        }
    }

    /**
     * 初始化解析表情的实例
     */
    private void initImageGetter() {
        imgageGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String s) {
                int id = Integer.parseInt(s);
                Drawable drawable = getResources().getDrawable(id);
                if(drawable != null) {
                    drawable.setBounds(0,0,40,
                            40);
                }
                return drawable;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_content,container,false);
        initView(v);
        initExpressions(inflater);
        initEvent();
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this, v);
        imgMessageType.setSelected(true);
        listView.setInterface(this);
        btnSounds.setImageBitmap(up);
        listView.setBackground(new BitmapDrawable(backGroud));
        if(conversationAdapter == null) {
            conversationAdapter = new ListConversationAdapter(getActivity(),imgageGetter,getFragmentManager());
        }
        listView.setAdapter(conversationAdapter);
    }

    /**
     * 得到最后一次打开的组的id
     * @return
     */
    private int getCurrentGroupId() {
        int id = -1;
        id = getActivity().getSharedPreferences(Constants.SP_USER,0).getInt(Constants.PRE_GROUP,-1);
        return id;
    }

    /**
     * 获得是否进行实时语音标识
     * @return
     */
    public boolean getRealTimePlay() {
        if(groupEntity == null) return true;
        return groupEntity.getIsRealTimePlay();
    }

    private void initEvent() {
        btnSend.setOnClickListener(this);
        imgTroggle.setOnClickListener(this);
        imgHomeHide.setOnClickListener(this);
        tvTip.setOnClickListener(this);
        imgMessageType.setOnClickListener(this);
        imgAdd.setOnClickListener(this);
        imgPicture.setOnClickListener(this);
        imgAddress.setOnClickListener(this);
        imgFace.setOnClickListener(this);
        imgShoot.setOnClickListener(this);
        etSendMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() > 0) {
                    imgAdd.setVisibility(View.INVISIBLE);
                    btnSend.setVisibility(View.VISIBLE);
                } else {
                    imgAdd.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.INVISIBLE);
                }
            }
        });
        btnSounds.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //sendAsync();
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btnSounds.setImageBitmap(press);
                        soundPool.play(soundsId, 1, 1, 1, 0, 1);
                        sendSounds = true;
                        if (tvTip.getVisibility() == View.VISIBLE) {
                            String text = tvTip.getText().toString();
                            if (text.endsWith("正在说话...") || text.equals(tipConnectLose) ||
                                    text.equals(tipReconnecting)) {
                                sendSounds = false;
                                return false;
                            }
                        } else {
                            btnSounds.setImageResource(R.drawable.press);
                            mainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (sendSounds) {
                                        if (callback != null) {
                                            callback.send(MainActivity.SendAction.SOUNDS, null);
                                            tvTip.setVisibility(View.VISIBLE);
                                            tvTip.setText("我正在说话...");
                                            preTime=System.currentTimeMillis();
                                        }
                                    }

                                }
                            }, 200);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        curTime=System.currentTimeMillis();
                        seconds = (curTime-preTime);
                        btnSounds.setImageBitmap(up);
                        if (sendSounds && callback != null) callback.stopSendSounds();
                        sendSounds = false;
                        tvTip.setText("");
                        tvTip.setVisibility(View.GONE);;
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    /**
     * 设置同步字
     */
    private void sendAsync() {
        if(!sendAsy && groupEntity != null) {
            if(callback != null) callback.sendAsyncWord(groupEntity.getAsyncWord());
        }
    }

    /**
     * 设置同步字成功
     */
    public void asyncOk() {
        sendAsy = true;
        Toast.makeText(getActivity(),"设置同步字成功",Toast.LENGTH_SHORT).show();
    }

    private interface SoundsTimeCallbacks{
        public void onCalculateSoundsTime(long seconds);
    }

    public void setTimeCallbacks(SoundsTimeCallbacks timeCallbacks){
        this.timeCallbacks=timeCallbacks;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_face:
                if(imgFace.isSelected()){
                    imgFace.setSelected(false);
                    vp.setVisibility(View.GONE);
                    llDots.setVisibility(View.GONE);
                    if(imgAdd.isSelected()){
                        llOthersData.setVisibility(View.GONE);
                        imgAdd.setSelected(false);
                    }if(imgMessageType.isSelected()){
                        imgMessageType.setSelected(false);
                        etSendMessage.setVisibility(View.VISIBLE);
                        btnSounds.setVisibility(View.GONE);
                    }
                }
                else{
                    imgFace.setSelected(true);
                    vp.setVisibility(View.VISIBLE);
                    llDots.setVisibility(View.VISIBLE);
                    if(imgAdd.isSelected()){
                        llOthersData.setVisibility(View.GONE);
                        imgAdd.setSelected(false);
                    }
                    if(imgMessageType.isSelected()){
                        imgMessageType.setSelected(false);
                        etSendMessage.setVisibility(View.VISIBLE);
                        btnSounds.setVisibility(View.GONE);
                    }
                }
                break;
            case R.id.btn_send:
                sendText();
                break;
            case R.id.img_home_troggle:
                if(callback != null) callback.toggleMenu();
                break;
            case R.id.img_home_hide:
                if(getActivity() != null) {
                    if(popMenu == null) {
                        popMenu = new ContextPopMenu(getActivity(),top);
                        final ContextPopMenu.CallbackInContextMenu callbackInContextMenu =
                                new ContextPopMenu.CallbackInContextMenu() {
                                    @Override
                                    public void isRealTimePlay(boolean isPlay) {
                                        //设置是否进行实时语音
                                        if(groupEntity != null) {
                                            groupEntity.setIsRealTimePlay(isPlay);
                                        }else {
                                            Toast.makeText(getActivity(),"请先建组",Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void showGroup() {
                                        if(groupEntity == null) {
                                            Toast.makeText(getActivity(),"请先建组",Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        showGeoupDetail();
                                    }

                                    @Override
                                    public void exitGroup() {
                                        if(groupEntity == null) {
                                            Toast.makeText(getActivity(),"请先建组",Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        //退出改组
                                        tvTitle.setText("公共频道");
                                        groupEntity = null;
                                        currentGroupId = -1;
                                        if(callback != null) callback.setMyId(-1);
                                        myId = -1;
                                        dataLists.clear();
                                        conversationAdapter.updateData(dataLists);
                                    }

                                    @Override
                                    public void reset() {
                                        //复位
                                        if(callback != null) {
                                            callback.resetFromH();
                                        }
                                    }
                                };
                        popMenu.setCallBack(callbackInContextMenu);
                    }
                    popMenu.show();
                }
                break;
            case R.id.tv_tip_home:
                if(tvTip.getText().toString().equals(tipConnectLose)) {
                    if(callback != null) {
                        //重新连接设备
                        callback.reconnectDevice();
                        tvTip.setText(tipReconnecting);
                        //5秒之后还没有连上即显示连接失败
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(tvTip.getVisibility() == View.VISIBLE &&
                                        tvTip.getText().toString().equals(tipReconnecting)) {
                                    tvTip.setText(tipConnectLose);
                                }
                            }
                        },5000);
                    }
                }
                break;
            case R.id.img_sounds_text:
                //click to change send message type
                if(imgMessageType.isSelected()) {
                    imgMessageType.setSelected(false);
                    etSendMessage.setVisibility(View.VISIBLE);
                    btnSounds.setVisibility(View.GONE);
                    imgFace.setVisibility(View.VISIBLE);
                }else {
                    imgFace.setVisibility(View.INVISIBLE);
                    imgMessageType.setSelected(true);
                    etSendMessage.setVisibility(View.INVISIBLE);
                    btnSounds.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.INVISIBLE);
                    imgAdd.setVisibility(View.VISIBLE);
                    if(imgAdd.isSelected()){
                        imgAdd.setSelected(false);
                        llOthersData.setVisibility(View.GONE);
                    }
                    else if(imgFace.isSelected()){
                        imgFace.setSelected(false);
                        vp.setVisibility(View.GONE);
                        llDots.setVisibility(View.GONE);
                    }
                }
                break;
            case R.id.img_other:
                //want to send other data,address or picture
                if(imgAdd.isSelected()) {
                    imgAdd.setSelected(false);
                    llOthersData.setVisibility(View.GONE);
                }else {
                    imgAdd.setSelected(true);
                    llOthersData.setVisibility(View.VISIBLE);
                    if(imgFace.isSelected()){
                        vp.setVisibility(View.GONE);
                        llDots.setVisibility(View.GONE);
                        imgFace.setSelected(false);
                    }
                    if(imgMessageType.isSelected()){
                        imgMessageType.setSelected(false);
                        etSendMessage.setVisibility(View.VISIBLE);
                        btnSounds.setVisibility(View.GONE);
                        imgFace.setVisibility(View.VISIBLE);
                    }
                }
                break;
            case R.id.img_home_shoot:
                //拍照发送
                openCamera();
                break;
            case R.id.img_home_picture:
                //want to send picture
//                ImagesFragment imagesFragment = ImagesFragment.getInstance(REQUEST_HOME);
//                imagesFragment.setTargetFragment(HomeFragment.this,REQUEST_HOME);
//                getFragmentManager().beginTransaction().replace(R.id.frame_content,
//                        imagesFragment)
//                        .addToBackStack(null)
//                        .commit();
                openCapture();
                break;
            case R.id.img_home_address:
                //want to send address
                Intent intent = new Intent();
                intent.setClass(getActivity(),LocationActivity.class);
                getActivity().startActivityForResult(intent, REQUEST_LOCATION);
                break;
            default:
                break;
        }
    }

    /**
     * 打开系统图库
     */
    private void openCapture() {
        Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(i.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    /**
     * 更新发送图片的进度
     * @param percent
     */
    public void upteImageProgress(final int percent) {
        mainHandler.obtainMessage(UPDATE_IMAGE,percent,-1,null).sendToTarget();
    }

    /**
     * callback in MyListView
     */
    @Override
    public void onLoad() {
        if(groupEntity == null) listView.loadComplete();
        try {
            final long lastData = dataLists.get(0).getDateTime();
            PoolThreadUtil.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    loadConverationData(lastData, 20, true);
                }
            });
        }catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
            dbManager.saveMessage();
    }


    public void setCallback(CallbackInHomeFragment callback) {
        this.callback = callback;
    }

    /**
     * send text
     */
    private void sendText() {
        //sendAsync();
        Editable editable = editableFactory.newEditable(etSendMessage.getText());
        String message = Html.toHtml(editable);
        message = message.replace("<p dir=\"ltr\">","");
        message = message.replace("</p>", "");
        Log.e("sendText", message);
        if(!TextUtils.isEmpty(message)) {
            if(callback != null) {
                callback.send(MainActivity.SendAction.Words,message);
            }
        }
    }

    /**
     * 显示收到的消息
     * @param tye 0 语音消息
     *            1 文字消息
     *            2 地址消息
     *            3 图片消息
     */
    public void receivingData(int tye,String data,int memberId,long soundsReceivingTime) {
        ConversationData receiveData = null;
        Drawable drawable = null;
        if(groupEntity != null) {
            for(int i = 0;i < groupEntity.getMembers().size();i++) {
                GroupMember member = groupEntity.getMembers().get(i);
                if(member.getId() == memberId) {
                    if(tye == 0 && data == null) {
                        if(tvTip.getVisibility() == View.VISIBLE && tvTip.getText().toString().endsWith("正在说话...")) {
                            tvTip.setText("");
                            if(callback != null) callback.stopSendSounds();
                        }
                        tvTip.setVisibility(View.VISIBLE);
                        tvTip.setText(member.getName() + "正在说话...");
                        btnSounds.setEnabled(false);
                        return;
                    }
                    drawable = member.getDrawable();
                    break;
                }
            }
        }else {
            drawable = drawableDef;
        }
        long time = new Date().getTime();
        Bitmap recevBitmap = null;
        switch (tye) {
            case 0:
                if(data != null) {
                    if(soundsReceivingTime>500||soundsReceivingTime<30000)
                    receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_SOUNDS,
                            data,soundsReceivingTime);
                }
                break;
            case 1:
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_TEXT,
                        data, null);
                break;
            case 2:
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_ADDRESS,
                        null, null);
                receiveData.setAddress(data);
                break;
            case 3:
                recevBitmap = getBitmapFromText(data);
                if(recevBitmap != null) {
                    receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_PIC,
                            null, recevBitmap);
                }
                break;
        }
        if(receiveData == null) return;
        receiveData.setDateTime(time);
        receiveData.setPhotoDrawable(drawable);
        String receTime = checkDataTime(time, true);
        if(receTime != null) {
            ConversationData timeData = new ConversationData(ListConversationAdapter.ConversationType.TIME, receTime);
            dataLists.add(timeData);
        }
        dataLists.add(receiveData);
        conversationAdapter.updateData(dataLists);
        listView.setSelection(conversationAdapter.getCount() - 1);
        Object oj = recevBitmap != null ? recevBitmap : data;
        //接受语音，检查是否保存组语音信息
        if(groupEntity == null) return;
        if (!(tye == 0 && !groupEntity.getIsSaveSoundOfGroup())) {
            saveMessage(oj,tye,memberId,time);
        }
        recevBitmap = null;
    }

    /**
     * according the last data's time to deciding show time message or not
     */
    private String checkDataTime(long timeStamp,boolean compareWithPre) {
        long currentMills = System.currentTimeMillis();
        final StringBuilder result = new StringBuilder();
        Date now = new Date(currentMills);
        Date other = new Date(timeStamp);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentMills);
        int currentYear = cal.get(Calendar.YEAR);
        cal.setTimeInMillis(timeStamp);
        int preYear = cal.get(Calendar.YEAR);
        int isPreYears = currentYear - preYear;
        if(isPreYears > 0) {
            result.append(preYear).append("年")
                    .append(cal.get(Calendar.MONTH))
                    .append("月").append(cal.get(Calendar.DAY_OF_MONTH))
                    .append("日 ").append(cal.get(Calendar.HOUR_OF_DAY))
                    .append(":").append(cal.get(Calendar.MINUTE));
        }else {
            cal.setTimeInMillis(currentMills);
            int toDay = cal.get(Calendar.DAY_OF_YEAR);
            cal.setTimeInMillis(timeStamp);
            int preDay = cal.get(Calendar.DAY_OF_YEAR);
            int days = toDay - preDay;

            switch (days) {
                case 0:
                    if(!compareWithPre) break;
                    if(dataLists.size() > 1) {
                        long pre = -1;
                        int index = dataLists.size() -1;
                        while (index >= 0) {
                            if(dataLists.get(index).getDateTime() != 0) {
                                pre = dataLists.get(index).getDateTime();
                                break;
                            }
                            index--;
                        }
                        if(pre != -1) {
                            double mills =  timeStamp - pre;
                            if(mills < 60000) {
                                return null;
                            }
                        }
                    }
                    break;
                case 1:
                    result.append("昨天");
                    break;
                case 2:
                    result.append("前天");
                    break;
                default:
                    result.append(cal.get(Calendar.MONTH) + 1);
                    result.append(cal.get(Calendar.DAY_OF_MONTH));
                    result.append(" ");
                    break;
            }
            result.append(getHourAndMin(timeStamp,cal));
        }
        if(compareWithPre) {
            saveMessage(result.toString(), 4, 0, timeStamp-1);
        }
        cal = null;
        return result.toString();
    }

    //get hour and minite by timeStamo
    private String getHourAndMin(long timeStamp,Calendar calendar) {
        StringBuilder result = new StringBuilder();
        calendar.setTimeInMillis(timeStamp);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        if(hours <= 5) {
            result.append("凌晨");
        }else if(hours <= 11) {
            result.append("早上");
        }else if(hours <= 14) {
            result.append("中午");
        }else if(hours <= 18) {
            result.append("下午");
        }else {
            result.append("晚上");
        }
        result.append(calendar.get(Calendar.HOUR))
                .append(":").append(calendar.get(Calendar.MINUTE));
        return result.toString();
    }


    /**
     * 已经接收到整个语音信息
     * @param sounds
     * @param memberId
     */
    public void endReceiveSounds(String sounds, int memberId,long receivingSoundsTime) {
        receivingData(0, sounds, memberId, receivingSoundsTime);
    }

    private void saveMessage(Object message,int type,int mid,long time) {
        if(groupEntity != null) {
            dbManager.readyMessage(message, type, mid, currentGroupId, time);
        }
    }

    /**
     * 接收信息后更新界面
     * 接收信息后更新界面
     * @param type 0 is sounds data,
     */
    public void endReceive(int type) {
        if(type == 0) {
           //stop to recevie sounds data
            tvTip.setText("");
            tvTip.setVisibility(View.GONE);
            btnSounds.setEnabled(true);
        }
    }

    private Bitmap getBitmapFromText(String data) {
        Bitmap recevBitmap = null;
        try {
            byte[] imgs = Base64.decode(data, Base64.DEFAULT);
            recevBitmap = BitmapFactory.decodeByteArray(imgs, 0, imgs.length);
        }catch (Exception e){
            if(getActivity() != null) {
                Toast.makeText(getActivity(),"接收图片失败",Toast.LENGTH_SHORT).show();
            }
            return null;
        }
        return recevBitmap;
    }
    /**
     * call if can send text by ble
     * @param sendText the text wait to be send
     */
    public void sendMessage(String sendText,MainActivity.SendAction sendAction) {
        if(TextUtils.isEmpty(sendText)) return;
        ConversationData subData = null;
        long time = new Date().getTime();
        Bitmap sendBitmap = null;
        switch (sendAction) {
            case Words:
                hideSoft();
                etSendMessage.setText("");
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_TEXT,
                        sendText);
                break;
            case Address:
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_ADDRESS,
                        null);
                subData.setAddress(sendText);
                break;
            case Image:
                sendBitmap = getBitmapFromText(sendText);
                if(sendBitmap != null) {
                    subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_PIC,
                            null);
                    subData.setBitmap(sendBitmap);
                    //记住正在发送的图片的数据源
                    conversationData = subData;
                }
                break;
            case SOUNDS:
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_SOUNDS
                        ,sendText,seconds);
                break;
        }
        if(subData == null) return;
        subData.setDateTime(time);
        String sendTime = checkDataTime(time,true);
        if(sendTime != null) {
            ConversationData timeData = new ConversationData(ListConversationAdapter.ConversationType.TIME,sendTime);
            dataLists.add(timeData);
        }
        dataLists.add(subData);
        conversationAdapter.updateData(dataLists);
        listView.setSelection(conversationAdapter.getCount() - 1);
        Object object = sendBitmap == null ? sendText : sendBitmap;
        //发送语音，检查是否保存组语音消息
        if(groupEntity == null) return;
        if(!(sendAction.ordinal() == 0 && !groupEntity.getIsSaveSoundOfGroup())) {
            saveMessage(object, sendAction.ordinal(), myId, time);
        }
        sendBitmap = null;
    }

    /**
     * hide the softkey
     */
    private void hideSoft() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSendMessage.getWindowToken(), 0);
    }

    /**
     * call when starting recording sounds
     */
    public void startSendingSounds() {
        //btnSounds.setText(getString(R.string.recording_sound));
    }

    /**
     * called to check the viewpager is touched or not
     * @param touchX
     * @param touchY
     * @return
     */
    public boolean checkTouch(int touchX, int touchY) {
        vp.getGlobalVisibleRect(rect);
        if(rect.contains(touchX,touchY)) {
            //tell parent do not intercept touch event
            vp.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        }
        btnSounds.getGlobalVisibleRect(rect);
        if(rect.contains(touchX,touchY)) {
            btnSounds.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    /**
     * 连接设备的结果
     * @param connect
     */
    public void deviceConnected(boolean connect) {
        if(mainHandler != null) {
            mainHandler.obtainMessage(UPDATE_BLESTATE, -1, -1, connect).sendToTarget();
        }
    }
    /**
     * 更新组
     * @param groupEntity
     */
    public void updateGroup(GroupEntity groupEntity) {

        this.groupEntity = groupEntity;
        myId = groupEntity.getTempId();
        if(callback != null) callback.setMyId(myId);
        showGroupTitle();
        if(getActivity() != null) {
            //将该id保存为最新打开的组的id
            GroupUtil.saveCurrentGid(currentGroupId,getActivity().getSharedPreferences(Constants.SP_USER,0));
        }
    }

    /**
     * 显示组的名称
     */
    private void showGroupTitle() {
        String name = groupEntity.getName();
        String title = name +"(" + groupEntity.getMembers().size() + "人" + ")";
        tvTitle.setText(title);
    }

    /**
     * 改变当前的组
     * @param gid
     */
    public void changeGroup(int gid) {
        //如果就是现在显示的组，不做任何处理
        if(gid == currentGroupId) return;
        //根据id加载新的组
        mainHandler.obtainMessage(CHANGE_GROUP,gid,-1,null).sendToTarget();
    }

    public interface CallbackInHomeFragment {
        /**
         * 请求发送文本或语音信息
         */
        void send(MainActivity.SendAction sendAction,String text);

        /**
         * 停止发送语音
         */
        void stopSendSounds();

        /**
         * 打开或关闭左侧菜单
         */
        void toggleMenu();

        /**
         * 重新连接设备
         */
        void reconnectDevice();

        /**
         * 设置我的id，方便在发送时标识自己
         * 设置同步字
         * @param tempId
         */
        void setMyId(int tempId);

        void sendAsyncWord(byte[] async);

        void openScroll(boolean open);

        /**
         * 检查蓝牙是否已开启
         * @return 0 蓝牙已开启
         *         1 蓝牙未开启
         *         2 服务还未绑定成功
         * */
        int isBleOpen();

        /**
         * 打开蓝牙
         */
        void openBle();

        /**
         * 自动连接绑定过的设备
         */
        void connectDeviceAuto();

        /**
         * 获取新建的组
         */
        GroupEntity getNewGroup();

        /**
         * 复位，测试使用
         */
        void resetFromH();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveCurrentGid();
        soundPool.release();
        expressions.clear();
        imgDots.clear();
        dataLists.clear();
        if(popMenu!=null){
            popMenu.setCallBack(null);
        }
        recycleBitmap();
    }

    /**
     * 回收Bitmap资源，释放内存
     */
    private void recycleBitmap() {
        if(up != null) {
            up.recycle();
        }
        if(press != null) {
            press.recycle();
        }
        if(backGroud != null) {
            backGroud.recycle();
        }
    }

    /**
     * 保存当前组的id
     */
    private void saveCurrentGid() {
        if(getActivity() == null) return;
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Constants.SP_USER,0).edit();
        editor.putInt(Constants.PRE_GROUP, currentGroupId);
        editor.apply();
    }


    /**
     * select a group from database by gid
     */
    private void loadGroup(final int gid) {
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                GroupEntity ge = dbManager.getAgroup(gid);
                if (ge != null) {
                    mainHandler.obtainMessage(LOAD_GROUP, -1, -1, ge).sendToTarget();
                }
            }
        });
    }

    /**
     *
     * @param timeStamp message send or received time
     * @param limits get max data from db
     * @return
     */
    private void loadConverationData(long timeStamp,int limits,boolean isLoad) {
        List<ConversationData> preDatas = dbManager.getConversationData(currentGroupId,myId,timeStamp,limits);
        if(isLoad) {
            loadComplete();
        }
        if(preDatas != null && preDatas.size() > 0) {
            if(Constants.DEBUG) {
                Log.e("loadConverationData","获取到组的聊天记录");
            }
            for(int i = 0;i < preDatas.size();i++) {
                int mid = preDatas.get(i).getMid();
                for(int j = 0;j < groupEntity.getMembers().size();j++) {
                    if(groupEntity.getMembers().get(j).getId() == mid) {
                        preDatas.get(i).setPhotoDrawable(groupEntity.getMembers().get(j).getDrawable());
                        break;
                    }
                }
                if(preDatas.get(i).getConversationType() == ListConversationAdapter.ConversationType.TIME) {
                    //重新计算时间
                    preDatas.get(i).setContent(checkDataTime(
                            preDatas.get(i).getDateTime(),false
                    ));
                }
            }
            mainHandler.obtainMessage(LOAD_CONVERDATA,-1,-1,preDatas).sendToTarget();
        }
    }

    private void loadComplete() {
        mainHandler.sendEmptyMessage(LOAD_COMPELET);
    }

    //打开相机拍照
    private String takePath;    //拍照图片的存储路径
    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePicture.resolveActivity(getActivity().getPackageManager()) != null) {
            takePath = getActivity().getExternalFilesDir(null) +"/" + System.currentTimeMillis() + ".jpg";
            Uri imageUri = Uri.fromFile(new File(takePath));
            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePicture, REQUEST_IMAGE_CPTURE);
        }
    }

    /**
     * 压缩图片并发送
     */
    private void getImageToSend() {
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = ImageUtil.createImageThumbnail(sendImgagePath,imgSendSize);
                if(bitmap == null) return;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int options = 100;
                bitmap.compress(Bitmap.CompressFormat.PNG,options,outputStream);
                while (outputStream.toByteArray().length / 1024 > 60) {
                    outputStream.reset();
                    options -= 10;
                    bitmap.compress(Bitmap.CompressFormat.PNG,options,outputStream);
                }
                String imgData = Base64.encodeToString(outputStream.toByteArray(),Base64.DEFAULT);
                mainHandler.obtainMessage(SEND_IMG,-1,-1,imgData).sendToTarget();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_HOME && resultCode == Activity.RESULT_CANCELED && data != null) {
            getFragmentManager().popBackStackImmediate();
            sendImgagePath = data.getStringExtra(Constants.PHOTO_PATH);
            if (sendImgagePath == null) return;
            getImageToSend();
        } else if (requestCode == REQUEST_CONTEXT_MENU) {
            switch (resultCode) {
                case 1:
                    //to look group
                    if (groupEntity != null && groupEntity.getMembers().size() > 0) {
                        if (callback != null) {
                            callback.openScroll(false);
                        }
                        Fragment groupFragment = GroupDetailFragment.getInstance(groupEntity);
                        groupFragment.setTargetFragment(HomeFragment.this, REQUEST_GROUP_DETAIL);
                        getFragmentManager().beginTransaction().replace(R.id.frame_content,
                                groupFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                    break;
                case 2:
                    //unplay sounds
                    break;
            }
        } else if (requestCode == REQUEST_GROUP_DETAIL) {
            switch (resultCode) {
                case 0:
                    //clear chat records
                    deleteMessage(currentGroupId);//调用下文实现的方法删除聊天记录
                    Toast.makeText(getActivity(), "成功删除聊天记录", Toast.LENGTH_SHORT).show();
                    dataLists.clear();//删除聊天界面上的聊天记录
                    conversationAdapter.updateData(dataLists);
                    break;
                case 1:
                    //open scroll
                    if (callback != null) callback.openScroll(true);
                    break;
            }
        } else if (requestCode == REQUEST_IMAGE_CPTURE && resultCode == Activity.RESULT_OK) {
            sendImgagePath = takePath;
            if (sendImgagePath == null) return;
            getImageToSend();
        } else if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            try {
                String picturePath = ImageUtil.getImgPathFromIntent(data,getActivity());
                if(picturePath != null) {
                    sendImgagePath = picturePath;
                    getImageToSend();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    public  void showGeoupDetail() {//回调接口的实现  实例化查看组的类
        if (groupEntity != null && groupEntity.getMembers().size() > 0) {
            Fragment groupFragment = GroupDetailFragment.getInstance(groupEntity);
            groupFragment.setTargetFragment(HomeFragment.this, REQUEST_GROUP_DETAIL);
            getFragmentManager().beginTransaction().replace(R.id.frame_content,
                    groupFragment)
                    .addToBackStack(null)
                    .commit();

        }
    }
    public void deleteMessage(int gid){//对数据库操作实现删除聊天记录的功能
        dbManager.deleteMessage(gid);
    }

    public static final int REQUEST_LOCATION = 302;
    public static final int REQUEST_HOME = 303;
    public static final int REQUEST_CONTEXT_MENU = 304;
    public static final int REQUEST_GROUP_DETAIL = 305;
    public static final int REQUEST_IMAGE_CPTURE = 306; //请求拍照
    public static final int RESULT_LOAD_IMAGE = 307;    //请求系统图库
    public static final String EXTRA_LOCATION = "address";
    private static final int LOAD_GROUP = 0; //加载到一个组
    private static final int CHANGE_GROUP = 1;   //改变组
    private static final int UPDATE_IMAGE = 2;   //更新图片发送进度
    private static final int UPDATE_BLESTATE = 3;   //更新图片发送进度
    private static final int LOAD_CONVERDATA = 4;    //加载到聊天信息
    private static final int LOAD_COMPELET = 5;    //加载聊天信息完毕
    private static final int SEND_IMG = 6;    //发送图片
}
