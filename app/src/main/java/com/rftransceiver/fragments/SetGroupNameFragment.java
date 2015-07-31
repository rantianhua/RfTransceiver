package com.rftransceiver.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-15.
 */
public class SetGroupNameFragment extends Fragment {

    @InjectView(R.id.et_limit_input)
    EditText etGroupName;
    @InjectView(R.id.tv_label_add)
    TextView tvAddlabel;
    @InjectView(R.id.tv_label_create)
    TextView tvCreateLabel;
    @InjectView(R.id.tv_input_length)
    TextView tvEtLength;
    @InjectView(R.id.img_cancel_input)
    ImageView imgCancel;
    @InjectView(R.id.btn_sure_set_group_name)
    Button btnSure;
    @InjectView(R.id.img_rainbow_above)
    ImageView background;
    /**
     * the instance of OnGroupNameSet
     */
    private OnGroupNameSet listener;
    /**
     * the max length of group name
     */
    private final int etLength = 8;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setgruopname,container,false);
        initView(view);
        initEvent();
        return view;
    }


    private void initView(View view) {
        ButterKnife.inject(this, view);
        imgCancel.setImageResource(R.drawable.cancel_black);
        tvEtLength.setTextColor(Color.BLACK);
        tvEtLength.setText(0 + "/" + etLength);
        etGroupName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(etLength)});
        etGroupName.setTextColor(Color.BLACK);
        etGroupName.setHint(R.string.hint_et_group_name);
        etGroupName.addTextChangedListener(textWatcher);

        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 4;
        Bitmap back = BitmapFactory.decodeResource(getResources(),R.drawable.creat_group_above,op);
        background.setBackground(new BitmapDrawable(back));

        SpannableString ss = new SpannableString("一人建组");
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
        ss.setSpan(colorSpan,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvCreateLabel.setText(ss);
        ss = null;
        ss = new SpannableString("多人加入");
        ss.setSpan(colorSpan,0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvAddlabel.setText(ss);
        ss = null;
        colorSpan = null;
    }

    private void initEvent() {
        btnSure.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = etGroupName.getText().toString();
                if(!TextUtils.isEmpty(name)) {
                    if(listener != null) {
                        listener.getGroupName(name);
                    }
                }
            }
        });
        imgCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etGroupName.setText("");
            }
        });
    }

    public void setOnGroupNameSetCallback(OnGroupNameSet listener) {
        this.listener = listener;
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            tvEtLength.setText(editable.toString().length()+"/"+etLength);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        setOnGroupNameSetCallback(null);
    }

    /**
     * interface interactive with ACtivity
     */
    public interface OnGroupNameSet {
        void getGroupName(String name);
    }
}
