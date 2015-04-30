package com.brige.usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import java.util.HashMap;
import java.util.Iterator;

public class Usb_Device //usb设备类
{
	UsbManager usb_manager;               //usb管理器对象
	UsbDevice stm32_device;				     //stm32设备对象
	UsbInterface usb_interface;					 //usb接口对象
	UsbDeviceConnection connection;		 //usb设备连接对象
	UsbEndpoint ep_out,ep_in;				 //输入、输出 端点 对象
    Usb_Thread usb_thread;             //usb后台消息线程
	Handler usb_handle;                 //usb消息handler
	public boolean usb_state;                  //usb的连接状态
	public boolean usb_permisson;              //设置权限
	 
	
	
   public void  Usb_GetPermisson(Activity activity)  //获取usb权限
	{
	    //注册广播
		final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		activity.registerReceiver(mUsbReceiver, filter);
		usb_manager = (UsbManager)activity.getSystemService(Context.USB_SERVICE); //获取USB服务
		if (usb_manager == null)
		{
			usb_permisson=false;
			return ;
		}
		//搜索所有的usb设备
				HashMap<String,UsbDevice> deviceList = usb_manager.getDeviceList();
				Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
				while (deviceIterator.hasNext()) 
				{
					UsbDevice device = deviceIterator.next();						
						stm32_device = device;			
				}	
				if(stm32_device==null)	
				{   
					usb_permisson=false;
					return ;//找不到设备
				}
		if(!usb_manager.hasPermission(stm32_device))//判断是否有权限 使用usb设备
		{ 		
		     //没有权限则询问用户是否授予权限			
		   usb_manager.requestPermission(stm32_device, mPermissionIntent); //该代码执行后，系统弹出一个对话框，  
		                                                  //询问用户是否授予程序操作USB设备的权限  
		} 
		else
		{
		   usb_permisson=true;
		   return;
		}
		
	}
	
	public boolean Usb_Connect( )// 进行USB连接
	{
			
					
		connection = usb_manager.openDevice(stm32_device);//和stm32创建连接    
		if(connection==null)
			return false;//连接失败
		if (stm32_device.getInterfaceCount() != 1) 	
			return false;
		usb_interface=stm32_device.getInterface(0); //获取接口
		connection.claimInterface(usb_interface, true);//独占接口
		int cnt = usb_interface.getEndpointCount();  //获取可用端点的数目
		if(cnt<1)
			return false;
		for (int index = 0; index < cnt; index++)  //遍历端点 找到输入端点和输出端点
		{
			  UsbEndpoint ep = usb_interface.getEndpoint(index); //获取index序号的端点			 
			  if ((ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) && (ep.getDirection() == UsbConstants.USB_DIR_OUT))
				  ep_out = ep;    //发送数据端点			  
			  
			  if ((ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) && (ep.getDirection() == UsbConstants.USB_DIR_IN)) 			  
				  ep_in = ep;    //接收数据端点					
		}		
		usb_thread = new Usb_Thread(connection,ep_in,usb_handle);  //创建 usb数据接收线程
		usb_thread.start();
		usb_state= true;
		
		return true; //连接成功
	}
	
	boolean Usb_Disconnect() //usb断开连接
	{		
		if (usb_state) 
		{
		 connection.releaseInterface(usb_interface);   //释放接口
	     usb_thread.interrupt();		   //结束 线程	
	     return true;
		}
		else
		return false;	
	}
	
	public boolean Usb_Transfer(byte buffer[],int length) //传输数据 使用时放在后台线程
	{
		 
		int cnt=connection.bulkTransfer(ep_out, buffer, length, 5000); //输出端点 缓冲区 缓冲长度 时间
		if(cnt>=0)
		return true;
		else 
		return false;		
	}
	
	
	boolean Usb_State()//获取USB打开状态
	{
		return usb_state;
	}
	
	
	
	
	
	//广播授权
	private static final String ACTION_USB_PERMISSION =
		    "com.android.example.USB_PERMISSION";
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		    //usb 设备操作 授权
		    public void onReceive(Context context, Intent intent)
		    {
		        String action = intent.getAction();
		        if (ACTION_USB_PERMISSION.equals(action)) 
		        {
		            synchronized (this) 
		            {
		            	UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
		                {
		                    if(device != null)	      
		                    {
		                       stm32_device = device;  
		                       usb_permisson=true;
		                    }
		                else
		                   { 
		                	  usb_permisson=false;
		                	  return;
		                   }
		               
		                }          
		                
		            }
		        }
		    }
	 };
	
   
}
