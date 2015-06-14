package com.rftransceiver.fragments;

import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-13.
 */
public class LoadDialogFragment extends DialogFragment {

    @InjectView(R.id.img_loading)
    ImageView imgLoading;
    @InjectView(R.id.tv_text_loading)
    TextView tvMessage;

    public static final String EXTRA_DATA = "text";
    private ObjectAnimator roate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View v = inflater.inflate(R.layout.dialog_loading,container,false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);
        String text = getArguments().getString(EXTRA_DATA);
        tvMessage.setText(text);
        text = null;
        initAnim();
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                120,getActivity().getResources().getDisplayMetrics());
        getDialog().getWindow().setAttributes(params);
        roate.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        roate.cancel();
    }

    public static LoadDialogFragment getInstance(String text) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_DATA,text);
        LoadDialogFragment fragment = new LoadDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private void initAnim() {
        if(roate == null) {
            roate = ObjectAnimator.ofFloat(imgLoading,"rotation",0.0F,360F);
            roate.setDuration(800);
            roate.setRepeatCount(ObjectAnimator.INFINITE);
        }
    }
}
