package com.util;

/**
 * Created by Rth on 2015/4/26.
 */
public class Constants {
    /**
     * 标志蓝牙服务器状态发生变化
     */
    public static final int MSG_WHAT_STATE = 0;

    /**
     * 标志蓝牙设备的名称,handler中使用
     */
    public static final int MSG_WHAT_DEVICE_NAME = 1;

    /**
     * 通知UI显示Toast，
     */
    public static final int MESSAGE_TOAST= 2;

    /**
     * 通知UI读取到的信息
     */
    public static final int MESSAGE_READ= 3;

    /**
     * 通知UI发送的信息
     */
    public static final int MESSAGE_WRITE= 4;


    /**
     * 蓝牙设备名称的flag
     */
    public static final String DEVICE_NAME = "device_name";

    /**
     * 显示toast的flasg
     */
    public static final String TOAST = "toast";
}
