package com.rftransceiver.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.customviews.CircleImageDrawable;
import com.rftransceiver.fragments.SelfInfoFragment;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rth on 15-7-31.
 */
public class PersonalActivity extends Activity {
    @InjectView(R.id.img_top_left)
    ImageView imgBack;
    @InjectView(R.id.tv_title_left)
    TextView tvTitle;

    //用户头像
    private Bitmap bmHead;
    //用户昵称
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);
        Intent intent = getIntent();
        if(intent != null)  {
            name = intent.getStringExtra("name");
            try{
                Bundle bundle = intent.getBundleExtra("bitmap");
                bmHead = bundle.getParcelable("bitmap");
            }catch (Exception e) {
                e.printStackTrace();
            }
        }else {
        }
        initView();
        initEvent();
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_personal);

    }

    private void initEvent() {
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initView() {
        ButterKnife.inject(this);
        //设置标题栏
        tvTitle.setText("个人中心");
        imgBack.setImageResource(R.drawable.back);
        Drawable drawable = null;
        if(bmHead != null) {
            drawable = new CircleImageDrawable(bmHead);
        }
        SelfInfoFragment selfInfoFragment = new SelfInfoFragment();
        selfInfoFragment.setHead(drawable);
        selfInfoFragment.setName(name);
        selfInfoFragment.setChangeInfo(false);
        getFragmentManager().beginTransaction().add(R.id.rl_content_personal, selfInfoFragment).commit();
    }

}
