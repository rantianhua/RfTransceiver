package com.rftransceiver.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.rftransceiver.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-24.
 */
public class MapViewFragment extends Fragment implements BDLocationListener{

    @InjectView(R.id.mapview_location)
    MapView mapView;

    /**
     * core class of location
     */
    private LocationClient locationClient;
    private BaiduMap baiduMap;

    private boolean isFirstLocate = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initLocation();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapview,container,false);
        initView(view);
        initEvent();
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this,view);
        //init map
        baiduMap = mapView.getMap();
        /**
         * open location layer
         */
        baiduMap.setMyLocationEnabled(true);
    }

    private void initEvent() {

    }

    private void initLocation() {
        locationClient = new LocationClient(getActivity().getApplicationContext());
        locationClient.registerLocationListener(this);
        //the way of location
        LocationClientOption option = new LocationClientOption();
        //open gps
        option.setOpenGps(true);
        //only use gps
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //return result is longitude and latitude in baidu and default is gcj02
        option.setCoorType("bd09ll");
        //the location result contain address's info
        option.setIsNeedAddress(true);
        option.setAddrType("all");

        locationClient.setLocOption(option);
        locationClient.start();
    }


    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        //destroy location
        locationClient.stop();
        //close location layer
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
        super.onDestroy();
    }

    /**
     *
     * @param bdLocation the location info
     */
    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        if(bdLocation == null || mapView == null) {
            return;
        }
        MyLocationData myLocationData = new MyLocationData.Builder()
                .accuracy(bdLocation.getRadius())
                .latitude(bdLocation.getLatitude())
                .longitude(bdLocation.getLongitude()).build();
        baiduMap.setMyLocationData(myLocationData);
        if(isFirstLocate) {
            LatLng ll = new LatLng(bdLocation.getLatitude(),
                    bdLocation.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(u);
        }
    }

    /**
     * request to locate my location
     */
    public void requestLocation() {
        locationClient.requestLocation();
    }
}
