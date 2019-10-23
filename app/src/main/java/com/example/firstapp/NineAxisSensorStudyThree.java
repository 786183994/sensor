package com.example.firstapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

/**
 * 智能设备监听器
 */
public class NineAxisSensorStudyThree extends AppCompatActivity implements SensorEventListener {

    private TextView gyr,quat,ypr,linAcc;

    // 軸
    private final int AXISX = 0;
    private final int AXISY = 1;
    private final int AXISZ = 2;

    private SensorManager sensorManager;

    public class SDData {
        //    private float[] acc=new float[3];
        float[] acc = new float[4];
        float[] gyr = new float[4];
        float[] lin = new float[4];
        float[] _rotmat = new float[16];
        float[] _rotmatori = new float[3];
        float[] ori = new float[4];
        float[] _rotmatqua = new float[4];
        double[] gps_data = new double[4];
        int index = 0;
    }

    private SDData sensorData = new SDData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nine_axis_sensor_study_three);

        gyr = findViewById(R.id.gyr);
        quat  = findViewById(R.id.quat);
        ypr = findViewById(R.id.ypr);
        linAcc = findViewById(R.id.linAcc);

        boolean isSensorExist = false;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        if (sensorList != null && !sensorList.isEmpty()) {
            for (Sensor sensor : sensorList) {
                System.out.println("sensor.getName()" + sensor.getName() + "======>sensor.getStringType():" + sensor.getStringType());
                switch (sensor.getType()) {
                    case Sensor.TYPE_GYROSCOPE:
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                    case Sensor.TYPE_ROTATION_VECTOR:
                    case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    case Sensor.TYPE_ORIENTATION:
                        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
                        isSensorExist = true;
                        break;
                }
            }
        }
        if (isSensorExist){
            Log.i("*****","有传感器接入");
        }

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                sensorData.gyr[AXISX] = event.values[0]; // X軸
                sensorData.gyr[AXISY] = event.values[1]; // Y軸
                sensorData.gyr[AXISZ] = event.values[2]; // Z軸
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                sensorData.lin[AXISX] = event.values[0]; // X軸
                sensorData.lin[AXISY] = event.values[1]; // Y軸
                sensorData.lin[AXISZ] = event.values[2]; // Z軸
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getQuaternionFromVector(sensorData._rotmatqua, event.values);
                SensorManager.getRotationMatrixFromVector(sensorData._rotmat, event.values);
                SensorManager.getOrientation(sensorData._rotmat, sensorData._rotmatori);
                for (int i = 0; i < 3; i++) {
                    sensorData.ori[i] = sensorData._rotmatori[i];
                }
                break;
        }
        new Three(sensorData, gyr, quat, ypr,linAcc).run();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("NineAxisSensorStudy","onSensorChanged,sensor=" + sensor);
        Log.i("NineAxisSensorStudy","onSensorChanged,accuracy=" + accuracy);
    }

}
    class Three implements Runnable{

        static final float R2D = 57.2958f;
        NineAxisSensorStudyThree.SDData f;
        private TextView gyr,quat,ypr,linAcc;

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

        public Three(NineAxisSensorStudyThree.SDData data,TextView gyr,TextView quat,TextView ypr,TextView linAcc){
            this.f = data;
            this.gyr = gyr;
            this.quat = quat;
            this.ypr = ypr;
            this.linAcc = linAcc;
        }

        @Override
        public void run() {
            //gyr data    陀螺仪
            gyrX = f.gyr[0]*R2D;
            gyrY = f.gyr[1]*R2D;
            gyrZ = f.gyr[2]*R2D;
            gyr.setText("gyrX:===>"+gyrX+"      gyrY:=====>"+gyrY+"    gyrZ:=====>"+gyrZ);

            //Quat data     直线加速度
            quatW = f._rotmatqua[0];
            quatX = -f._rotmatqua[1];
            quatY = f._rotmatqua[2];
            quatZ = -f._rotmatqua[3];
            quat.setText("quatW:====>"+quatW+"    quatX:====>"+quatX+"      quatY:====>"+quatY+"       quatZ:====>"+quatZ);

            //旋转向量
            yprX = f.ori[1]*R2D;
            yprY = f.ori[2]*R2D;
            yprZ = f.ori[0]*R2D;
            ypr.setText("yprX:====>"+yprX+"     yprY:===>"+yprY+"       yprZ:===>"+yprZ);

            //Linear acceleration  线加速度
            linAccX = f.lin[0];
            linAccY = f.lin[1];
            linAccZ = f.lin[2];
            linAcc.setText("linAccX:====>"+linAccX+"       linAccY:====>"+linAccY+"        linAccZ:====>"+linAccZ);
        }
    }
