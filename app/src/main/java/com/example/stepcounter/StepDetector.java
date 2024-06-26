package com.example.stepcounter;

import android.content.Context;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class StepDetector {
    private static final String TAG = "SD";
    private static final int WALKINGTHRESHOLD = 5;
    private static final int JOGGINGTHRESHOLD = 12;
    private static final int RUNNINGTHRESHOLD = 23;
    private static final int SIZEOFLISTDATA = 50;
    private static final int DURATIONOFSTEP = 500;
    private static final float ALPHA = 0.8f;
    private float[] gravity = {0.0f, 0.0f, 9.8f};
    private Context context;
    private StepListener stepListener;

    private ArrayList<AccelerationData> accelerationDataList;
    private ArrayList<AccelerationData> calculatedList;

    public StepDetector(Context context) {
        this.context = context;
        accelerationDataList = new ArrayList<>();
        calculatedList = new ArrayList<>();
    }

    public void RegisterStepListener(StepListener stepListener){
        this.stepListener = stepListener;
    }

    /**
     * The AddAccelerationData method accepts new measurements from the acceleration sensor.
     * If 25 data sets are available, they are processed and 25 data sets are collected again.
     * @param newAccelerationData
     */
    public void AddAccelerationData(AccelerationData newAccelerationData){
        accelerationDataList.add(newAccelerationData);

        if(accelerationDataList.size() >= SIZEOFLISTDATA){
            HandleAccelerationData();
        }
    }

    /**
     * The HandleAccelerationData method detects steps in acceleration data.
     * The four methods CalculateValueAndTime, FindHighPoints, RemoveNearHighPoints, and ExamineStepTypeAndSendResponse are also used for this.
     * The vector length (= speed at a specific point in time) is also calculated for each data set and
     * the time attribute of the data set is changed from nanoseconds since the device started to Unix time (milliseconds).
     * After all data has been processed, the detected steps are output via the interface and
     * the array lists are emptied again so that they can be used again.
     */
    private void HandleAccelerationData(){
        LogToFile.log(context, TAG, "List data: ");
        for (int i = 0; i < accelerationDataList.size(); i++) {
            AccelerationData accelerationData = accelerationDataList.get(i);
            accelerationData = CalculateValueAndTime(accelerationData);
            calculatedList.add(accelerationData);
        }
        LogToFile.log(context, TAG,"List high point: ");
        ArrayList<AccelerationData> highPointList = FindHighPoints();
        LogToFile.log(context, TAG,"Remove near high point: ");
        highPointList = RemoveNearHighPoints(highPointList);
        ExamineStepTypeAndSendResponse(highPointList);

        calculatedList.clear();
        accelerationDataList.clear();
    }
    /**
     * The calculateValueAndTime method calculates the vector length and the Unix timestamp for accelerationData.
     * The corresponding values are changed in accelerationData. Then accelerationData is returned.
     * @param accelerationData Object from which the vector length and the Unix timestamp are calculated.
     * @return accelerationData: The object with changed values.
     */
    private AccelerationData CalculateValueAndTime(AccelerationData accelerationData){
        // Get acceleration data
        float x = accelerationData.GetX();
        float y = accelerationData.GetY();
        float z = accelerationData.GetZ();

        // Gravity filter
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

        // Recalculate acceleration data with gravity filter
        x = x - gravity[0];
        y = y - gravity[1];
        z = z - gravity[2];
        //LogToFile.log(context, TAG,"gravity = " + String.format("%.2f", gravity[0]) + String.format(" %.2f", gravity[1]) + String.format(" %.2f", gravity[2]));
        //gravity = new float[]{0.0f, 0.0f, 0.0f};
        double vectorLength = Math.sqrt(x * x + y * y + z * z);
        accelerationData.SetValue(vectorLength);
        if (vectorLength > 1){
            LogToFile.log(context, TAG,"value = " + String.format("%.2f", vectorLength));

        }
        long time = accelerationData.GetTime();
        long timeOffsetToUnix = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        long unixTimestamp = (time / 1000000L) + timeOffsetToUnix;
        accelerationData.SetTime(unixTimestamp);

        return accelerationData;
    }
    /**
     * The findHighPoints method finds the acceleration records from rawAccelerationData
     * whose total acceleration is higher than the value of WALKINGTHRESHOLD (17). These are
     * added to another ArrayList, which is returned.
     * @return ArrayList: A list with the highest high points.
     */
    private ArrayList<AccelerationData> FindHighPoints(){
        ArrayList<AccelerationData> highPointList = new ArrayList<>();
        ArrayList<AccelerationData> aboveWalkingThresholdList = new ArrayList<>();
        boolean wasAboveThreshold = true;
        for (int i = 0; i < calculatedList.size(); i++) {
            AccelerationData calculatedDataSet = calculatedList.get(i);
            if(calculatedDataSet.GetValue() > WALKINGTHRESHOLD){
                aboveWalkingThresholdList.add(calculatedDataSet);
                wasAboveThreshold = true;
            } else {
                if(wasAboveThreshold && aboveWalkingThresholdList.size() > 0){
                    Collections.sort(aboveWalkingThresholdList, new AccelerationDataSorter());
                    LogToFile.log(context, TAG,"high point: " + aboveWalkingThresholdList.get(aboveWalkingThresholdList.size() - 1).GetValue());
                    highPointList.add(aboveWalkingThresholdList.get(aboveWalkingThresholdList.size() - 1));
                    aboveWalkingThresholdList.clear();
                }
                wasAboveThreshold = false;
            }
        }
        return highPointList;
    }
    /**
     * The RemoveNearHighPoints method goes through the ArrayList accelerationData with the highest points
     * and checks whether there is another "highest peak" within DURATIONOFSTEP(500) milliseconds.
     * If so, the smaller of the two is removed from the list.
     * @param accelerationDataList The list of high points as an ArrayList
     * @return An ArrayList with the removed high points within DURATIONOFSTEP(500) milliseconds
     */
    private ArrayList<AccelerationData> RemoveNearHighPoints(ArrayList<AccelerationData> accelerationDataList){
        ArrayList<Integer> wrongHighPointIndexes = new ArrayList<>();
        for (int i = 0; i < accelerationDataList.size() - 1; i++) {
            if(((accelerationDataList.get(i + 1).GetTime() - accelerationDataList.get(i).GetTime()) < DURATIONOFSTEP)/* || ((accelerationDataList.get(i + 1).GetValue() - accelerationDataList.get(i).GetValue())) < WALKINGTHRESHOLD*/){
                if(accelerationDataList.get(i + 1).GetValue() < accelerationDataList.get(i).GetValue()){
                    wrongHighPointIndexes.add(i + 1);
                } else {
                    wrongHighPointIndexes.add(i);
                }
            }
        }
        for (int i = wrongHighPointIndexes.size() - 1; i >= 0; i--) {
            LogToFile.log(context, TAG, "Remove: " + i + accelerationDataList.get(i).GetValue());
            accelerationDataList.remove(i);
        }
        return accelerationDataList;
    }

    /**
     * The ExamineStepTypeAndSendResponse method checks the total acceleration of the highest peaks from
     * accelerationDataList and sends all detected steps via the registered stepListener interface.
     * If the total acceleration is greater than RUNNINGPEAK, the step type RUNNING is output,
     * if the total acceleration is greater than JOGGINGPEAK, the step type JOGGING is output,
     * otherwise the step type WALKING.
     * @param accelerationDataList An ArrayList with the highest peaks
     */
    private void ExamineStepTypeAndSendResponse(ArrayList<AccelerationData> accelerationDataList){
        for (int i = 0; i < accelerationDataList.size(); i++) {
            AccelerationData highPoint = accelerationDataList.get(i);
            if(highPoint.GetValue() > RUNNINGTHRESHOLD){
                stepListener.Step(highPoint, StepType.RUNNING);
            } else if(highPoint.GetValue() > JOGGINGTHRESHOLD){
                stepListener.Step(highPoint, StepType.JOGGING);
            } else if(highPoint.GetValue() > WALKINGTHRESHOLD){
                stepListener.Step(highPoint, StepType.WALKING);
            } else {
                stepListener.Step(highPoint, StepType.STATIONARY);
            }
        }
    }

    /**
     * The DataSorter class is a comparator class for array lists with AccelerationData.
     * Sorting is performed in ascending order according to the size of the total acceleration value.
     */
    public class AccelerationDataSorter implements Comparator<AccelerationData> {
        /**
         * The compare method compares two sets of AccelerationData based on the
         * total acceleration "value". If the value of the first acceleraationData is greater,
         * 1 is returned. If the value of the second acceleraationData is greater, -1 is returned.
         * Otherwise (if the value of both accelerationData is the same), 0 is returned.
         * @param data1 AccelerationData: The first comparison object
         * @param data2 AccelerationData: The second comparison object
         * @return int 0, 1 or -1; whichever is greater.
         */
        @Override
        public int compare(AccelerationData data1, AccelerationData data2) {
            int returnValue = 0;
            if(data1.GetValue() < data2.GetValue()){
                returnValue = -1;
            } else if(data1.GetValue() > data2.GetValue()){
                returnValue = 1;
            }
            return returnValue;
        }
    }
}
