package com.example.firstapp;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

//https://blog.csdn.net/xueyuanlzh/article/details/80068971

public class NineAxisSensorStudy extends AppCompatActivity {

    private SensorManager sensorManager;
    private TextView accxText;
    private TextView accyText;
    private TextView acczText;
    private TextView gyroxText;
    private TextView gyroyText;
    private TextView gyrozText;
    private TextView magxText;
    private TextView magyText;
    private TextView magzText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nine_axis_sensor_study);
        accxText = findViewById(R.id.accx);
        accyText = findViewById(R.id.accy);
        acczText = findViewById(R.id.accz);
        gyroxText = findViewById(R.id.gyrox);
        gyroyText = findViewById(R.id.gyroy);
        gyrozText = findViewById(R.id.gyroz);
        magxText = findViewById(R.id.magx);
        magyText = findViewById(R.id.magy);
        magzText = findViewById(R.id.magz);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //加速度计
        Sensor sensora = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listenera, sensora, SensorManager.SENSOR_DELAY_GAME);
        //陀螺仪
        Sensor sensorg = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(listenerg, sensorg, SensorManager.SENSOR_DELAY_GAME);
        //磁场
        Sensor sensorm = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(listenerm, sensorm, SensorManager.SENSOR_DELAY_GAME);
    }

    //加速度
    private SensorEventListener listenera = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float accx = event.values[0];
            float accy = event.values[1];
            float accz = event.values[2];
            accxText.setText("accx:" + accx);
            accyText.setText("accy:" + accy);
            acczText.setText("accz:" + accz);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    //陀螺仪
    private SensorEventListener listenerg = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float gyrox = event.values[0];
            float gyroy = event.values[1];
            float gyroz = event.values[2];
            gyroxText.setText("gyrox:" + gyrox);
            gyroyText.setText("gyroy:" + gyroy);
            gyrozText.setText("gyroz:" + gyroz);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    //磁场
    private SensorEventListener listenerm = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float magx = event.values[0];
            float magy = event.values[1];
            float magz = event.values[2];
            magxText.setText("magx:" + magx);
            magyText.setText("magy:" + magy);
            magzText.setText("magz:" + magz);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(listenera);
            sensorManager.unregisterListener(listenerg);
            sensorManager.unregisterListener(listenerm);
        }
    }

}