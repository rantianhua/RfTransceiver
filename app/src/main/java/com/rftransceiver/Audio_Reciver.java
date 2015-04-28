package com.rftransceiver;


import com.datasets.MyDataQueue;

public class Audio_Reciver implements Runnable
{
    private boolean isReceiving = false;
    private MyDataQueue dataQueue = null;

	 public void startReceiver()
	 {
         /**
          * 初始化缓冲区
          */
         dataQueue = MyDataQueue.getInstance(MyDataQueue.DataType.Sound_Receiver);
         new Thread(this).start();
	 }

    @Override
    public void run() {
        //先启动解码器
        Audio_Decoder decoder = Audio_Decoder.getInstance();
        decoder.startDecoding();

        this.isReceiving = true;

        while(isReceiving) {

            AudioData data = (AudioData)dataQueue.get();
            if(data == null) {
                //此处可以增加一个延时
            }else {
                //将数据添加至解码器
                decoder.addData(data);
            }
        }
        //停止解码器
        decoder.stopDecoding();
    }

    public void stopReceiver() {
        //停止接收线程
        isReceiving = false;
    }

    public void cacheData(byte[] data,int size) {
        AudioData receviceData = new AudioData();
        receviceData.setSize(size);
        receviceData.setencodeData(data);
        dataQueue.add(receviceData);
     }
}
