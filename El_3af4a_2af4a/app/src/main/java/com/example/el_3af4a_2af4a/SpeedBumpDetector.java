package com.example.el_3af4a_2af4a;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpeedBumpDetector implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private List<Float> accYValues = new ArrayList<>(); // To store accelerometer Y values
    private List<Float> gyroXValues = new ArrayList<>(); // To store gyroscope X values
    private List<Float> gyroYValues = new ArrayList<>(); // To store gyroscope Y values

    // Constants (use actual coefficients from the research paper)
    private static final double INTERCEPT = -8.066;
    private static final double GXM3_COEF = -1.131;
    private static final double GXDR_COEF = 0.0005070;
    private static final double GYM3_COEF = 2.500;
    private static final double AYM4_COEF = -0.7382;

    // Window size for data collection (2 seconds in milliseconds)
    private static final int WINDOW_SIZE_MS = 2000;

    private long windowStartTime = 0;
    private SpeedBumpListener speedBumpListener; // Interface for event handling

    public interface SpeedBumpListener {
        void onSpeedBumpDetected(); // Method called when speed bump is detected
    }

    public SpeedBumpDetector(Context context, SpeedBumpListener listener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        speedBumpListener = listener;
    }

    public void startDetection() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        windowStartTime = System.currentTimeMillis(); // Start time for data window
    }

    public void stopDetection() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accYValues.add(event.values[1]); // Store Y-axis value (vertical acceleration)
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroXValues.add(event.values[0]);
            gyroYValues.add(event.values[1]);
        }

        // Check if the 2-second window has passed
        if (System.currentTimeMillis() - windowStartTime >= WINDOW_SIZE_MS) {
            // 4. Calculate Features
            double gxM3 = calculateSkewness(gyroXValues);
            double gxDR = calculateDynamicRange(gyroXValues);
            double gyM3 = calculateSkewness(gyroYValues);
            double ayM4 = calculateKurtosis(accYValues);

            // 5. Apply Model
            double prediction = applyModel(gxM3, gxDR, gyM3, ayM4);

            // 6. Check for Speed Bump
            if (prediction >= 0.5) { // You can adjust this threshold
                // Speed Bump detected!
                if (speedBumpListener != null) {
                    speedBumpListener.onSpeedBumpDetected();
                }
            }

            // Clear data and reset the window timer
            accYValues.clear();
            gyroXValues.clear();
            gyroYValues.clear();
            windowStartTime = System.currentTimeMillis();
        }
    }

    // 7. Statistical Feature Calculation Methods
    private double calculateMean(List<Float> data) {
        if (data.isEmpty()) return 0.0;
        float sum = 0;
        for (float value : data) {
            sum += value;
        }
        return sum / data.size();
    }

    private double calculateVariance(List<Float> data) {
        if (data.size() < 2) return 0.0;
        double mean = calculateMean(data);
        double sumOfSquaredDiffs = 0;
        for (float value : data) {
            sumOfSquaredDiffs += Math.pow((value - mean), 2);
        }
        return sumOfSquaredDiffs / (data.size() - 1);
    }

    private double calculateSkewness(List<Float> data) {
        if (data.size() < 3) return 0.0;
        double mean = calculateMean(data);
        double variance = calculateVariance(data);
        if (variance == 0) return 0.0; // To avoid division by zero

        double sumOfCubedDiffs = 0;
        for (float value : data) {
            sumOfCubedDiffs += Math.pow((value - mean), 3);
        }
        return (sumOfCubedDiffs / data.size()) / Math.pow(variance, 1.5);
    }

    private double calculateKurtosis(List<Float> data) {
        if (data.size() < 4) return 0.0;
        double mean = calculateMean(data);
        double variance = calculateVariance(data);
        if (variance == 0) return 0.0;

        double sumOfFourthPowerDiffs = 0;
        for (float value : data) {
            sumOfFourthPowerDiffs += Math.pow((value - mean), 4);
        }
        return (sumOfFourthPowerDiffs / data.size()) / Math.pow(variance, 2) - 3;
    }

    private double calculateDynamicRange(List<Float> data) {
        if (data.isEmpty()) return 0.0;
        return Collections.max(data) - Collections.min(data);
    }


    // 8. Apply Logistic Regression Model
    private double applyModel(double gxM3, double gxDR, double gyM3, double ayM4) {
        double z = INTERCEPT + (GXM3_COEF * gxM3) + (GXDR_COEF * gxDR)
                + (GYM3_COEF * gyM3) + (AYM4_COEF * ayM4);

        return 1.0 / (1.0 + Math.exp(-z)); // Logistic function (sigmoid)
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used for this example, but you may need to implement if required.
    }
}