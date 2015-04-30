package com.rftransceiver.datasets;

public class AudioData //音频数据类
{
	int size;  //数据大小
	short[] realData;  //实时数据
	byte[] encodeData;//已编码数据
	public void setSize(int size) //设置大小 
	{
		this.size = size;  		
	}

	public void setRealData(short[] tempData)  //设置实时数据缓冲区
	{
		  this.realData = tempData;  
		
	}
	
	public void setencodeData(byte[] tempData)  //设置编码数据缓冲区
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
