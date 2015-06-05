package com.rftransceiver.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rftransceiver.R;

import java.util.ArrayList;

/**
 * Created by rantianhua on 15-6-5.
 */
public class GroupItemsAdapter extends BaseAdapter {

        private ArrayList<ScanResult> devices;
        private LayoutInflater mInflator;

        public GroupItemsAdapter(Context context) {
            super();
            devices = new ArrayList<>();
            mInflator = LayoutInflater.from(context);
        }

        public boolean addDevice(ScanResult device) {
            boolean deviceExist = false;
            for(ScanResult result : devices) {
                if(result.SSID.equals(device.SSID)) {
                    deviceExist = true;
                    break;
                }
            }
            if(!deviceExist) {
                devices.add(device);
                return true;
            }
            return false;
        }

        public ScanResult getDevice(int position) {
            return devices.get(position);
        }

        public void clear() {
            devices.clear();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ScanResult device = devices.get(i);
            final String deviceName = device.SSID;
            if (!TextUtils.isEmpty(deviceName))
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.BSSID);
            return view;
        }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
