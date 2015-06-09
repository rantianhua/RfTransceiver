package com.rftransceiver.datasets;

public class AudioData
{
	int size;
	short[] realData;
	byte[] encodeData;
	public void setSize(int size) //���ô�С 
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
