package com.rftransceiver.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.rftransceiver.R;
import com.rftransceiver.fragments.MyDeviceFragment;

import butterknife.InjectView;

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
        Bundle bundle = new Bundle();
        bundle.putBoolean(BINDDEVICE,true);
        sendIntent.putExtras(bundle);
        setResult(Activity.RESULT_OK,sendIntent);
        finish();
    }

    public static String BINDDEVICE = "bind_device";
}
