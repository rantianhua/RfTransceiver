package com.rftransceiver.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rftransceiver.R;
import com.rftransceiver.adapter.ContactsAdapter;
import com.rftransceiver.customviews.LetterView;
import com.rftransceiver.datasets.ContactsData;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.PoolThreadUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rth on 15-7-22.
 */
public class ContactsFragment extends Fragment implements ContactsAdapter.CallbackInContactsAdpter{

    @InjectView(R.id.letterview_contact)
    LetterView letterView;
    @InjectView(R.id.expand_list_contacts)
    ExpandableListView contacts;
    @InjectView(R.id.rl_loading_contacts)
    RelativeLayout rlLoading;
    @InjectView(R.id.img_top_left)
    ImageView imgBack;
    @InjectView(R.id.tv_title_left)
    TextView tvTitle;

    //提示选中字母的popwindow
    private PopupWindow showLetter;

    //显示字母
    private TextView tvLetter;

    //根视图
    private View contentView;

    private DBManager dbManager;

    //数据源
    private Map<String,List<ContactsData>> mapContacts;

    //适配器
    private ContactsAdapter adpter;

    private static final Handler mainHan = new Handler();

    private CallbackInContacts callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbManager = DBManager.getInstance(getActivity());
        mapContacts = new HashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_contacts,container,false);
        initView(contentView);
        initEvent();
        initPopWindow();
        loadContacts();
        return contentView;
    }

    private void initView(View view) {
        ButterKnife.inject(this, view);
        letterView.setListener(selectLetterListener);
        contacts.setGroupIndicator(null);
        contacts.setDivider(null);
        contacts.setSelector(android.R.color.transparent);
        contacts.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                return true;
            }
        });
        imgBack.setImageResource(R.drawable.back);
        tvTitle.setText(getString(R.string.contacts));
    }

    private void initEvent() {
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStackImmediate();
            }
        });
    }

    public void setCallback(CallbackInContacts callback) {
        this.callback = callback;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(callback != null) callback.openScorll(false);
    }

    private void loadContacts() {
        rlLoading.setVisibility(View.VISIBLE);
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                final List<ContactsData> contactDatas = dbManager.getContacts();
                if (contactDatas != null && contactDatas.size() > 0) {
                    for (int j = 0; j < letterView.letters.length; j++) {
                        String key = letterView.letters[j];
                        for (int i = 0; i < contactDatas.size(); i++) {
                            String itemKey = contactDatas.get(i).getFirstLetter();
                            if (itemKey.equals(key)) {
                                List<ContactsData> subData = null;
                                if (!mapContacts.containsKey(key)) {
                                    subData = new ArrayList<ContactsData>();
                                    mapContacts.put(key, subData);
                                } else {
                                    subData = mapContacts.get(key);
                                }
                                subData.add(contactDatas.get(i));
                                contactDatas.remove(i);
                                i--;
                            }
                        }
                    }
                    mainHan.post(new Runnable() {
                        @Override
                        public void run() {
                            rlLoading.setVisibility(View.GONE);
                            adpter = new ContactsAdapter(mapContacts, getActivity());
                            adpter.setCallback(ContactsFragment.this);
                            contacts.setAdapter(adpter);
                            for (int i = 0; i < mapContacts.size(); i++) {
                                contacts.expandGroup(i);
                            }
                        }
                    });
                } else {
                    mainHan.post(new Runnable() {
                        @Override
                        public void run() {
                            rlLoading.setVisibility(View.GONE);
                            if (getActivity() == null) return;
                            Toast.makeText(getActivity(), "还没有联系人", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * 在ContactsAdapter中进行回调，回调长按组时其对应的id
     * @param gid
     */
    @Override
    public void getGroupId(final int gid, String name, final String key) {
        //显示提示框进一步确认
        String message = "确定删除该组?";
        MyAlertDialogFragment myAlert = MyAlertDialogFragment.getInstance(0,0,message,true);
        myAlert.setListener(new MyAlertDialogFragment.CallbackInMyAlert() {
            @Override
            public void onClickSure() {
                if (gid != Constants.GROUPID) {
                    PoolThreadUtil.getInstance().addTask(new Runnable() {
                        @Override

                        public void run() {
                            //在此处执行删除组的操作
                            dbManager.deleteGroup(gid);//更新数据库中的数据
                        }
                    });
                    //更新数据源
                    List<ContactsData> contactsDatas = mapContacts.get(key);
                    for(int i = 0; i < contactsDatas.size();i++) {
                        ContactsData data = contactsDatas.get(i);
                        if(data.getGroupId() == gid) {
                            contactsDatas.remove(i);
                            adpter.notifyDataSetChanged();
                            break;
                        }
                    }
                }else
                {
                    Toast.makeText(getActivity(), "不能删除您正在使用的组", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onClickCancel() {

            }
        });
        myAlert.show(getFragmentManager(), null);
    }

    @Override
    public void goToGroup(String gName, final int gid) {
        String message = "要进入" + "\"" + gName + "\"" + "进行群聊吗？";
        MyAlertDialogFragment myAlert = MyAlertDialogFragment.getInstance(200, 180, message, true);
        myAlert.setListener(new MyAlertDialogFragment.CallbackInMyAlert() {
            @Override
            public void onClickSure() {
                if (callback != null) {
                    Constants.GROUPID = gid;
                    callback.changeGroup(gid);
                }
            }

            @Override
            public void onClickCancel() {

            }
        });
        try {
            myAlert.show(getFragmentManager(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final LetterView.SelectLetterListener selectLetterListener = new LetterView.SelectLetterListener() {
        @Override
        public void selectLetter(String letter, boolean pop) {
            if(pop) {
                if(showLetter == null) return;
                if(!showLetter.isShowing()) {
                    showLetter.showAtLocation(contentView,Gravity.CENTER,0,0);
                }
                tvLetter.setText(letter);
                contacts.setSelectedGroup(getIndexByKey(letter));
            }else {
                if(showLetter != null && showLetter.isShowing()) {
                    showLetter.dismiss();
                }
            }
        }
    };

    /**
     * 通过key值找到相应的索引
     * @return
     */
    private int getIndexByKey(String letter) {
        int index = 0;
        for(String key : mapContacts.keySet()) {
            if(letter.equals(key)) {
                break;
            }
            index++;
        }
        return index;
    }


    private void initPopWindow() {
        if(showLetter == null) {
            if(getActivity() == null) return;
            int size = (int)(getResources().getDisplayMetrics().density * 80  + 0.5f);
            showLetter = new PopupWindow(getActivity());
            showLetter.setWidth(size);
            showLetter.setHeight(size);
            showLetter.setBackgroundDrawable(getResources().getDrawable(R.drawable.corner_with_gray));
            tvLetter = new TextView(getActivity());
            tvLetter.setGravity(Gravity.CENTER);
            tvLetter.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
            tvLetter.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
            tvLetter.setTextColor(Color.WHITE);
            tvLetter.setTextSize(30f);
            showLetter.setContentView(tvLetter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        letterView.setListener(null);
        showLetter = null;
        mapContacts = null;
        if(callback != null) callback.openScorll(true);
    }

    public interface CallbackInContacts{
        void changeGroup(int gid);
        void openScorll(boolean open);

    }
}
