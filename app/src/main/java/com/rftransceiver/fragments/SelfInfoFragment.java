package com.rftransceiver.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.rftransceiver.R;


import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Wuyang on 2015/7/30.
 */
public class SelfInfoFragment extends Fragment {

    @InjectView(R.id.img_head_selfinfo)
    ImageView imgHead;
    @InjectView(R.id.ed_name_selfinfo)
    EditText edName;
    @InjectView(R.id.btn_confirm_selfinfo)
    Button btnConfirm;

    private RelativeLayout content;
    private Drawable dwHead,dwClean,dwEdit;
    private String name;
    private RelativeLayout.LayoutParams lp;
    private float dentisy;
    //标识是否可以修改名称
    private boolean changeInfo = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dentisy = getResources().getDisplayMetrics().density;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        content = (RelativeLayout) inflater.inflate(R.layout.fragment_selfnfo,container,false);
        dwClean = getResources().getDrawable(R.drawable.cancel1);
        dwEdit = getResources().getDrawable(R.drawable.pen);
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 4;
        Bitmap backGround = BitmapFactory.decodeResource(getResources(),R.drawable.chatbackground,op);
        content.setBackground(new BitmapDrawable(backGround));
        initVierw(content);
        initEvent();
        return content;
    }

    private void initVierw(View view) {
        ButterKnife.inject(this, view);
        if(dwHead != null) {
            //展示头像
            imgHead.setImageDrawable(dwHead);
        }
        if(!TextUtils.isEmpty(name)) {
            //展示用户名
            edName.setText(name);
        }

        lp = (RelativeLayout.LayoutParams) edName.getLayoutParams();
        //������ʾ��edName�Ҳ����ΪdwClean��dwEdit��CompoundDrawables��С
        dwClean.setBounds(0, 0, (int) (dentisy * 20 + 0.5f), (int) (dentisy * 20 + 0.5f));
        dwEdit.setBounds(0, 0, (int) (dentisy * 20 + 0.5f), (int) (dentisy * 20 + 0.5f));
        edName.setCompoundDrawables(null, null, dwEdit, null);
        edName.setCompoundDrawablePadding((int) (dentisy * 5 + 0.5f));
        if(!changeInfo) {
            //设置EditText不能编辑
            edName.setClickable(false);
            edName.setFocusable(false);
            edName.setEnabled(false);
        }
    }

    public void initEvent() {
        edName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                    lp.setMargins(150, 0, 0, 0);
                    edName.setLayoutParams(lp);
                    edName.setGravity(Gravity.LEFT);
                    btnConfirm.setVisibility(View.VISIBLE);
                    if (edName.length() != 0) {
                        edName.setCompoundDrawables(null, null, dwClean, null);
                    }
                } else {
                    lp.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    lp.setMargins(0, 0, 0, 0);
                    edName.setLayoutParams(lp);
                    edName.setGravity(Gravity.CENTER);
                    edName.setCompoundDrawables(null, null, dwEdit, null);
                }
            }
        });
        edName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edName.length() != 0)
                    edName.setCompoundDrawables(null, null, dwClean, null);
                else
                    edName.setCompoundDrawables(null, null, null, null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        edName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (edName.hasFocus() && event.getRawX() > edName.getX() + edName.getWidth() - dentisy * 30 + 0.5f)
                    edName.setText("");
                return false;
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edName.clearFocus();
                imgHead.requestFocus();
                btnConfirm.setVisibility(View.INVISIBLE);
                String newName = edName.getText().toString();
                setName(newName);
                if (!newName.equals(name)) {
                    if (getTargetFragment() != null) {
                        Intent intent = new Intent();
                        intent.putExtra("name",name);
                        getTargetFragment().onActivityResult(SettingFragment.REQUEST_CHANGEINFO,
                                Activity.RESULT_OK,intent);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     * 设置头像
     * @param head
     */
    public void setHead(Drawable head){
        this.dwHead=head;
    }

    /**
     * 设置名称
     * @param name
     */
    public void setName(String name){
        this.name=name;
    }

    /**
     * 设置是否可以修改个人信息
     * @param changeInfo
     */
    public void setChangeInfo(boolean changeInfo) {
        this.changeInfo = changeInfo;
    }
}
