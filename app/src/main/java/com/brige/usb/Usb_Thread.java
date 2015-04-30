package com.brige.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Handler;
import android.os.Message;

import com.rftransceiver.datasets.DataPack;

//usb消息线程后台类
public class Usb_Thread extends Thread
{
	UsbEndpoint epIn; //usb stm32设备到手机的输入端点
	UsbDeviceConnection connection; //USB设备连接
	private Handler usbHandler;     // handler，发送子线程的数据
	
	public Usb_Thread(UsbDeviceConnection connection,UsbEndpoint epIn,Handler msgHandler) 
	{   //构造函数，获得mmInStream和msgHandler对象
		this.epIn = epIn;
		this.connection = connection;
		this.usbHandler = msgHandler;
	}
	
	public void run()
	{
		byte[] InBuffer = new byte[64];           //创建 缓冲区,1次传输 8个字节    
		int length = InBuffer.length;
		int timeout = 5000;
		while (!Thread.interrupted()) {                             
			//接收bulk数据
			int cnt = connection.bulkTransfer(epIn, InBuffer, length, timeout);   
			if ( cnt < 0) 
			{						//没有接收到数据，则继续循环
				continue;
			}	
						    
		    Message msg = new Message();          //定义一个消息,并填充数据
		    msg.what = 0x1234; //线程标识
		    DataPack temPack=new DataPack();
		    temPack.Inbuffer=InBuffer;
		    temPack.length=cnt;
		    msg.obj=temPack;		 
		    usbHandler.sendMessage(msg);          //通过handler发送消息
		 
		}
	}
}
