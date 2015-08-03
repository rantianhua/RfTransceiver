package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Image;
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
import android.widget.ImageButton;
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
    //当前视图的标题
    private String homeTitle;
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

    private boolean isPublicChannel = false;    //标识是否在公共频道
    private Drawable drawableDef;   //默认头像
    //显示正在处理一些事情
    private ProgressDialog pd;
    //标识是否需要自动连接设备
    private boolean needConnecAuto = false;
    //正在发送图片的数据源
    private ConversationData conversationData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHandler();

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
        soundsId = soundPool.load(getActivity(),R.raw.btn_down,1);
        pd = new ProgressDialog(getActivity());
        //打开蓝牙
        openBle();
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
                        break;
                    case CHANGE_GROUP:
                        //取消加组或建组
                        int gid = msg.arg1;
                        if(gid == currentGroupId) return false;
                        currentGroupId = gid;
                        dataLists.clear();
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
        ButterKnife.inject(this,v);

        BitmapFactory.Options op1 = new BitmapFactory.Options();
        op1.inSampleSize = 2;
        press = BitmapFactory.decodeResource(getResources(),R.drawable.press,op1);

        BitmapFactory.Options op2 = new BitmapFactory.Options();
        op2.inSampleSize = 2;
        up = BitmapFactory.decodeResource(getResources(),R.drawable.up,op2);

        imgMessageType.setSelected(true);
        listView.setInterface(this);
        btnSounds.setImageBitmap(up);

        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 4;
        Bitmap backGround = BitmapFactory.decodeResource(getResources(),R.drawable.chatbackground,op);
        listView.setBackground(new BitmapDrawable(backGround));
        conversationAdapter = new ListConversationAdapter(getActivity(),imgageGetter,getFragmentManager());
        listView.setAdapter(conversationAdapter);
        conversationAdapter.updateData(dataLists);
        if(!TextUtils.isEmpty(homeTitle)) {
            tvTitle.setText(homeTitle);
            homeTitle = null;
            isPublicChannel = false;
        }else {
            tvTitle.setText("处于公共频道");
            isPublicChannel = true;
            initDefDrawable();
        }
    }

    /**
     * 实例化默认图片
     */
    private void initDefDrawable() {
        int size = 150 * 150;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.photo);
        drawableDef = new CircleImageDrawable(bitmap);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(groupEntity == null) {
            isPublicChannel = true;
            if(getCurrentGroupId() != -1) {
                loadGroup(currentGroupId);
                Constants.GROUPID = currentGroupId;
            }else {
                Log.e("onViewCreated","没有组要加载");
            }
        }else {
            isPublicChannel = false;
            updateGroup(groupEntity);

        }
    }

    /**
     * get current group id
     * @return
     */
    private int getCurrentGroupId() {
        if(currentGroupId == -1) {
            try {
                currentGroupId = getActivity().getSharedPreferences(Constants.SP_USER,0).getInt(Constants.PRE_GROUP,-1);

            }catch (Exception e ){

            }
        }
        return currentGroupId;
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
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btnSounds.setImageBitmap(press);
                        //btnSounds.setSelected(true);
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
                    case MotionEvent.ACTION_CANCEL:
                        btnSounds.setImageBitmap(up);
                        return false;
                    case MotionEvent.ACTION_UP:
                        curTime=System.currentTimeMillis();
                        seconds = (curTime-preTime);
                        BitmapFactory.Options oP = new BitmapFactory.Options();
                        oP.inSampleSize = 2;
                        Bitmap up = BitmapFactory.decodeResource(getResources(),R.drawable.up,oP);

                        btnSounds.setImageBitmap(up);

                        //btnSounds.setSelected(false);
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
                        ContextPopMenu.CallbackInContextMenu callbackInContextMenu =
                                new ContextPopMenu.CallbackInContextMenu() {
                                    @Override
                                    public void isRealTimePlay(boolean isPlay) {
                                        //设置是否进行实时语音
                                        if(groupEntity != null) {
                                            groupEntity.setIsRealTimePlay(isPlay);
                                        }
                                    }
                                };
                        popMenu.setCallBack(callbackInContextMenu);
                        showG();
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
                    loadConverationData(currentGroupId, myId, lastData, 20, true);
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
     * is receiving sounds or text data
     * @param tye 0 is sounds data
     *            1 is words data
     *            2 is address data
     *            3 is image data
     */
    public void receivingData(int tye,String data,int memberId,long soundsReceivingTime) {
        ConversationData receiveData = null;
        Drawable drawable = null;
        if(groupEntity != null) {
            for(int i = 0;i < groupEntity.getMembers().size();i++) {
                GroupMember member = groupEntity.getMembers().get(i);
                if(member.getId() == memberId) {
                    if(tye == 0 && data == null) {
                        tvTip.setVisibility(View.VISIBLE);
                        tvTip.setText(member.getName() + "正在说话...");
                        btnSounds.setEnabled(false);
                        return;
                    }
                    drawable = member.getDrawable();
                    break;
                }
            }
        }else if(isPublicChannel) {
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
        String receTime = checkDataTime(time,true);
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
     * receive whole sounds ,cast to String to save in db
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
     * after reveive all data
     * @param type 0 is sounds data,
     */
    public void endReceive(int type) {
        if(type == 0 && !sendSounds) {
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
            mainHandler.obtainMessage(UPDATE_BLESTATE,-1,-1,connect).sendToTarget();
        }
    }
    /**
     * update the group of talk
     * @param groupEntity
     */
    public void updateGroup(final GroupEntity groupEntity) {

        this.groupEntity = groupEntity;
        if(groupEntity == null) {
            isPublicChannel = true;
            return;
        }
        myId = groupEntity.getTempId();
        if(callback != null) callback.setMyId(groupEntity.getTempId());
        String name = groupEntity.getName();
        homeTitle = name
                +"(" + groupEntity.getMembers().size() + "人" + ")";
        if(tvTitle != null) {
            tvTitle.setText(homeTitle);
        }
        isPublicChannel = false;
    }

    /**
     * change group by gid
     * @param gid
     */
    public void changeGroup(int gid) {
        if(gid == currentGroupId) return;
        mainHandler.obtainMessage(CHANGE_GROUP,gid,-1,null).sendToTarget();
        if(getActivity() != null) {
            GroupUtil.saveCurrentGid(gid,getActivity().getSharedPreferences(Constants.SP_USER,0));
        }
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
         * @param tempId
         */
        void setMyId(int tempId);

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
    }

    @Override
    public void onDestroy() {
        saveCurrentGid();
        soundPool.release();
        super.onDestroy();
        expressions.clear();
        imgDots.clear();
        dataLists.clear();
        if(popMenu!=null){
            popMenu.setCallBack(null);
        }
        if(groupEntity != null) {
            GroupUtil.recycle(groupEntity.getMembers());
        }
    }

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
                    loadConverationData(gid, ge.getTempId(), -1, 20, false);
                }
            }
        });
    }

    /**
     *
     * @param gid the group's id
     * @param timeStamp message send or received time
     * @param limits get max data from db
     * @return
     */
    private void loadConverationData(int gid,int myid,long timeStamp,int limits,boolean isLoad) {
        if(groupEntity == null) return;
        List<ConversationData> preDatas = dbManager.getConversationData(gid,myId,timeStamp,20);
        if(isLoad) {
            loadComplete();
        }
        if(preDatas != null) {
            for(int i = 0;i < preDatas.size();i++) {
                int mid = preDatas.get(i).getMid();
                for(int j = 0;j < groupEntity.getMembers().size();j++) {
                    if(groupEntity.getMembers().get(j).getId() == mid) {
                        preDatas.get(i).setPhotoDrawable(groupEntity.getMembers().get(j).getDrawable());
                        break;
                    }
                }
                if(preDatas.get(i).getConversationType() == ListConversationAdapter.ConversationType.TIME) {
                    //recaculate the time message
                    preDatas.get(i).setContent(checkDataTime(
                            preDatas.get(i).getDateTime(),false
                    ));
                }
            }
            dataLists.addAll(0,preDatas);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    conversationAdapter.updateData(dataLists);
                    listView.setSelection(dataLists.size()-1);
                }
            });
            preDatas = null;
        }
    }

    private void loadComplete() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listView.loadComplete();
            }
        });
    }

    //打开相机拍照
    private String takePath;    //拍照图片的存储路径
    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //ensure the process can handle with the return intent
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
                final String imgData = Base64.encodeToString(outputStream.toByteArray(),Base64.DEFAULT);
                if(callback != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.send(MainActivity.SendAction.Image, imgData);
                        }
                    });
                }
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
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
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
    public  void showG() {//回调接口的实现  实例化查看组的类
        ContextPopMenu.CallbackInPopMenue callbackInPopMenue = new ContextPopMenu.CallbackInPopMenue() {
            @Override
            public void showGroup() {
                if (groupEntity != null && groupEntity.getMembers().size() > 0) {
                    Fragment groupFragment = GroupDetailFragment.getInstance(groupEntity);
                    groupFragment.setTargetFragment(HomeFragment.this, REQUEST_GROUP_DETAIL);
                    getFragmentManager().beginTransaction().replace(R.id.frame_content,
                            groupFragment)
                            .addToBackStack(null)
                            .commit();

                }
            }
        };
        popMenu.setCallbackInPopMenue(callbackInPopMenue);
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
    public static final int LOAD_GROUP = 0; //加载到一个组
    public static final int CHANGE_GROUP = 1;   //改变组
    public static final int UPDATE_IMAGE = 2;   //更新图片发送进度
    public static final int UPDATE_BLESTATE = 3;   //更新图片发送进度
}
