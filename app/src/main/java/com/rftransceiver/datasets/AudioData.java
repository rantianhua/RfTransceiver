package com.rftransceiver.datasets;

public class AudioData//存储音频信息的类
{
	int size;
	short[] realData;	//实际采集到的原生语音数据
	byte[] encodeData;	//经过编码后的语音数据
	public void setSize(int size)
	{
		this.size = size;  		
	}

	public void setRealData(short[] tempData)
	{
		  this.realData = tempData;  
		
	}
	
	public void setencodeData(byte[] tempData)
	{
		  this.encodeData = tempData;  
		
	}


	public int getSize()
	{
		return this.size;
	}

	public short[] getRealData() 
	{
		 return realData;  
	}
	

	public byte[] getencodeData() 
	{
		 return encodeData;  
	}

}
