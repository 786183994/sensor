package com.example.firstapp;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Half;
import android.util.Log;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

//https://blog.csdn.net/Sunxiaolin2016/article/details/101069990
public class NineAxisSensorStudyTwo extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "NineAxisSensorStudyTwo";
    private TextView magnetometer_value;
    private TextView accelerometer_value_X;
    private TextView accelerometer_value_Y;
    private TextView accelerometer_value_Z;
    private TextView gyroscope_value_X;
    private TextView gyroscope_value_Y;
    private TextView gyroscope_value_Z;
    private SensorManager sensorManager;
    public static DecimalFormat DECIMAL_FORMATTER;

    //Accelerometer
    float[] accelerometerValues = new float[3];
    float[] gravity = new float[3];
    float[] linear_acceleration = new float[3];
    float[] magneticValues = new float[3];

    //Gyroscope
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nine_axis_sensor_study_two);

        Log.i(TAG,"Magnetometer,onCreate");
        magnetometer_value = findViewById(R.id.magnetometer_value);
        accelerometer_value_X = findViewById(R.id.accelerometer_value_X);
        accelerometer_value_Y = findViewById(R.id.accelerometer_value_Y);
        accelerometer_value_Z = findViewById(R.id.accelerometer_value_Z);
        gyroscope_value_X = findViewById(R.id.gyroscope_value_X);
        gyroscope_value_Y = findViewById(R.id.gyroscope_value_Y);
        gyroscope_value_Z = findViewById(R.id.gyroscope_value_Z);
        // define decimal formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER = new DecimalFormat("#.#", symbols);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            // Success! There's a magnetometer.
            Log.i(TAG,"Magnetometer,Success! There's a magnetometer.");
        } else {
            // Failure! No magnetometer.
            Log.i(TAG,"Magnetometer,Failure! No magnetometer.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG,"Magnetometer,onResume");

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            // Success! There's a magnetometer.
            Log.i(TAG,"Magnetometer,Success! There's a magnetometer.");
        } else {
            // Failure! No magnetometer.
            Log.i(TAG,"Magnetometer,Failure! No magnetometer.");
        }
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED),
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.i(TAG,"onSensorChanged,event=" + event);
        if ( event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ) {
            Log.i(TAG,"onSensorChanged,TYPE_MAGNETIC_FIELD");
            // get values for each axes X,Y,Z    获取每个轴x、y、z的值
            float magX = event.values[0];
            float magY = event.values[1];
            float magZ = event.values[2];
            double magnitude = Math.sqrt((magX * magX) + (magY * magY) + (magZ * magZ));
            // set value on the screen    在屏幕上设置值
            magnetometer_value.setText( DECIMAL_FORMATTER.format(magnitude) + " \u00B5Tesla");
        }else if( event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED ){
            Log.i(TAG,"onSensorChanged,TYPE_MAGNETIC_FIELD");
        }else if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ){
            //Log.i(TAG,"onSensorChanged,TYPE_ACCELEROMETER");

            accelerometerValues = event.values.clone();

            // alpha is calculated as t / (t + dT)
            // with t, the low-pass filter's time-constant
            // and dT, the event delivery rate
            final float alpha = 0.8f;
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];
            accelerometer_value_X.setText(DECIMAL_FORMATTER.format( accelerometerValues[0]));
            accelerometer_value_Y.setText(DECIMAL_FORMATTER.format( accelerometerValues[1]));
            accelerometer_value_Z.setText(DECIMAL_FORMATTER.format( accelerometerValues[2]));
        }else if( event.sensor.getType() == Sensor.TYPE_GYROSCOPE ){
            // This time step's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > Half.EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the time step
                // in order to get a delta rotation from this sample over the time step
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
                gyroscope_value_X.setText( DECIMAL_FORMATTER.format( axisX));
                gyroscope_value_Y.setText(  DECIMAL_FORMATTER.format( axisY));
                gyroscope_value_Z.setText( DECIMAL_FORMATTER.format( axisZ));
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current
            // rotation in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG,"onSensorChanged,sensor=" + sensor);
        Log.i(TAG,"onSensorChanged,accuracy=" + accuracy);
    }
}
