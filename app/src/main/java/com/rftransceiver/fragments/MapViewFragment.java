package com.rftransceiver.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.rftransceiver.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-24.
 */
public class MapViewFragment extends Fragment implements BDLocationListener{

    @InjectView(R.id.mapview_location)
    MapView mapView;

    //定位的核心类
    private LocationClient locationClient;
    private BaiduMap baiduMap;

    //标识是否是第一次定位
    private boolean isFirstLocate = true;

    //反编码
    private GeoCoder geocoder;

    //显示特定位置
    private String address;

    //poi搜索结果
    private List<PoiInfo> poiInfoList;

   //当前位置信息
    private PoiInfo currentInfo;


    /**
     * interface interactive with other class
     */
    private CallbackInMVF callbackInMVF;

    private String city;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            address = getArguments().getString("address");
        }catch (Exception e) {
            address = null;
        }

        try {
            geocoder = GeoCoder.newInstance();
            geocoder.setOnGetGeoCodeResultListener(getGeoCoderResultListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapview, container, false);
        initView(view);

        if (TextUtils.isEmpty(address)) {
            if (locationClient == null) initLocation();
        } else {
            String[] addresses = address.split("\\|");
            if (addresses.length > 1) {
                if (geocoder != null) {
                    geocoder.geocode(new GeoCodeOption().address(addresses[0]).city(addresses[1]));
                }
            }
        }

        initEvent();
        return view;
    }

    private void initView(View view) {
        ButterKnife.inject(this, view);
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
        poiInfoList = new ArrayList<>();
        //locate
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
        if(locationClient != null) {
            locationClient.requestLocation();
        }
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        //destroy location
        if(locationClient != null) locationClient.stop();
        //close location layer
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        if(geocoder != null) {
            geocoder.destroy();
        }
        super.onDestroy();
        if(poiInfoList != null) {
            poiInfoList.clear();
        }
        setCallback(null);
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
        if(city == null) city = bdLocation.getCity();
        baiduMap.setMyLocationData(myLocationData);
        //custom the icon
        MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL,
                true,null);
        baiduMap.setMyLocationConfigeration(config);

        if(isFirstLocate) {
            isFirstLocate = false;
            LatLng ll = new LatLng(bdLocation.getLatitude(),
                    bdLocation.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(u);
            if(callbackInMVF == null) return;
            geocoder.reverseGeoCode(new ReverseGeoCodeOption().location(ll));
        }
    }

    /**
     * the listener of geo coder
     */
    final OnGetGeoCoderResultListener getGeoCoderResultListener = new OnGetGeoCoderResultListener() {
        @Override
        public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
            if(geoCodeResult == null || geoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Log.e("onGetReverseGeo"," 没有检索到结果");
                return;
            }
            LatLng ll = geoCodeResult.getLocation();
            if(ll == null) return;
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(u);
        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {

            if(reverseGeoCodeResult == null ||
                    reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                // no result
                Log.e("onGetReverseGe"," 没有检索到结果");
                return;
            }
            if(currentInfo == null) {
                currentInfo = new PoiInfo();
            }
            currentInfo.address = reverseGeoCodeResult.getAddress();
            currentInfo.location = reverseGeoCodeResult.getLocation();
            currentInfo.name = "[当前位置]";
            currentInfo.city = city;
            poiInfoList.clear();
            poiInfoList.add(currentInfo);

            //add surround position info to list
            if(reverseGeoCodeResult.getPoiList() != null) {
                poiInfoList.addAll(reverseGeoCodeResult.getPoiList());
            }
            //update adapter
            if(callbackInMVF == null) return;
            callbackInMVF.surroundInfo(poiInfoList,city);
        }
    };

    public void setCallback(CallbackInMVF callback) {
        this.callbackInMVF = callback;
    }

    /**
     * request to locate my location
     */
    public void requestLocation() {
        locationClient.requestLocation();
    }

    public static MapViewFragment getInstance(String address) {
        MapViewFragment fragment = new MapViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("address",address);
        fragment.setArguments(bundle);
        return fragment;
    }

    //public static final String EXTRA_POISEARCH = "poiSearch";

    public interface CallbackInMVF {
        void surroundInfo(List<PoiInfo> infos,String city);
    }
}
