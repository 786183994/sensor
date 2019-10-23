package com.example.firstapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import cn.alubi.lpresearch_library.lpsensorlib.LpmsBData;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private static TextView gyr,quat,ypr,linAcc;

    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;
    // 列表
    private ListView lvDevices;
    // 存储搜索到的蓝牙
    private List<String> bluetoothDevices = new ArrayList();
    // listview的adapter
    private MyAdapter arrayAdapter;
    // UUID.randomUUID()随机获取UUID
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 连接对象的名称
    private final String NAME = "LGL";
    // 这里本身即是服务端也是客户端，需要如下类
    private BluetoothSocket clientSocket;
    private BluetoothDevice device;
    // 输出流_客户端需要往服务端输出
    private OutputStream os;
    private InputStream in;

    //-----------------------------------------------------
    final int STATE_IDLE = 0;

    boolean waitForAck = false;
    boolean waitForData = false;
    boolean isConnected = false;

    private int readDataSize = 0;
    private byte[] rawRxBuffer = new byte[256];
    private byte[] rxBuffer = new byte[512];

    private int currentFunction;
    private int currentAddress = 0;
    private byte inBytes[] = new byte[2];
    private int currentLength = 0;
    private int rxIndex = 0;
    private int lrcCheck = 0;

    // LpBus identifiers
    final int PACKET_ADDRESS0 = 0;
    final int PACKET_ADDRESS1 = 1;
    final int PACKET_FUNCTION0 = 2;
    final int PACKET_FUNCTION1 = 3;
    final int PACKET_RAW_DATA = 4;
    final int PACKET_LRC_CHECK0 = 5;
    final int PACKET_LRC_CHECK1 = 6;
    final int PACKET_END = 7;
    final int PACKET_LENGTH0 = 8;
    final int PACKET_LENGTH1 = 9;

    // LPMS-B function registers (most important ones only, currently only LPMS_GET_SENSOR_DATA is used)
    final int LPMS_ACK = 0;
    final int LPMS_NACK = 1;
    final int LPMS_GET_CONFIG = 4;
    final int LPMS_GET_STATUS = 5;
    final int LPMS_GOTO_COMMAND_MODE = 6;
    final int LPMS_GOTO_STREAM_MODE = 7;
    final int LPMS_GOTO_SLEEP_MODE = 8;
    final int LPMS_GET_SENSOR_DATA = 9;
    final int LPMS_SET_TRANSMIT_DATA = 10;

    int rxState = PACKET_END;

    private Vector<byte[]> sensorData = new Vector<byte[]>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gyr = findViewById(R.id.gyr1);
        quat  = findViewById(R.id.quat1);
        ypr = findViewById(R.id.ypr1);
        linAcc = findViewById(R.id.linAcc1);

        initView();
    }

    private void initView() {
        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 判断手机是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
            // 不做提示，强行打开
            // mBluetoothAdapter.enable();
        }

        // 初始化listview
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        lvDevices.setOnItemClickListener(this);

        /**
         * 异步搜索蓝牙设备——广播接收
         */
        // 找到设备的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // 注册广播
        registerReceiver(receiver, filter);
        // 搜索完成的广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // 注册广播
        registerReceiver(receiver, filter);

        // adapter
        arrayAdapter = new MyAdapter(bluetoothDevices, this);
        lvDevices.setAdapter(arrayAdapter);

    }

    public void btnSearch(View v) {
        // 设置进度条
        setProgressBarIndeterminateVisibility(true);
        setTitle("正在搜索...");
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 开始搜索
        mBluetoothAdapter.startDiscovery();
    }

    // 广播接收器
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 收到的广播类型
            String action = intent.getAction();
            // 发现设备的广播
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 从intent中获取设备
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 判断是否配对过
                //if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 添加到列表
                    bluetoothDevices.add(device.getName() + "=====>"
                            + device.getAddress() + "\n");
                    arrayAdapter.notifyDataSetChanged();
                //}
                // 搜索完成
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // 关闭进度条
                setProgressBarIndeterminateVisibility(true);
                setTitle("搜索完成！");
            }
        }
    };

    // 客户端
    @Override
    public void onItemClick(AdapterView parent, View view, int position,long id) {

        // 先获得蓝牙的地址和设备名
        String s = arrayAdapter.getItem(position);
        Log.i("*******",s);
        // 单独解析地址
        String address = s.substring(s.indexOf(">") + 1).trim();
        Log.i("**address***",address);
        // 主动连接蓝牙
        try {
            // 判断是否在搜索,如果在搜索，就取消搜索
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            try {
                // 判断是否可以获得
                if (device == null) {
                    // 获得远程设备
                    device = mBluetoothAdapter.getRemoteDevice(address);
                }
                // 开始连接
                if (clientSocket == null) {
                    clientSocket = device
                            .createRfcommSocketToServiceRecord(MY_UUID);
                    // 连接
                    clientSocket.connect();
                    isConnected = true;
                    // 获得输出流
                    os = clientSocket.getOutputStream();
                    in = clientSocket.getInputStream();
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
            Log.i("****","蓝牙连接情况为：" + clientSocket.isConnected());
            Thread t = new Thread(new StartReadingSensorDataThread());
            t.start();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    // 服务端，需要监听客户端的线程类
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            Toast.makeText(MainActivity.this, String.valueOf(msg.obj),
                    Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };

    // 线程服务类
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        // 输入 输出流
        private OutputStream os;
        private InputStream is;
        public AcceptThread() {
            try {
                serverSocket = mBluetoothAdapter
                        .listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // 截获客户端的蓝牙消息
            try {
                socket = serverSocket.accept(); // 如果阻塞了，就会一直停留在这里
                is = socket.getInputStream();
                os = socket.getOutputStream();
                // 不断接收请求,如果客户端没有发送的话还是会阻塞
                while (true) {
                    // 每次只发送128个字节
                    byte[] buffer = new byte[128];
                    // 读取
                    int count = is.read();
                    // 如果读取到了，我们就发送刚才的那个Toast
                    Message msg = new Message();
                    msg.obj = new String(buffer, 0, count, "utf-8");
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }

    private class  StartReadingSensorDataThread implements Runnable{
        /**
         *  @brief   继续从连接的传感器读取数据
         */
        public void run() {
            int nBytes = 0;
            // Starts state machine thread
            Thread t = new Thread(new ClientStateThread());
            t.start();

//            while (btSocket.isConnected() == true) {
            while (isConnected == true) {
                readDataSize = 0;
                try {
                    readDataSize = in.read(rawRxBuffer);
                } catch (Exception e) {
                    break;
                }
                parseSensorData();

                Object readDate = null;
                try {
                    readDate = read();
                    if (readDate instanceof LpmsBData){
                        new Four((LpmsBData) readDate).run();
                    }else{
                        new Five((byte[]) readDate).run();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // State machine thread class
    private class ClientStateThread implements Runnable {
        int state = STATE_IDLE;
        int timeout = 0;

        public void run() {
            try {
//                while (btSocket.isConnected() == true) {
                while (isConnected == true) {
                    if (waitForAck == false && waitForData == false) {
                        switch (state) {
                            case STATE_IDLE:
                                break;
                        }
                    } else if (timeout > 100) {
                        timeout = 0;
                        state = STATE_IDLE;
                        waitForAck = false;
                        waitForData = false;
                    } else {
                        Thread.sleep(10);
                        ++timeout;
                    }
                }
                // 切断された  被切断了
            } catch (Exception e) {
                isConnected = false;
            }
        }
    }

    //public void parseSensorData(byte[] rawData, int dataSize){
    private void parseSensorData(){
        int lrcReceived = 0;
        byte byteData = 0;

        for (int i=0; i<readDataSize; i++) {
            byteData = rawRxBuffer[i];

            switch (rxState) {
                case PACKET_END:
                    if (byteData == 0x3a) {
                        rxState = PACKET_ADDRESS0;
                    }
                    break;

                case PACKET_ADDRESS0:
                    inBytes[0] = byteData;
                    rxState = PACKET_ADDRESS1;
                    break;

                case PACKET_ADDRESS1:
                    inBytes[1] = byteData;
                    currentAddress = convertRxbytesToInt16(0, inBytes);
                    rxState = PACKET_FUNCTION0;
                    break;

                case PACKET_FUNCTION0:
                    inBytes[0] = byteData;
                    rxState = PACKET_FUNCTION1;
                    break;

                case PACKET_FUNCTION1:
                    inBytes[1] = byteData;
                    currentFunction = convertRxbytesToInt16(0, inBytes);
                    rxState = PACKET_LENGTH0;
                    break;

                case PACKET_LENGTH0:
                    inBytes[0] = byteData;
                    rxState = PACKET_LENGTH1;
                    break;

                case PACKET_LENGTH1:
                    inBytes[1] = byteData;
                    currentLength = convertRxbytesToInt16(0, inBytes);

                    if (currentLength > 512) {
                        rxState = STATE_IDLE;
                        currentLength = 0;
                        break;
                    }

                    rxState = PACKET_RAW_DATA;
                    rxIndex = 0;
                    break;

                case PACKET_RAW_DATA:
                    if (rxIndex == currentLength) {
                        lrcCheck = (currentAddress & 0xffff) + (currentFunction & 0xffff) + (currentLength & 0xffff);

                        for (int j=0; j<currentLength; j++) {
                            lrcCheck += (int) rxBuffer[j] & 0xff;
                        }

                        inBytes[0] = byteData;
                        rxState = PACKET_LRC_CHECK1;
                    } else{
                        try {
                            rxBuffer[rxIndex] = byteData;
                        }catch(ArrayIndexOutOfBoundsException e){
                            e.printStackTrace();
                        }
                        ++rxIndex;
                    }
                    break;

                case PACKET_LRC_CHECK1:
                    inBytes[1] = byteData;

                    lrcReceived = convertRxbytesToInt16(0, inBytes);
                    lrcCheck = lrcCheck & 0xffff;

                    if (lrcReceived == lrcCheck) {
                        parseFunction(); } else {}

                    rxState = PACKET_END;
                    break;

                default:
                    rxState = PACKET_END;
                    break;
            }

        }
    }

    private void parseFunction( ) {
        switch (currentFunction) {
            case LPMS_ACK:
                Log.e("*****","[LpmsBThread] Received ACK");
                break;

            case LPMS_NACK:
                Log.e("*****","[LpmsBThread] Received NACK");
                break;

            case LPMS_GET_CONFIG:
                Log.e("*****","[LpmsBThread] Received GET_CONFIG");
                break;

            case LPMS_GET_STATUS:
                Log.e("*****","[LpmsBThread] Received GET_STATUS");
                break;

            case LPMS_GOTO_COMMAND_MODE:
                Log.e("*****","[LpmsBThread] Received COMMAND_MODE");
                break;

            case LPMS_GOTO_STREAM_MODE:
                Log.e("*****","[LpmsBThread] Received STREAM_MODE");
                break;

            case LPMS_GOTO_SLEEP_MODE:
                Log.e("*****","[LpmsBThread] Received SLEEP_MODE");
                break;

            // If new sensor data is received parse the data
            case LPMS_GET_SENSOR_DATA:
                sensorData.add(rxBuffer);

//                double gyrX = (float) ((short) (((rxBuffer[4 + 1]) << 8) | (rxBuffer[4 + 0] & 0xff))) / 1000.0f*57.2958f;
//                System.out.println("==========sensorData  Data :" + gyrX);
                // for Debug
                //System.out.println("aaaaaaaaaaaaaaaa,"+(long)(((float) convertRxbytesToInt(0, rxBuffer)*0.0025f)*1000));

                break;

            case LPMS_SET_TRANSMIT_DATA:
                break;
            default:
                break;
        }

        waitForAck = false;
        waitForData = false;
    }

    // Converts received 16-bit word to int value           将接收到的16位字转换为int值
    private int convertRxbytesToInt16(int offset, byte buffer[]) {
        int v;
        byte[] t = new byte[2];

        for (int i=0; i<2; ++i) {
            t[1-i] = buffer[i+offset];
        }

        v = (int) ByteBuffer.wrap(t).getShort(0) & 0xffff;

        return v;
    }


    public byte[] read() throws IOException {
        byte[] res = sensorData.get(0);
        return Arrays.copyOf(res,res.length);
    }

}

class  Four implements Runnable{

    LpmsBData b;

    // 时间戳(mobiletime)
    public long mobiletime;
    // 时间戳(sensortime)
    public long sensortime;

    // 角速度X,Y,Z
    public double gyrX;
    public double gyrY;
    public double gyrZ;

    // 加速度X,Y,Z
    public double accX;
    public double accY;
    public double accZ;

    // 地磁気X,Y,ZmagX
    public double magX;
    public double magY;
    public double magZ;

    // 絶対座標角速度X,Y,Z                 绝对坐标角速度X,Y,Z
    public double angX;
    public double angY;
    public double angZ;

    // 4元数角度x,y,z,w
    public double quatX;
    public double quatY;
    public double quatZ;
    public double quatW;

    // ヨーピッチロール角x,y,z           偏转角x,y,z
    public double yprX;
    public double yprY;
    public double yprZ;

    // 線形加速度x,y,z                   线性加速度x,y,z
    public double linAccX;
    public double linAccY;
    public double linAccZ;

    Four(LpmsBData b){
        this.b = b;
    }

    @Override
    public void run() {
        int o = 0;

        // モバイル時間
        mobiletime = System.currentTimeMillis();
        // センサー時間
        sensortime = (long)(b.timestamp*1000);

        o += 4;

        //gyr data
        gyrX = b.gyr[0];
        System.out.println("==sensorData  sensortime : " + sensortime + " gyrX :" + gyrX);
        o += 2;
        gyrY = b.gyr[1];
        o += 2;
        gyrZ = b.gyr[2];
        o += 2;
        Log.i("****","gyrX:===>"+gyrX+"      gyrY:=====>"+gyrY+"    gyrZ:=====>"+gyrZ);

        //Acc data
        accX = b.acc[0];
        o += 2;
        accY = b.acc[1];
        o += 2;
        accZ = b.acc[2];
        o += 2;

        //Magnetometer data
        magX = b.mag[0];
        o += 2;
        magY = b.mag[1];
        o += 2;
        magZ = b.mag[2];
        o += 2;

        //Anguler data
        angX = b.angVel[0];
        o += 2;
        angY = b.angVel[1];
        o += 2;
        angZ = b.angVel[2];
        o += 2;


        //Quat data
        quatW = b.quat[0]/10;
        o += 2;
        quatX = b.quat[1]/10;
        o += 2;
        quatY = b.quat[2]/10;
        o += 2;
        quatZ = b.quat[3]/10;
        o += 2;
        Log.i("****","quatW:====>"+quatW+"    quatX:====>"+quatX+"      quatY:====>"+quatY+"       quatZ:====>"+quatZ);

        //Euler angles
        yprX = b.euler[0];
        o += 2;
        yprY = b.euler[1];
        o += 2;
        yprZ = b.euler[2];
        o += 2;
        Log.i("****","yprX:====>"+yprX+"     yprY:===>"+yprY+"       yprZ:===>"+yprZ);

        //Linear acceleration
        linAccX = b.linAcc[0];
        o += 2;
        linAccY = b.linAcc[1];
        o += 2;
        linAccZ = b.linAcc[2];
        o += 2;
        Log.i("****","linAccX:====>"+linAccX+"       linAccY:====>"+linAccY+"        linAccZ:====>"+linAccZ);


    }
}


class  Five implements Runnable{

    private byte[] b;
    private static final float R2D = 57.2958f;

    // 时间戳(mobiletime)
    public long mobiletime;
    // 时间戳(sensortime)
    public long sensortime;

    // 角速度X,Y,Z
    public double gyrX;
    public double gyrY;
    public double gyrZ;

    // 加速度X,Y,Z
    public double accX;
    public double accY;
    public double accZ;

    // 地磁気X,Y,ZmagX
    public double magX;
    public double magY;
    public double magZ;

    // 絶対座標角速度X,Y,Z                 绝对坐标角速度X,Y,Z
    public double angX;
    public double angY;
    public double angZ;

    // 4元数角度x,y,z,w
    public double quatX;
    public double quatY;
    public double quatZ;
    public double quatW;

    // ヨーピッチロール角x,y,z           偏转角x,y,z
    public double yprX;
    public double yprY;
    public double yprZ;

    // 線形加速度x,y,z                   线性加速度x,y,z
    public double linAccX;
    public double linAccY;
    public double linAccZ;

    public Five(byte[] b){
        this.b = b;
    }

    @Override
    public void run() {
        int o = 0;

        // 移动时间
        mobiletime = System.currentTimeMillis();
        // センサー時間
        sensortime = (long)(((float) convertRxbytesToInt(o, b)*0.0025f)*1000);
        o += 4;

        //gyr data
        gyrX = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f*R2D;
        o += 2;
        gyrY = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f*R2D;
        o += 2;
        gyrZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f*R2D;
        o += 2;
        Log.i("******","gyrX:===>"+gyrX+"      gyrY:=====>"+gyrY+"    gyrZ:=====>"+gyrZ);

        //Acc data
        accX = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        accY = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        accZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        Log.i("****","accX:====>"+accX+"    accY:====>"+accY+"      accZ:====>"+accZ);

        //Magnetometer data
        magX = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        magY = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        magZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        Log.i("****","magX:====>"+magX+"    magY:====>"+magY+"      magZ:====>"+magZ);

        //Anguler data
        angX = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f*R2D;
        o += 2;
        angY = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f*R2D;
        o += 2;
        angZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f*R2D;
        o += 2;
        Log.i("****","angX:====>"+angX+"    angY:====>"+angY+"      angZ:====>"+angZ);

        //Quat data
        quatW = -(float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f;
        o += 2;
        quatX = -(float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f;
        o += 2;
        quatY = -(float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f;
        o += 2;
        quatZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f;
        o += 2;
        Log.i("******","quatW:====>"+quatW+"    quatX:====>"+quatX+"      quatY:====>"+quatY+"       quatZ:====>"+quatZ);

        //Euler angles
        yprX = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f*R2D;
        o += 2;
        yprY = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f*R2D;
        o += 2;
        yprZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 10000.0f*R2D;
        o += 2;
        Log.i("******","yprX:====>"+yprX+"     yprY:===>"+yprY+"       yprZ:===>"+yprZ);

        //Linear acceleration
        linAccX = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        linAccY = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        linAccZ = (float) ((short) (((b[o + 1]) << 8) | (b[o + 0] & 0xff))) / 1000.0f;
        o += 2;
        Log.i("******","linAccX:====>"+linAccX+"       linAccY:====>"+linAccY+"        linAccZ:====>"+linAccZ);

    }

    public int convertRxbytesToInt(int offset, byte buffer[]) {
        int v;
        v = (int) ((buffer[offset] & 0xFF)
                | ((buffer[offset + 1] & 0xFF) << 8)
                | ((buffer[offset + 2] & 0xFF) << 16)
                | ((buffer[offset + 3] & 0xFF) << 24));
        return v;

    }
}
