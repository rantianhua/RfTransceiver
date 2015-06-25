package com.rftransceiver.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.MapView;
import com.rftransceiver.R;
import com.rftransceiver.fragments.MapViewFragment;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-22.
 */
public class LocationActivity extends Activity{

    @InjectView(R.id.btn_send_location)
    Button btnSendLocation;

    private MapViewFragment mapViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_location);
        initView();
        initEvent();
    }

    private void initView() {
        ButterKnife.inject(this);
        mapViewFragment = new MapViewFragment();
        getFragmentManager().beginTransaction().add(R.id.frame_container_mapview_location,
                mapViewFragment).commit();
    }

    private void initEvent() {
        btnSendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mapViewFragment == null) return;
                mapViewFragment.requestLocation();
            }
        });
    }

}
