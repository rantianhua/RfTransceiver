package com.rftransceiver.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.rftransceiver.R;
import com.rftransceiver.fragments.MyDeviceFragment;


/**
 * Created by rantianhua on 15-6-21.
 */
public class MyDeviceActivity extends Activity implements MyDeviceFragment.CallbackInMyDevice{

    /**
     * the reference of MyDeviceFragment
     */
    private MyDeviceFragment myDeviceFragment;

    /**
     *
     */
    private Intent sendIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mydevice);

        sendIntent = getIntent();

        myDeviceFragment = new MyDeviceFragment();
        myDeviceFragment.setCallback(this);
        getFragmentManager().beginTransaction().add(R.id.ll_container_mydevice,
                myDeviceFragment).commit();
    }

    /**
     * callback in MyDeviceFragment
     */
    @Override
    public void bindDevice() {
        sendIntent.putExtra(EXTRA_INMYDEVICE,BINDDEVICE);
        setResult(Activity.RESULT_OK,sendIntent);
        finish();
    }

    /**
     * callback in MyDeviceFragment
     */
    @Override
    public void unbindDevice() {
        sendIntent.putExtra(EXTRA_INMYDEVICE,UNBINDDEVICE);
        setResult(Activity.RESULT_OK,sendIntent);
    }

    /**
     * the key of intent extra
     */
    public static final String EXTRA_INMYDEVICE = "msg_in_mydevice";
    /**
     * to tell MainActivity to bind device
     */
    public static final String BINDDEVICE = "bind_device";
    /**
     * to tell MainACtivity to unbind device
     */
    public static final String UNBINDDEVICE = "unbind_device";
}
