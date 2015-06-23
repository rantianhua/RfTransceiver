package com.rftransceiver.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.MapView;
import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-22.
 */
public class LocationActivity extends Activity implements BDLocationListener{

    @InjectView(R.id.btn_send_location)
    Button btnSendLocation;
    @InjectView(R.id.mapview_location)
    MapView mapView;

    /**
     * core class of location
     */
    private LocationClient locationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_location);
        initLocation();
        initView();
    }

    private void initLocation() {
        locationClient = new LocationClient(getApplicationContext());
        //the way of location
        LocationClientOption option = new LocationClientOption();
        //open gps
        option.setOpenGps(true);
        //only use gps
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        //return result is longitude and latitude in baidu and default is gcj02
        option.setCoorType("bd09ll");
        //the location result contain address's info
        option.setIsNeedAddress(true);
        option.setAddrType("all");

        locationClient.setLocOption(option);
    }

    private void initView() {
        ButterKnife.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        locationClient.start();
        locationClient.requestLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        locationClient.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    /**
     *
     * @param bdLocation the location info
     */
    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        if(bdLocation == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("time : ");
        sb.append(bdLocation.getTime());
        sb.append("\nerror code : ");
        sb.append(bdLocation.getLocType());
        sb.append("\nlatitude : ");
        sb.append(bdLocation.getLatitude());
        sb.append("\nlontitude : ");
        sb.append(bdLocation.getLongitude());
        sb.append("\nradius : ");
        sb.append(bdLocation.getRadius());
        if (bdLocation.getLocType() == BDLocation.TypeGpsLocation){
            sb.append("\nspeed : ");
            sb.append(bdLocation.getSpeed());
            sb.append("\nsatellite : ");
            sb.append(bdLocation.getSatelliteNumber());
        } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation){
            sb.append("\naddr : ");
            sb.append(bdLocation.getAddrStr());
        }
        Log.e("onReceiveLocation",sb.toString());
        sb = null;
    }
}
