package com.example.stepcounter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener{

    private TextView tvJoggingCounter, tvAcivityType,tvRunningCounter, tvWalkingCounter;
    private Button btnStart, btnStop, btnFinish,btnGuiline;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private SensorEventListener sensorEventListener;
    private StepDetector stepDetector;
    private StepListener stepListener;
    private ArrayList<AccelerationData> accelerationDataArrayList;
    private boolean isAccelerometerSensorPresent;
    private int amountOfStepRunning = 0;
    private int amountOfStepJogging = 0;
    private int amountOfStepWalking = 0;
    private boolean isStop = false;
	
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvAcivityType = (TextView) findViewById(R.id.tv_ActivityType);
        tvJoggingCounter = (TextView) findViewById(R.id.tv_JoggingCounter);
        tvWalkingCounter = findViewById(R.id.tv_WalkingCounter);
        tvRunningCounter = findViewById(R.id.tv_RunningCounter);
        btnStart = (Button) findViewById(R.id.btn_Start);
        btnStop = (Button) findViewById(R.id.btn_Stop);
        btnFinish = (Button) findViewById(R.id.btn_Finish);
        btnGuiline = findViewById(R.id.btn_guiline);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometerSensor = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //sensorManager.registerListener(this, accelerometerSensor, sensorManager.SENSOR_DELAY_UI);
            sensorEventListener = this;
            isAccelerometerSensorPresent = true;
        } else {
            Toast.makeText(this, "There is no Accelerometer sensor!!!", Toast.LENGTH_SHORT).show();
            isAccelerometerSensorPresent = false;
            finish();
        }

        sensorEventListener = this;
        stepListener = this;

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "Starting!", Toast.LENGTH_SHORT).show();

                isStop = false;
                if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                    accelerometerSensor = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    //sensorManager.registerListener(this, accelerometerSensor, sensorManager.SENSOR_DELAY_UI);
                    isAccelerometerSensorPresent = true;

                } else {
                    Toast.makeText(getBaseContext(), "There is no Accelerometer sensor!!!", Toast.LENGTH_SHORT).show();
                    isAccelerometerSensorPresent = false;
                    finish();
                }
                sensorManager.registerListener(sensorEventListener, accelerometerSensor, sensorManager.SENSOR_DELAY_UI);
                accelerationDataArrayList = new ArrayList<>();
                stepDetector = new StepDetector(getBaseContext());
                stepDetector.RegisterStepListener(stepListener);
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                btnFinish.setEnabled(true);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStop = true;
                Toast.makeText(MainActivity.this, "Stopping!", Toast.LENGTH_SHORT).show();
                btnStart.setText("Continue");
                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.unregisterListener(sensorEventListener);
                //accelerationDataArrayList.clear();
                isStop = true;
                amountOfStepRunning = 0;
                amountOfStepJogging = 0;
                amountOfStepWalking = 0;
                tvWalkingCounter.setText("Walking: " + amountOfStepWalking);
                tvJoggingCounter.setText("Jogging: " + amountOfStepJogging);
                tvRunningCounter.setText("Running: " + amountOfStepRunning);
                btnStart.setText("Start");
                btnFinish.setEnabled(false);
                btnStart.setEnabled(true);

            }
        });
        btnGuiline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Guilines.class);
                startActivity(intent);
            }
        });
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event != null) {
            AccelerationData newAccelerationData = new AccelerationData();
            newAccelerationData.SetX(event.values[0]);
            newAccelerationData.SetY(event.values[1]);
            newAccelerationData.SetZ(event.values[2]);
            newAccelerationData.SetTime(event.timestamp);

            if (newAccelerationData != null && stepDetector != null) {
                accelerationDataArrayList.add(newAccelerationData);
                if (isStop == false) {
                    stepDetector.AddAccelerationData(newAccelerationData);
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isAccelerometerSensorPresent) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    protected void onResume() {
        super.onResume();
        if (isAccelerometerSensorPresent) {
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void Step(AccelerationData accelerationData, StepType stepType) {
        // Step event coming back from StepDetector
        String activityType = "";
        switch (stepType) {
            case RUNNING:
                activityType = "Running";
                ++amountOfStepRunning;
                break;
            case JOGGING:
                activityType = "Jogging";
                ++amountOfStepJogging;
                break;
            case WALKING:
                activityType = "Walking";
                ++amountOfStepWalking;
                break;
            case STATIONARY:
                activityType = "Stationary";
                break;
        }
        tvAcivityType.setText(activityType+"");
        tvWalkingCounter.setText("Walking: " + amountOfStepWalking);
        tvJoggingCounter.setText("Jogging: " + amountOfStepJogging);
        tvRunningCounter.setText("Running: " + amountOfStepRunning);
        /*mViewModel.setAmountOfSteps(mViewModel.getAmountOfSteps() + 1);
        textView_amount_steps.setText(String.valueOf(mViewModel.getAmountOfSteps()));
        if(stepType == StepType.WALKING) {
            mViewModel.setWalkingSteps(mViewModel.getWalkingSteps() + 1);
            textView_type_of_step.setText(getResources().getText(R.string.walking));
        }
        else if(stepType == StepType.JOGGING) {
            mViewModel.setJoggingSteps(mViewModel.getJoggingSteps() + 1);
            textView_type_of_step.setText(getResources().getText(R.string.jogging));
        }
        else {
            mViewModel.setRunningSteps(mViewModel.getRunningSteps() + 1);
            textView_type_of_step.setText(getResources().getText(R.string.running));
        }*/
    }
}