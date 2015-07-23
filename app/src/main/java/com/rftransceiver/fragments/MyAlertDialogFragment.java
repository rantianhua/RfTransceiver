package com.rftransceiver.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.rftransceiver.R;

import org.w3c.dom.Text;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rth on 15-7-23.
 */
public class MyAlertDialogFragment extends BaseDialogFragment {
    @InjectView(R.id.tv_sure_alert)
    TextView tvSure;
    @InjectView(R.id.tv_cancel_alert)
    TextView tvCancel;
    @InjectView(R.id.tv_message_alert)
    TextView tvMessage;

    private int width = 100,heiht = 100;
    private boolean showCancel = true;
    private String message;

    private CallbackInMyAlert listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float dentisy = getResources().getDisplayMetrics().density;
        int setW = getArguments().getInt("w");
        int setH = getArguments().getInt("h");
        if(setW > width) width = setW;
        if(setH > heiht) heiht = setH;
        width = (int)(dentisy * width + 0.5f);
        heiht = (int)(dentisy * heiht + 0.5f);
        showCancel = getArguments().getBoolean("cancel");
        message = getArguments().getString("message");
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = getResources().getDisplayMetrics().widthPixels - 100;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    @Override
    public View initContentView(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.dialog_alert,null);
        ButterKnife.inject(this, view);
        if(!showCancel) tvCancel.setVisibility(View.INVISIBLE);
        if(message != null) tvMessage.setText(message);
        return view;
    }

    @Override
    public void initEvent() {
        tvSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if(listener != null) {
                    listener.onClickSure();
                    setListener(null);
                }
            }
        });

        if(showCancel) {
            tvCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                    if(listener != null) {
                        listener.onClickCancel();
                        setListener(null);
                    }
                }
            });
        }
    }

    public void setListener(CallbackInMyAlert listener) {
        this.listener = listener;
    }

    public static MyAlertDialogFragment getInstance(int width,int height,String message,boolean needCancel) {
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        bundle.putInt("w", width);
        bundle.putInt("h", height);
        bundle.putBoolean("cancel", needCancel);
        MyAlertDialogFragment fragment = new MyAlertDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public interface CallbackInMyAlert{
        void onClickSure();
        void onClickCancel();
    }
}
