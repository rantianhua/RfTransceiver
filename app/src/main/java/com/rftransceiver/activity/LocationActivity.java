package com.rftransceiver.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.baidu.mapapi.search.core.PoiInfo;
import com.rftransceiver.R;
import com.rftransceiver.fragments.HomeFragment;
import com.rftransceiver.fragments.MapViewFragment;
import com.rftransceiver.customviews.CommonAdapter;
import com.rftransceiver.customviews.CommonViewHolder;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by rantianhua on 15-6-22.
 */
public class LocationActivity extends Activity implements MapViewFragment.CallbackInMVF{

    @InjectView(R.id.btn_send_location)
    Button btnSendLocation;
    @InjectView(R.id.listview_location_result)
    ListView listView;
    @InjectView(R.id.pb_searching_location)
    ProgressBar pb;

    private MapViewFragment mapViewFragment;

    /**
     * the source data of listview
     */
    private List<PoiInfo> posInfos;

    /**
     * after an item is selected,the ImageView in the item show
     */
    private ImageView chooseItem;

    /**
     * the address to be send
     */
    private String address;

    private String city;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        posInfos = new ArrayList<>();
        initView();
        initEvent();
    }

    private void initView() {
        ButterKnife.inject(this);
        mapViewFragment = new MapViewFragment();
        mapViewFragment.setCallback(this);
        getFragmentManager().beginTransaction().add(R.id.frame_container_mapview_location,
                mapViewFragment).commit();

        listView.setAdapter(new CommonAdapter<PoiInfo>(this,posInfos,R.layout.list_item_location) {
            @Override
            public void convert(CommonViewHolder helper, PoiInfo item) {
                helper.setText(R.id.tv_place_name,item.name);
                helper.setText(R.id.tv_place_address,item.address);
            }
        });
    }

    private void initEvent() {
        btnSendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if(!TextUtils.isEmpty(address)) {
                   Intent intent = new Intent();
                   intent.putExtra(HomeFragment.EXTRA_LOCATION,address);
                   setResult(Activity.RESULT_OK,intent);
                   finish();
               }
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(chooseItem != null) {
                    chooseItem.setVisibility(View.INVISIBLE);
                }
                chooseItem = (ImageView)view.findViewById(R.id.img_choose_location);
                chooseItem.setVisibility(View.VISIBLE);
                PoiInfo poiInfo = posInfos.get(i);
                address = poiInfo.address + "|" + city;
            }
        });
    }

    /**
     * callback in MapViewFragment
     * @param infos
     */
    @Override
    public void surroundInfo(List<PoiInfo> infos,String city) {
        pb.setVisibility(View.GONE);
        this.city = city;
        posInfos.clear();
        posInfos.addAll(infos);
        if(infos.size() == 0) {
            Toast.makeText(this,"未搜索到周边位置",Toast.LENGTH_SHORT).show();
            return;
        }
        CommonAdapter adapter = (CommonAdapter)listView.getAdapter();
        adapter.notifyDataSetChanged();
    }
}
