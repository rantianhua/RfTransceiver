
package com.audio;

//==========================================
//1、编码类需要在libs\armeabi\libspeex.so 支持
//2、编码类操作顺序 
//创建对象              Speex coder=new Speex();  初始化编码器    coder.init();
//编码                        coder.encode(rawdata,0,encodeddata,160);
//解码                        coder.decode(encodeddata,0,rawdata,encodeddata.getsize());
//关闭解码器         coder.close();
//=============================================
public class Speex  
{

	/* quality
	 * 1 : 4kbps (very noticeable artifacts, usually intelligible)
	 * 2 : 6kbps (very noticeable artifacts, good intelligibility)
	 * 4 : 8kbps (noticeable artifacts sometimes)
	 * 6 : 11kpbs (artifacts usually only noticeable with headphones)
	 * 8 : 15kbps (artifacts not usually noticeable)
	 */
	//此处调节语音编码质量
	private static final int DEFAULT_COMPRESSION = 1;
	

	public Speex() 
	{
		
	}

	//编码器初始化
	public void init() 
	{
		load();	
		open(DEFAULT_COMPRESSION);	//读取编码质量设置	
	}
	
	private void load()
	{
		try 
		{
		    System.loadLibrary("speex");//读取so库
		} 
		catch (Throwable e) 
		{
			e.printStackTrace();
		}

	}

	public native int open(int compression);
	
	//获取需要的帧长
	public native int getFrameSize();
	//传入编码后的Byte数组进行解码，size为Byte数组的长度 lin为输出的short数组 
	//函数返回编码数据的长度
	public native int decode(byte encoded[], short lin[], int size);
	//传入待编码的PCM数据,size为short数组长度
	//函数返回解码数据的长度
	public native int encode(short lin[], int offset, byte encoded[], int size);
	//关闭解码器
	public native void close();
	
}
