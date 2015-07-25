package com.rftransceiver.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.LocationActivity;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.adapter.ListConversationAdapter;
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
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.DataPacketOptions;


import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.internal.ListenerClass;

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
    Button btnSounds;
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
    @InjectView(R.id.img_home_picture)
    ImageView imgPicture;
    @InjectView(R.id.img_home_address)
    ImageView imgAddress;
    @InjectView(R.id.rl_home_imgs_address)
    RelativeLayout rlOthersData;
    @InjectView(R.id.vp_home_expression)
    ViewPager vp;
    @InjectView(R.id.ll_dots_home)
    LinearLayout llDots;
    @InjectView(R.id.tv_title_content)
    TextView tvTitle;
    @InjectView(R.id.rl_top_home)
    RelativeLayout top;

    /**
     * the reference of callback interface
     */
    private CallbackInHomeFragment callback;

    private ListConversationAdapter conversationAdapter = null; //the adapter of listView

    private List<ConversationData> dataLists;

    /**
     * save all gridView filled with expressions
     */
    private List<GridView> expressions;

    /**
     * current displayed gridview's index in viewpager
     */
    private int currentEpIndex;

    /**
     * save all dots to indicate which gridview is selected in vp
     */
    private List<ImageView> imgDots;

    /**
     * to parse expression
     */
    private Html.ImageGetter imgageGetter;

    /**
     * a instance of a GroupEntity
     */
    private GroupEntity groupEntity;

    private Editable.Factory editableFactory = Editable.Factory.getInstance();

    private Rect rect = new Rect();

    private int imgSendSize;

    private String tipConnectLose,tipReconnecting,tipConnecSuccess,tipSendSounds,tipStopSounds;

    /**
     * decide send or not sent for touch event on btnSounds
     */
    private boolean sendSounds = false;

    /**
     * to manipulate database
     */
    private DBManager dbManager;

    private String homeTitle;

    private static final android.os.Handler mainHandler = new android.os.Handler(Looper.getMainLooper());

    /**
     * to play sounds for button click
     */
    private SoundPool soundPool;
    /**
     * the play sounds' id
     */
    private int soundsId;

    /**
     * my id in group
     */
    private int myId = -1;

    /**
     * current group's id in db
     */
    private int currentGroupId = -1;

    //private ContextMenuDialogFrag contextMenuDialogFrag;

    //要发送的图片的存储地址
    private String sendImgagePath;

    //顶部菜单栏的弹出菜单
    private ContextPopMenu popMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expressions = new ArrayList<>();
        imgDots = new ArrayList<>();
        initImageGetter();
//        imgSendSize = getResources().getDimensionPixelSize(R.dimen.img_data_height)
//             * getResources().getDimensionPixelSize(R.dimen.img_data_width);

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
     }

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
                    int epId = ExpressionUtil.epDatas.get(currentEpIndex).get(i);
                    insertExpression(epId,etSendMessage);
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
     * insert expression to EditText
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
        listView.setInterface(this);
        conversationAdapter = new ListConversationAdapter(getActivity(),imgageGetter,getFragmentManager());
        listView.setAdapter(conversationAdapter);
        conversationAdapter.updateData(dataLists);
        if(!TextUtils.isEmpty(homeTitle)) {
            tvTitle.setText(homeTitle);
            homeTitle = null;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(groupEntity == null) {
            if(getCurrentGroupId() != -1) {
                loadGroup(currentGroupId);
            }
        }else {
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

    private void initEvent() {
        btnSend.setOnClickListener(this);
        imgTroggle.setOnClickListener(this);
        imgHomeHide.setOnClickListener(this);
        tvTip.setOnClickListener(this);
        imgMessageType.setOnClickListener(this);
        imgAdd.setOnClickListener(this);
        imgPicture.setOnClickListener(this);
        imgAddress.setOnClickListener(this);

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
                        btnSounds.setSelected(true);
                        soundPool.play(soundsId, 1, 1, 1, 0, 1);
                        sendSounds = true;
                        btnSounds.setText(tipStopSounds);
                        if (tvTip.getVisibility() == View.VISIBLE) {
                            String text = tvTip.getText().toString();
                            if (text.endsWith("正在说话...") || text.equals(tipConnectLose) ||
                                    text.equals(tipReconnecting)) {
                                sendSounds = false;
                                return false;
                            }
                        } else {
                            mainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (sendSounds) {
                                        if (callback != null) {
                                            callback.send(MainActivity.SendAction.SOUNDS, null);
                                            tvTip.setVisibility(View.VISIBLE);
                                            tvTip.setText("我正在说话...");
                                        }
                                    }
                                }
                            }, 1000);
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return false;
                    case MotionEvent.ACTION_UP:
                        btnSounds.setSelected(false);
                        if (sendSounds && callback != null) callback.stopSendSounds();
                        sendSounds = false;
                        btnSounds.setText(tipSendSounds);
                        tvTip.setText("");
                        tvTip.setVisibility(View.GONE);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                sendText();
                break;
            case R.id.img_home_troggle:
                if(callback != null) callback.toggleMenu();
                break;
            case R.id.img_home_hide:
                //click hide menu on the top
//                if(contextMenuDialogFrag == null) {
//                    contextMenuDialogFrag = new ContextMenuDialogFrag();
//                    contextMenuDialogFrag.setTargetFragment(HomeFragment.this,REQUEST_CONTEXT_MENU);
//                }
//                try {
//                    contextMenuDialogFrag.show(getFragmentManager(),"contextMenu");
//                }catch (Exception e) {
//
//                }
                if(getActivity() != null) {
                    if(popMenu == null) {
                        popMenu = new ContextPopMenu(getActivity());
                    }
                    popMenu.show(top);
                }
                break;
            case R.id.tv_tip_home:
                if(tvTip.getText().toString().equals(tipConnectLose)) {
                    if(callback != null) {
                        callback.reconnectDevice();
                        tvTip.setText(tipReconnecting);
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
                    btnSounds.setVisibility(View.INVISIBLE);
                }else {
                    imgMessageType.setSelected(true);
                    etSendMessage.setVisibility(View.INVISIBLE);
                    btnSounds.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.INVISIBLE);
                    imgAdd.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.img_other:
                //want to send other data,address or picture
                if(imgAdd.isSelected()) {
                    imgAdd.setSelected(false);
                    rlOthersData.setVisibility(View.GONE);
                }else {
                    imgAdd.setSelected(true);
                    rlOthersData.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.img_home_picture:
                //want to send picture
                ImagesFragment imagesFragment = ImagesFragment.getInstance(REQUEST_HOME);
                imagesFragment.setTargetFragment(HomeFragment.this,REQUEST_HOME);
                getFragmentManager().beginTransaction().replace(R.id.frame_content,
                        imagesFragment)
                        .addToBackStack(null)
                        .commit();
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

    public void upteImageProgress(final int percent) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                conversationAdapter.updateImgageProgress(percent);
            }
        });
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
    public void receivingData(int tye,String data,int memberId) {
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
        }
        long time = new Date().getTime();
        Bitmap recevBitmap = null;
        switch (tye) {
            case 0:
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_SOUNDS,
                        data);
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
            ConversationData timeData = new ConversationData(ListConversationAdapter.ConversationType.TIME,receTime);
            dataLists.add(timeData);
        }
        dataLists.add(receiveData);
        conversationAdapter.updateData(dataLists);
        listView.setSelection(conversationAdapter.getCount() - 1);
        Object oj = recevBitmap != null ? recevBitmap : data;
        saveMessage(oj,tye,memberId,time);
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
        result.append(calendar.get(Calendar.HOUR_OF_DAY))
            .append(":").append(calendar.get(Calendar.MINUTE));
        return result.toString();
    }


    /**
     * receive whole sounds ,cast to String to save in db
     * @param sounds
     * @param memberId
     */
    public void endReceiveSounds(String sounds, int memberId) {
        receivingData(0, sounds, memberId);
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
                }
                break;
            case SOUNDS:
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_SOUNDS
                        ,sendText);
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
        saveMessage(object,sendAction.ordinal(),myId,time);
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

    public void deviceConnected(final boolean connect) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    /**
     * update the group of talk
     * @param groupEntity
     */
    public void updateGroup(final GroupEntity groupEntity) {
        this.groupEntity = groupEntity;
        if(groupEntity == null) return;
        myId = groupEntity.getTempId();
        if(callback != null) callback.setMyId(groupEntity.getTempId());
        final String name = groupEntity.getName();
        homeTitle = name
                +"(" + groupEntity.getMembers().size() + "人" + ")";
        if(tvTitle != null) {
            if(!this.isAdded()) return;
            Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvTitle.setText(homeTitle);
                }
            });
        }
    }

    /**
     * change group by gid
     * @param gid
     */
    public void changeGroup(int gid) {
        if(gid == currentGroupId) return;
        dataLists.clear();
        conversationAdapter.updateData(dataLists);
        groupEntity = null;
        loadGroup(gid);
        currentGroupId = gid;

    }

    public interface CallbackInHomeFragment {
        /**
         * send text or sound message
         */
        void send(MainActivity.SendAction sendAction,String text);

        /**
         * stop send sounds
         */
        void stopSendSounds();

        /**
         * call to open or close menu
         */
        void toggleMenu();

        /**
         * call to reconnect device
         */
        void reconnectDevice();

        /**
         * call to reset cms
         */
        void resetCms();

        void setMyId(int tempId);

        void openScroll(boolean open);
    }

    @Override
    public void onDestroy() {
        saveCurrentGid();
        soundPool.release();
        super.onDestroy();
        expressions.clear();
        imgDots.clear();
        dataLists.clear();
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
                    updateGroup(ge);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_HOME && resultCode == Activity.RESULT_CANCELED && data != null) {
            getFragmentManager().popBackStackImmediate();
            sendImgagePath = data.getStringExtra(Constants.PHOTO_PATH);
            if(sendImgagePath == null) return;
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
        }else if(requestCode == REQUEST_CONTEXT_MENU) {
            switch (resultCode) {
                case 0:
                    //to reset scm
                    if(callback != null) {
                        callback.resetCms();
                        endReceive(0);
                        btnSounds.setEnabled(true);
                    }
                    break;
                case 1:
                    //to look group
                    if(groupEntity != null && groupEntity.getMembers().size() > 0) {
                        if(callback != null) {
                            callback.openScroll(false);
                        }
                        Fragment groupFragment = GroupDetailFragment.getInstance(groupEntity);
                        groupFragment.setTargetFragment(HomeFragment.this,REQUEST_GROUP_DETAIL);
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
        }else if(requestCode == REQUEST_GROUP_DETAIL) {
            switch (resultCode) {
                case 0:
                    //clear chat records
                    break;
                case 1:
                    //open scroll
                    if(callback != null) callback.openScroll(true);
                    break;
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static final int REQUEST_LOCATION = 302;
    public static final int REQUEST_HOME = 303;
    public static final int REQUEST_CONTEXT_MENU = 304;
    public static final int REQUEST_GROUP_DETAIL = 305;
    public static final String EXTRA_LOCATION = "address";
}
