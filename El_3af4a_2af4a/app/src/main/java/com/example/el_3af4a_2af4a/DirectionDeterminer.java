package com.example.el_3af4a_2af4a;

import android.location.Location;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DirectionDeterminer {
    private static final int MAX_HISTORY_SIZE = 5;
    private static final float MAX_RELEVANT_DISTANCE = 50f;
    private static final float FRONT_ANGLE_THRESHOLD = 15f;  //15 mario
    private static final double MIN_TIME_THRESHOLD = 10.0;
    private static final double MIN_DISTANCE_THRESHOLD = 3.0; // 5 meters
    private static final float MIN_SPEED_THRESHOLD = 2f;

    private List<TimestampedLocation> locationHistory = new ArrayList<>();
    private Double currentBearing = null;
    private Float currentSpeed = null;

    public static class TimestampedLocation {
        public LatLng location;
        public long timestamp;

        public TimestampedLocation(LatLng location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }

    public static class Obstacle {
        public LatLng location;
        public String type;
        public String id;

        public Obstacle(LatLng location, String type, String id) {
            this.location = location;
            this.type = type;
            this.id = id;
        }
    }

    public void updateLocation(LatLng newLocation, long timestamp) {
        locationHistory.add(new TimestampedLocation(newLocation, timestamp));
        if (locationHistory.size() > MAX_HISTORY_SIZE) {
            locationHistory.remove(0);
        }
        updateBearingAndSpeed();
    }

    private void updateBearingAndSpeed() {
        if (locationHistory.size() < 2) return;

        TimestampedLocation last = locationHistory.get(locationHistory.size() - 1);
        TimestampedLocation secondLast = locationHistory.get(locationHistory.size() - 2);

        currentBearing = SphericalUtil.computeHeading(secondLast.location, last.location);

        float distance = (float) SphericalUtil.computeDistanceBetween(secondLast.location, last.location);
        float timeDiff = (last.timestamp - secondLast.timestamp) / 1000f; // in seconds
        currentSpeed = distance / timeDiff; // in meters per second
    }

    public Optional<Obstacle> getNearestUpcomingObstacle(List<Obstacle> obstacles) {
        if (locationHistory.isEmpty() || currentBearing == null || currentSpeed <= MIN_SPEED_THRESHOLD) { //currentSpeed < MIN_SPEED_THRESHOLD
            return Optional.empty();
        }

        LatLng currentLocation = locationHistory.get(locationHistory.size() - 1).location;
        Optional<Obstacle> nearestObstacle = Optional.empty();
        double nearestDistance = Double.MAX_VALUE;

        for (Obstacle obstacle : obstacles) {
            double distance = SphericalUtil.computeDistanceBetween(obstacle.location, currentLocation);
            double time = distance / currentSpeed;
            if (isInFrontOfCar(obstacle.location, currentLocation)
                    && distance > MIN_DISTANCE_THRESHOLD
                    && (time <= MIN_TIME_THRESHOLD || distance <= MAX_RELEVANT_DISTANCE)) {

//                double distance = SphericalUtil.computeDistanceBetween(currentLocation, obstacle.location);
                if (distance < nearestDistance) {
                    nearestObstacle = Optional.of(obstacle);
                    nearestDistance = distance;
                }
            }
        }

        return nearestObstacle;
    }

    private boolean isInFrontOfCar(LatLng obstacleLocation, LatLng carLocation) {
        double bearingToObstacle = SphericalUtil.computeHeading(carLocation, obstacleLocation);
        double angleDifference = Math.abs(bearingToObstacle - currentBearing);
        return angleDifference <= FRONT_ANGLE_THRESHOLD || angleDifference >= (360 - FRONT_ANGLE_THRESHOLD);
    }


}