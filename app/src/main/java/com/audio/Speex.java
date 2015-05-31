
package com.audio;

//==========================================
//1����������Ҫ��libs\armeabi\libspeex.so ֧��
//2�����������˳�� 
//��������              Speex coder=new Speex();  ��ʼ��������    coder.init();
//����                        coder.encode(rawdata,0,encodeddata,160);
//����                        coder.decode(encodeddata,0,rawdata,encodeddata.getsize());
//�رս�����         coder.close();
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
	//�˴�����������������
	private static final int DEFAULT_COMPRESSION = 1;
	

	public Speex() 
	{
		
	}

	//��������ʼ��
	public void init() 
	{
		load();	
		open(DEFAULT_COMPRESSION);	//��ȡ������������	
	}
	
	private void load()
	{
		try 
		{
		    System.loadLibrary("speex");//��ȡso��
		} 
		catch (Throwable e) 
		{
			e.printStackTrace();
		}

	}

	public native int open(int compression);
	
	//��ȡ��Ҫ��֡��
	public native int getFrameSize();
	//���������Byte������н��룬sizeΪByte����ĳ��� linΪ�����short���� 
	//�������ر������ݵĳ���
	public native int decode(byte encoded[], short lin[], int size);
	//����������PCM����,sizeΪshort���鳤��
	//�������ؽ������ݵĳ���
	public native int encode(short lin[], int offset, byte encoded[], int size);
	//�رս�����
	public native void close();
	
}
