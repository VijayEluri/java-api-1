package com.hoccer.api;

import java.util.*;

import org.json.*;

public class Environment {

    private LocationMeasurement mGpsMeasurement;

    public void setGpsMeasurement(double latitude, double longitude, int accuracy, Date timestamp) {
        mGpsMeasurement = new LocationMeasurement(latitude, longitude, accuracy, timestamp);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject environment = new JSONObject();
        if (mGpsMeasurement != null) {
            environment.put("gps", mGpsMeasurement.toJson());
        }
        return environment;
    }

    private class LocationMeasurement {
        private final double latitude;
        private final double longitude;
        private final int    accuracy;
        private final Date   timestamp;

        public LocationMeasurement(double latitude, double longitude, int accuracy, Date timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.timestamp = timestamp;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("accuracy", accuracy);
            json.put("timestamp", timestamp.toGMTString());
            return json;
        }
    }
}
