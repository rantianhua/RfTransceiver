package com.rftransceiver.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.activity.LocationActivity;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;
import com.rftransceiver.util.ExpressionUtil;


import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-14.
 */
public class HomeFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.listview_conversation)
    ListView listView;
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
    @InjectView(R.id.tv_reset_home)
    TextView tvReset;
    @InjectView(R.id.tv_group_info)
    TextView tvSeeGroup;
    @InjectView(R.id.tv_mute_home)
    TextView tvMute;
    @InjectView(R.id.tv_title_content)
    TextView tvTitle;

    /**
     * the reference of callback interface
     */
    private CallbackInHomeFragment callback;

    private ListConversationAdapter conversationAdapter = null; //the adapter of listView

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
     * the object animation for right menu
     */
    private AnimatorSet translateIn,translateOut;
    private float transOffset;

    private Editable.Factory editableFactory = Editable.Factory.getInstance();

    private Rect rect = new Rect();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expressions = new ArrayList<>();
        imgDots = new ArrayList<>();
        initImageGetter();
        transOffset = getResources().getDisplayMetrics().density * 80 + 0.5f;
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
                drawable.setBounds(0,0,40,
                        40);
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

        conversationAdapter = new ListConversationAdapter(getActivity(),imgageGetter);
        listView.setAdapter(conversationAdapter);
    }

    private void initEvent() {
        btnSend.setOnClickListener(this);
        btnSounds.setOnClickListener(this);
        imgTroggle.setOnClickListener(this);
        imgHomeHide.setOnClickListener(this);
        tvTip.setOnClickListener(this);
        imgMessageType.setOnClickListener(this);
        imgAdd.setOnClickListener(this);
        imgPicture.setOnClickListener(this);
        imgAddress.setOnClickListener(this);
        tvReset.setOnClickListener(this);
        tvMute.setOnClickListener(this);
        tvSeeGroup.setOnClickListener(this);

        etSendMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().length() > 0) {
                    imgAdd.setVisibility(View.INVISIBLE);
                    btnSend.setVisibility(View.VISIBLE);
                }else{
                    imgAdd.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                //Log.e("onClick",filterFromHtml(etSendMessage.getText().toString()));
//                if(MainActivity.findWriteCharac) {
//                    sendText();
//                }else {
//                    bleLose();
//                }
                sendText();
                break;
            case R.id.btn_sounds:
                if(btnSounds.getText().equals(getString(R.string.record_sound))) {
//                    if(MainActivity.findWriteCharac) {
//                        sendSounds();
//                    }else {
//                        bleLose();
//                    }
                    sendSounds();
                }
                else if(btnSounds.getText().equals(getString(R.string.recording_sound))) {
                    if(callback != null) callback.stopSendSounds();
                    btnSounds.setText(getString(R.string.record_sound));
                }
                break;
            case R.id.img_home_troggle:
                if(callback != null) callback.toggleMenu();
                break;
            case R.id.img_home_hide:
                //click hide menu on the top
                imgHomeHide.setClickable(false);
                if(tvMute.getVisibility() == View.INVISIBLE) {
                    showRightMenu();
                }else {
                    hideRightMenu();
                }
                break;
            case R.id.tv_tip_home:
                if(tvTip.getText().toString().equals(getString(R.string.connection_lose))) {
                    if(callback != null) {
                        callback.reconnectDevice();
                        tvTip.setText(getString(R.string.reconnecting));
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

                break;
            case R.id.img_home_address:
                //want to send address
                Intent intent = new Intent();
                intent.setClass(getActivity(),LocationActivity.class);
                startActivity(intent);
                break;
            case R.id.tv_mute_home:
                break;
            case R.id.tv_group_info:

                break;
            case R.id.tv_reset_home:
                hideRightMenu();
                if(callback != null) {
                    callback.resetCms();
                }
                break;
            default:
                break;
        }
    }

    /**
     * show right menu with translate animation
     */
    private void showRightMenu() {
        initInAnimation();
        translateIn.start();
    }

    private void initInAnimation() {
        if(translateIn != null) return;
        translateIn = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        ObjectAnimator transTvResetIn = ObjectAnimator.ofFloat(tvReset,"translationX",transOffset,0.0f);
        transTvResetIn.setDuration(300);
        transTvResetIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                tvReset.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvGroupIn = ObjectAnimator.ofFloat(tvSeeGroup,"translationX",transOffset,0.0f);
        transTvGroupIn.setDuration(300);
        transTvGroupIn.setStartDelay(150);
        transTvGroupIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                tvSeeGroup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvMuteIn = ObjectAnimator.ofFloat(tvMute,"translationX",transOffset,0.0f);
        transTvMuteIn.setDuration(300);
        transTvMuteIn.setStartDelay(300);
        transTvMuteIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                tvMute.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                imgHomeHide.setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animators.add(transTvResetIn);
        animators.add(transTvGroupIn);
        animators.add(transTvMuteIn);
        translateIn.playTogether(animators);
    }

    /**
     * hide right menu with animation
     */
    private void hideRightMenu() {
        initOutAnimation();
        translateOut.start();
    }

    private void initOutAnimation() {
        if(translateOut != null) return;
        translateOut = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        ObjectAnimator transTvResetOut = ObjectAnimator.ofFloat(tvReset,"translationX",transOffset);
        transTvResetOut.setDuration(300);
        transTvResetOut.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                tvReset.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvGroupOut = ObjectAnimator.ofFloat(tvSeeGroup,"translationX",transOffset);
        transTvGroupOut.setStartDelay(150);
        transTvGroupOut.setDuration(300);
        transTvGroupOut.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                tvSeeGroup.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvMuteOut = ObjectAnimator.ofFloat(tvMute,"translationX",0.0f,transOffset);
        transTvMuteOut.setDuration(300);
        transTvMuteOut.setStartDelay(300);
        transTvMuteOut.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                tvMute.setVisibility(View.INVISIBLE);
                imgHomeHide.setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animators.add(transTvResetOut);
        animators.add(transTvGroupOut);
        animators.add(transTvMuteOut);
        translateOut.playTogether(animators);
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
        Log.e("sendText",message);
        message = message.replace("<p>","");
        message = message.replace("</p>", "");
        if(!TextUtils.isEmpty(message)) {
            if(callback != null) {
                callback.send(MainActivity.SendAction.TEXT,message);
            }
        }
    }

    /**
     * send sounds
     */
    private void sendSounds() {
        if(callback != null){
            callback.send(MainActivity.SendAction.SOUNDS,null);
        }
    }

    /**
     * is receiving sounds or text data
     * @param tye 0 is sounds data
     *            1 is text data
     */
    public void receivingData(int tye,String data) {
        if(tye == 0) {
            btnSounds.setText(getString(R.string.sounds_im));
            btnSounds.setClickable(false);
        }else if(tye == 1 ){
            ConversationData other = new ConversationData(ListConversationAdapter.ConversationType.Other,
                    data, BitmapFactory.decodeResource(getResources(),R.drawable.photo),0,"100m");
            conversationAdapter.addData(other);
            conversationAdapter.notifyDataSetChanged();
            listView.setSelection(conversationAdapter.getCount()-1);
        }
    }

    /**
     * after reveive all data
     * @param type 0 is sounds data,
     */
    public void endReceive(int type) {
        if(type == 0) {
            btnSounds.setText(getString(R.string.record_sound));
            btnSounds.setClickable(true);
        }
    }

    /**
     * call if can send text by ble
     * @param sendText the text wait to be send
     */
    public void sendText(String sendText) {
        hideSoft();
        etSendMessage.setText("");
        ConversationData me = new ConversationData(ListConversationAdapter.ConversationType.Me,
                sendText);
        conversationAdapter.addData(me);
        conversationAdapter.notifyDataSetChanged();
        listView.setSelection(conversationAdapter.getCount()-1);
        me = null;
    }

    private void hideSoft() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSendMessage.getWindowToken(), 0);
    }

    public void reset() {
        btnSounds.setText(getString(R.string.record_sound));
        btnSounds.setClickable(true);
        btnSend.setClickable(true);
    }

    /**
     * call when starting recording sounds
     */
    public void startSendingSounds() {
        btnSounds.setText(getString(R.string.recording_sound));
    }

    /**
     * called to check the viewpager is touched or not
     * @param touchX
     * @param touchY
     * @return
     */
    public boolean checkTouch(int touchX, int touchY) {
        if(tvMute.getVisibility() == View.VISIBLE) {
            tvReset.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            tvSeeGroup.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            tvMute.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            imgHomeHide.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            hideRightMenu();
            //do not dispath touch event
            return true;
        }
        vp.getGlobalVisibleRect(rect);
        if(rect.contains(touchX,touchY)) {
            //tell parent do not intercept touch event
            vp.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    /**
     * the connection of ble has cut down
     */
//    public void bleLose() {
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                tvTip.setVisibility(View.VISIBLE);
//                tvTip.setText(getString(R.string.connection_lose));
//            }
//        });
//    }

    public void deviceConnected() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTip.setText(getString(R.string.connect_success));
                tvTip.setVisibility(View.GONE);
            }
        });
    }

    /**
     * update the group of talk
     * @param groupEntity
     */
    public void updateGroup(GroupEntity groupEntity) {
        String name = groupEntity.getName();
        if(TextUtils.isEmpty(name)) {
            name = groupEntity.getMembers().get(0).getName();
        }
        tvTitle.setText(name
                +"(" + groupEntity.getMembers().size() + "人" + ")");
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        expressions = null;
        imgDots = null;
    }
}
