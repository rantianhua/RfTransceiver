# RfTransceiver
#对讲机第一版程序文档--简洁版

##程序包结构简介：
- com.audio:该包只含有一个Speex语音编码解码库文件
- com.brige.blutooth.le:该包主要是蓝牙BLE的服务，其中BleService.java主要用来监听、管理蓝牙的连接，并操作数据的实际读取和发送。
- com.brige.blutooth.normal:该包也是和蓝牙有关的操作，不过这是最初用的普通蓝牙，现在已经没有在用，只是保留在这里。
- com.brige.wifi:该包下包含wifi的服务类--WifiNetSevice.java和Wifi连接状态的广播接收器--WiFiBroadcastReceiver.java.主要是前者，控制wifi的搜索、连接、socket的创建、数据传输等一切和wifi相关的实际操作。
- com.rftransceiver.activity:该类下都是Activity文件，最主要的是MainActivity.java文件。
- com.rftransceiver.adapter:该类下都是一些控件如ListView和GridView等的数据适配器。
- com.rftransceiver.customviews:该包下都是一些为了满足特定功能而自定义的View。
- com.rftransceiver.datasets:该包下都是一些数据描述文件，类java bean文件。其中一些要特别说明一下：1、AudioData.java，该类描述的是语音信息，在录音的时候使用,首先是Speex库采集到的原生语音数据数据信息，类型为short数组，然后要立即将这些原生信息经过编码器编码为特定长度的byte型数组，这也是实际传输的数据，这两部分的数据转换是实时的，且在播放时是一个逆过程，所以就这两部分数据描述在一个类里，方便传输和编解码;2、MyDataQueue.java,该类描述的实际是语音信息的载体，由于语音信息要实时采集发送和实时接收播放，所以需要用队列来作为载体，且要考虑多线程间的同步，所以统一在一个类里进行描述,作为载体也分了四个类型在不同地方和阶段进行使用，通过查看该类可以更加清楚。
- com.rftransceiver.db:该包只是描述数据库的创建，并且所有数据库的操作都封装在DBManager.java文件中。
- com.rftransceiver.fragments:和activity包一样，该包统一管理所有的Fragments。
- com.rftransceiver.group:由于建组，加组是一个比较复杂的过程，所以把这部分需要操作的数据单独在一个包里管理。主要是描述了组和组的成员这两个实体，并将这两个实体实现了Parcelable接口，以便将这些实体在Activity间传输。
- com.rftransceiver.util:该包下主要是一些全局变量和公共方法。其中PoolThread.java实现线程池的创建和管理。
- com.source.sounds:该包下管理所有和语音相关的操作，目前最重要的是SoundsEntity.java，该类主要功能是将采集并编码好的语音数据封装成我们自定义的数据包，并通过接口将封装好的数据包传递出去。
- com.source.text:该包和上面的sounds包一样，只是这里管理的是文本信息。
- com.source.parse: 该包下主要管理ble接受到的数据的解析。所有接收到的数据先发送到ParseFactory.java里进行初步判断，初步检查数据包的正确性，并根据我们自定义的包协议来判断出收到的是文本还是语音还是指令包数据，进而在发送到具体的解析类中进行最后的解析。


