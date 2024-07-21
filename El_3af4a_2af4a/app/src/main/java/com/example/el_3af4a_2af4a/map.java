package com.example.el_3af4a_2af4a;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button; // Import for Button
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import androidx.drawerlayout.widget.DrawerLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class map extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap myMap;
    private final int FINE_PERMISSION = 1;
    private FusedLocationProviderClient flpc;
    private SearchView searchView;
    private DirectionDeterminer directionDeterminer;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private ImageView menu;
    private TextView towing;
    private boolean start = true;
    private Marker obstacleMarker; // Marker to display the nearest obstacle
    private Button logoutButton;
    private Button buttonNavigate;
    private Button buttonCurrentLocation;
    private LatLng currentLocation;

    private PlacesClient placesClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        directionDeterminer = new DirectionDeterminer();
        flpc = LocationServices.getFusedLocationProviderClient(this);

        // Start the Foreground Service
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        Places.initialize(getApplicationContext(), "AIzaSyAe-hEMqhfUUErVx97dV8UAyI9JrpW4NRg");
        placesClient = Places.createClient(this);
        towing = findViewById(R.id.towing);
        towing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=Towing+Services+near+me&mode=d"));
                Log.d("map", "Google Maps Intent: " + intent.toString());
                startActivity(intent);
            }
        });

        // Location Updates Setup
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000); // Update interval (adjust as needed)
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        logoutButton = findViewById(R.id.logout); // Connect to the Button
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();

                AppUtils.mGoogleSignInClient.signOut() // Use the global instance
                        .addOnCompleteListener(map.this, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                // Now navigate back to MainActivity
                                Toast.makeText(map.this, "Logged Out", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(map.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
            }
        });


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // Update DirectionDeterminer with the new location
                        directionDeterminer.updateLocation(
                                new LatLng(location.getLatitude(), location.getLongitude()),
                                System.currentTimeMillis());

                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        // Update map camera (if needed)
                        if(start) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                            start = false;
                        }
                    }
                }
            }
        };

        searchView = findViewById(R.id.mapSearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String destination  = searchView.getQuery().toString();
                List<Address> addressList = null;
                if(destination != null) {
                    Geocoder geocoder = new Geocoder(map.this);
                    try {
                        addressList = geocoder.getFromLocationName(destination, 1);  //change maxResults
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (!addressList.isEmpty() || addressList == null) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        myMap.addMarker(new MarkerOptions().position(latLng).title(destination));
                        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15)); // Zoom to the current location
                    } else {
                        Toast.makeText(map.this, "Location does not exist", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        menu = findViewById(R.id.menu);
        towing = findViewById(R.id.towing);

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        buttonNavigate = findViewById(R.id.button_navigate);
        buttonCurrentLocation = findViewById(R.id.button_current_location);

        buttonNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Google Maps
                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });

        buttonCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myMap != null && currentLocation != null) {
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));
                }
            }
        });

        getLastLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION);
            return;
        }
        Task<Location> task = flpc.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null) {
                    // Update DirectionDeterminer with new location
                    directionDeterminer.updateLocation(new LatLng(location.getLatitude(), location.getLongitude()), System.currentTimeMillis());

                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(map.this);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        myMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true); // Enable the My Location button and blue dot
        }
        // Fetch and display obstacles from Firestore
        fetchAndDisplayObstacles();
        startLocationUpdates();
    }

    private void fetchAndDisplayObstacles() {
        db.collection("Coordinates")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d("map", "task successful");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                GeoPoint geoPoint = document.getGeoPoint("location");
                                String type = document.getString("type");

                                if (geoPoint != null && type != null) {
                                    LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                    BitmapDescriptor obstacleIcon = null;
                                    if(type.equals("pothole"))
                                        obstacleIcon = getResizedBitmapDescriptor(R.drawable.pothole_black, 80, 85);
                                    else if(type.equals("speed bump"))
                                        obstacleIcon = getResizedBitmapDescriptor(R.drawable.ic_warning, 80, 75);

                                    // Add a marker for each obstacle
                                    myMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title("Obstacle: " + type)
                                            .icon(obstacleIcon));
                                }
                            }
                        } else {
                            Log.d("map", "Error getting documents: ", task.getException());
                            Toast.makeText(map.this, "Failed to retrieve obstacles", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private BitmapDescriptor getResizedBitmapDescriptor(int resId, int width, int height) {
        Drawable drawable = ContextCompat.getDrawable(this, resId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, width, height, false));
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        flpc.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission is denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopForegroundService();
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        stopService(serviceIntent);
    }
}