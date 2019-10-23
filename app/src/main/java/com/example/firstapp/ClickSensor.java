package com.example.firstapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ClickSensor extends AppCompatActivity implements View.OnClickListener {

    private Button phone_sensor, wear_sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_click_sensor);

        phone_sensor = findViewById(R.id.phone_sensor);
        wear_sensor = findViewById(R.id.wear_sensor);

        phone_sensor.setOnClickListener(this);
        wear_sensor.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.phone_sensor:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.wear_sensor:
                Intent intent1 = new Intent(this, NineAxisSensorStudyThree.class);
                startActivity(intent1);
                break;
        }
    }
}
