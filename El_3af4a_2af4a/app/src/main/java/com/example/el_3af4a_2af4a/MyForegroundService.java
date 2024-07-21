package com.example.el_3af4a_2af4a;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.widget.RemoteViews;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MyForegroundService extends Service {

    private FirebaseFirestore db;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private DirectionDeterminer directionDeterminer;
    private DirectionDeterminer.Obstacle currentObstacle = null;
    private double currentObstacleDistance = 0;

    private final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final double MIN_DISTANCE_THRESHOLD = 3.0; // 5 meters

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        directionDeterminer = new DirectionDeterminer();

        // Create location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        directionDeterminer.updateLocation(latLng, System.currentTimeMillis());
                        displayNearestObstacle(latLng);
                    }
                }
            }
        };

        // Request location updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

//        checkAndPromptNotificationSettings();
    }

    private void checkAndPromptNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel channel = manager.getNotificationChannel(Utils.CHANNEL_ID);
            if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_HIGH) {
                Toast.makeText(this, "Please enable high priority notifications for this app", Toast.LENGTH_LONG).show();
                openNotificationSettings();
            }
        }
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensures the settings activity starts in a new task
        startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, Utils.CHANNEL_ID)
                .setContentTitle("EL 3af4a 2af4a")
                .setContentText("Currently Detecting Obstacles!")
                .setSmallIcon(R.drawable.pothole)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    Utils.CHANNEL_ID,
                    Utils.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            serviceChannel.setDescription(Utils.CHANNEL_DESC);
            serviceChannel.enableLights(true);
            serviceChannel.enableVibration(true);
            serviceChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void displayNearestObstacle(LatLng currentLocation) {
        // There is a current obstacle detected from the database
        if(currentObstacle != null){
            double newDistance = SphericalUtil.computeDistanceBetween(currentLocation, currentObstacle.location);
            if(newDistance > currentObstacleDistance){ // moving away from the bump
                currentObstacle = null;
                currentObstacleDistance = 0;
                return;
            }

            currentObstacleDistance = newDistance;
            if (currentObstacleDistance <= MIN_DISTANCE_THRESHOLD) {
                // User has reached the obstacle, display feedback notification
                RemoteViews feedbackNotificationLayout = new RemoteViews(getPackageName(), R.layout.feedback);

                // Create intents for Yes and No actions
                Intent yesIntent = new Intent(this, NotificationActionReceiver.class);
                yesIntent.setAction("YES_ACTION");
                yesIntent.putExtra("ID", currentObstacle.id);
                yesIntent.putExtra("notification", 3);
                PendingIntent yesPendingIntent = PendingIntent.getBroadcast(this, 0, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Intent noIntent = new Intent(this, NotificationActionReceiver.class);
                noIntent.setAction("NO_ACTION");
                noIntent.putExtra("ID", currentObstacle.id);
                noIntent.putExtra("notification", 3);
                PendingIntent noPendingIntent = PendingIntent.getBroadcast(this, 0, noIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Set the Yes and No buttons in the notification layout
                feedbackNotificationLayout.setOnClickPendingIntent(R.id.yes_button, yesPendingIntent);
                feedbackNotificationLayout.setOnClickPendingIntent(R.id.no_button, noPendingIntent);

                NotificationCompat.Builder feedbackBuilder = new NotificationCompat.Builder(MyForegroundService.this, Utils.CHANNEL_ID)
                        .setSmallIcon(R.drawable.splash)
                        .setCustomContentView(feedbackNotificationLayout)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_ALL);
                NotificationManager feedbackNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                feedbackNotificationManager.notify(3, feedbackBuilder.build());

                // Reset the current obstacle
                currentObstacle = null;
                currentObstacleDistance = 0;
            }
        }

        // There is no current speed bump, so fetch database
        else {
            getObstacles(new FirestoreCallback() {
                @Override
                public void onCallback(List<DirectionDeterminer.Obstacle> obstacles) {
                    currentObstacle = directionDeterminer.getNearestUpcomingObstacle(obstacles).orElse(null);
                    if (currentObstacle != null) {
                        currentObstacleDistance = SphericalUtil.computeDistanceBetween(currentLocation, currentObstacle.location);
                    }
                }
            });
        }

        if (currentObstacle != null) {
            // Notify the user about the nearest obstacle
            RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.alert);
            String distanceText = String.format("%d m", (int) currentObstacleDistance);
            notificationLayout.setTextViewText(R.id.distance, distanceText);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MyForegroundService.this, Utils.CHANNEL_ID)
                    .setSmallIcon(R.drawable.splash)
                    .setCustomContentView(notificationLayout)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setTimeoutAfter(5000)
                    .setDefaults(Notification.DEFAULT_ALL);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(2, builder.build());
        }
    }

    private void getObstacles(FirestoreCallback firestoreCallback) {
        List<DirectionDeterminer.Obstacle> obstacles = new ArrayList<>();

        db.collection("Coordinates")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            GeoPoint geoPoint = document.getGeoPoint("location");
                            String type = document.getString("type");

                            if (geoPoint != null && type != null) {
                                LatLng latandLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                obstacles.add(new DirectionDeterminer.Obstacle(latandLng, type, document.getId()));
                            }
                        }
                        firestoreCallback.onCallback(obstacles);
                    } else {
                        Toast.makeText(MyForegroundService.this, "Failed to retrieve obstacles", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private interface FirestoreCallback {
        void onCallback(List<DirectionDeterminer.Obstacle> obstacles);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        // Remove location updates
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

