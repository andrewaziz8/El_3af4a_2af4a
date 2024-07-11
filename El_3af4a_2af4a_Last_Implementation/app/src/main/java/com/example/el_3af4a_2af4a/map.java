package com.example.el_3af4a_2af4a;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;



import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;




public class map extends AppCompatActivity implements SensorEventListener, LocationListener {

    private static final String TAG = "SpeedBumpDetection";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private LocationManager locationManager;

    private List<Float> gyroXData = new ArrayList<>();
    private List<Float> gyroYData = new ArrayList<>();
    private List<Float> accYData = new ArrayList<>();

    private long lastTimestamp = 0;
    private static final int WINDOW_SIZE = 95 * 2; // 2 seconds at 95 Hz

    private static final double INTERCEPT = -8.066;
    private static final double GX_M3_COEFF = -1.131;
    private static final double GX_DR_COEFF = 0.000507;
    private static final double GY_M3_COEFF = 2.500;
    private static final double AY_M4_COEFF = -0.7382;

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    //our code
    DrawerLayout drawerLayout;
    ImageView menu;
    TextView towing;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        menu = findViewById(R.id.menu);
        towing = findViewById(R.id.towing);

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        } else {
            Toast.makeText(this, "GPS not enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroXData.add(event.values[0]);
            gyroYData.add(event.values[1]);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accYData.add(event.values[1]);
        }

        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastTimestamp >= 2000) { // 2 seconds
            lastTimestamp = currentTimestamp;
            processWindow();
        }
    }

    private void processWindow() {
        if (gyroXData.size() >= WINDOW_SIZE && gyroYData.size() >= WINDOW_SIZE && accYData.size() >= WINDOW_SIZE) {
            float gxSkewness = calculateSkewness(gyroXData);
            float gxDynamicRange = calculateDynamicRange(gyroXData);
            float gySkewness = calculateSkewness(gyroYData);
            float ayKurtosis = calculateKurtosis(accYData);

            double probability = calculateSpeedBumpProbability(gxSkewness, gxDynamicRange, gySkewness, ayKurtosis);

            Log.d(TAG, "Speed bump probability: " + probability);

            if (probability >= 0.5) {
                // Speed bump detected!
                Toast.makeText(this, "Speed bump detected!", Toast.LENGTH_SHORT).show();
                // You can add actions here, like playing a sound, vibrating, etc.
            }

            // Clear the data buffers for the next window
            gyroXData.clear();
            gyroYData.clear();
            accYData.clear();
        }
    }

    private double calculateSpeedBumpProbability(float gxSkewness, float gxDynamicRange, float gySkewness, float ayKurtosis) {
        double z = INTERCEPT + GX_M3_COEFF * gxSkewness + GX_DR_COEFF * gxDynamicRange + GY_M3_COEFF * gySkewness + AY_M4_COEFF * ayKurtosis;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private float calculateSkewness(List<Float> data) {
        Skewness skewness = new Skewness();
        return (float) skewness.evaluate(toPrimitive(data));
    }

    private float calculateDynamicRange(List<Float> data) {
        Max max = new Max();
        Min min = new Min();
        return (float) (max.evaluate(toPrimitive(data)) - min.evaluate(toPrimitive(data)));
    }

    private float calculateKurtosis(List<Float> data) {
        Kurtosis kurtosis = new Kurtosis();
        return (float) kurtosis.evaluate(toPrimitive(data));
    }

    private double[] toPrimitive(List<Float> data) {
        double[] primitiveArray = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            primitiveArray[i] = data.get(i);
        }
        return primitiveArray;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this example
    }

    @Override
    public void onLocationChanged(Location location) {
        // You can use location information here, for example, to display on a map
        Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
    }

}