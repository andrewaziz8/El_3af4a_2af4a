package com.example.el_3af4a_2af4a;

import com.google.firebase.firestore.GeoPoint;

public class Coordinate {
    private GeoPoint location;
    private String type; // Either "speed bump" or "pothole"

    public Coordinate() {

    }

    public Coordinate(GeoPoint location, String type) {
        this.location = location;
        this.type = type;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}