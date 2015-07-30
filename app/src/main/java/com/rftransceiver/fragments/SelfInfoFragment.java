package com.rftransceiver.fragments;

import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
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
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.db.DBManager;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Administrator on 2015/7/30.
 */
public class SelfInfoFragment extends Fragment {

    @InjectView(R.id.img_head_selfinfo)
    ImageView imgHead;
    @InjectView(R.id.ed_name_selfinfo)
    EditText edName;
    @InjectView(R.id.btn_confirm_selfinfo)
    Button btnConfirm;

    private Bitmap bmClean;
    private RelativeLayout content;
    private Drawable dwHead,dwClean;
    private String name;

    private float dentisy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dentisy = getResources().getDisplayMetrics().density;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        content = (RelativeLayout) inflater.inflate(R.layout.fragment_selfnfo,container,false);
        bmClean =  BitmapFactory.decodeResource(null,R.drawable.cancel1);
        dwClean = new BitmapDrawable(bmClean);
        initVierw(content);
        initEvent();
        return content;
    }

    private void initVierw(View view) {
        ButterKnife.inject(this, view);
        imgHead.setImageDrawable(dwHead);
        edName.setText(name);
        if(imgHead!=null) imgHead.setImageDrawable(dwHead);
        if(name!=null) edName.setText(name);
        if(edName.hasFocus()){
            RelativeLayout.LayoutParams lp =(RelativeLayout.LayoutParams) edName.getLayoutParams();
            lp.width=RelativeLayout.LayoutParams.MATCH_PARENT;
            lp.setMargins(150, 0, 0, 0);
            edName.setLayoutParams(lp);
            edName.setGravity(Gravity.LEFT);
            dwClean.setBounds(0, 0, (int) (dentisy * 20 + 0.5f), (int) (dentisy * 20 + 0.5f));
        }
    }

    public void initEvent(){
        edName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    btnConfirm.setVisibility(View.VISIBLE);
                    if (edName.length() != 0)
                        edName.setCompoundDrawables(null, null, dwClean, null);
                } else {
                    btnConfirm.setVisibility(View.INVISIBLE);
                    edName.setCompoundDrawables(null, null, null, null);
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
                if(event.getRawX()>edName.getX()+dentisy*20+0.5f)
                    edName.setText("");
                return false;
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edName.clearFocus();
                btnConfirm.setVisibility(View.INVISIBLE);
                setName(edName.getText().toString());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    public void setHead(Drawable head){
        this.dwHead=head;
    }

    public void setName(String name){
        this.name=name;
    }
}
