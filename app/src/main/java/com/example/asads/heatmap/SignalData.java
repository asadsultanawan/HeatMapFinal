package com.example.asads.heatmap;

import android.util.Log;

public class SignalData {

    private static final String TAG = "SignalData";
    private double signalLevel;
    private double latitude;
    private double longitude;

    public SignalData() {
        this(0, 0, 0);
    }

    public SignalData(double signalStrength, double latitude, double longitude) {
        this.signalLevel = signalStrength;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public SignalData(String[] values) {
        this.signalLevel = Double.parseDouble(values[0]);
        this.latitude = Double.parseDouble(values[1]);
        this.longitude = Double.parseDouble(values[2]);
    }

    public double getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(double signalLevel) {
        this.signalLevel = signalLevel;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getJsonFormat() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("\"signalData\" : ").append(signalLevel).append(", ");
        stringBuilder.append("\"latitude\" : ").append(latitude).append(", ");
        stringBuilder.append("\"longitude\" : ").append(longitude);
        stringBuilder.append("}");
        Log.d(TAG, "convertToQuery: query -> " + stringBuilder.toString());
        return stringBuilder.toString();
    }

    public static String convertToQuery(SignalData signalStrength) {
//        return signalLevel.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");
        stringBuilder.append(signalStrength.signalLevel).append("', '");
        stringBuilder.append(signalStrength.latitude).append("', '");
        stringBuilder.append(signalStrength.longitude);
        stringBuilder.append("'");
        Log.d(TAG, "convertToQuery: signalStrength -> " + signalStrength);
        Log.d(TAG, "convertToQuery: query -> " + stringBuilder.toString());
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return '{' + "signalLevel:" + signalLevel + ", latitude:" + latitude + ", longitude:" + longitude + '}';
    }

}
