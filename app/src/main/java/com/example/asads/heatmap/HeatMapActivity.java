package com.example.asads.heatmap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class HeatMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "HeatMapActivity";
    private static final int[] COLORS = {
            Color.argb(100, 0, 0, 0),
            Color.argb(100, 255, 255, 255),
            Color.argb(100, 0, 0, 255),
            Color.argb(100, 0, 255, 0),
            Color.argb(100, 255, 0, 0)
    };
    private GoogleMap googleMap;
    private ArrayList<Circle> circles = new ArrayList<>();
    private HashMap<String, DataSet> mLists = new HashMap<>();
    private String jsonData = "";
    private double latitude = 0, longitude = 0;
    private SeekBar seekbar;
    private int circleSize = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heatmaps_demo);

        seekbar = findViewById(R.id.seekBar);
        seekbar.setProgress(circleSize);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            jsonData = intent.getExtras().getString("jsonDataToString");
            latitude = intent.getExtras().getDouble("latitude");
            longitude = intent.getExtras().getDouble("longitude");
        }
        setUpMap();

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: starts");
                Log.d(TAG, "onProgressChanged: progress -> " + progress);
                removeCircles();
                circleSize = (progress > 10) ? progress : 1;
                initializeCircles(circleSize);
                Log.d(TAG, "onProgressChanged: ends");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMap();
    }

    private void setUpMap() {
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (googleMap != null) {
            return;
        }
        googleMap = map;
        startDemo();
    }

    protected void startDemo() {
        Log.d(TAG, "startDemo: start");
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        try {
            mLists.put(getString(R.string.gsm_signal), new DataSet(readItems(jsonData)));
            mLists.put(getString(R.string.police_stations), new DataSet(readItems(R.raw.police)));
            mLists.put(getString(R.string.medicare), new DataSet(readItems(R.raw.medicare)));
        } catch (JSONException e) {
            Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "startDemo: e -> " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Big Exception.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "startDemo: e -> " + e.getMessage());
            e.printStackTrace();
        }

        initializeCircles(circleSize);
        Log.d(TAG, "startDemo: end");
    }

    public void initializeCircles(int size) {
        Log.d(TAG, "initializeCircles: starts");
        ArrayList<SignalData> data = mLists.get(getResources().getString(R.string.gsm_signal)).getData();
        for (int i = 0; i < data.size(); i++) {
            circles.add(drawCircle(data.get(i), size));
        }
        Log.d(TAG, "initializeCircles: ends");
    }

    public Circle drawCircle(SignalData signalData, int size) {
        Log.d(TAG, "drawCircle: starts");
        LatLng latLng = new LatLng(signalData.getLatitude(), signalData.getLongitude());
        CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .fillColor(COLORS[(int) signalData.getSignalLevel()])
                .radius(size)
                .strokeWidth(1)
                .strokeColor(Color.BLACK);
        Log.d(TAG, "drawCircle: ends");
        return googleMap.addCircle(circleOptions);
    }

    public void removeCircles() {
        for (int i = 0; i < circles.size(); i++) {
            circles.get(i).remove();
        }
    }

    private ArrayList<SignalData> readItems(int resource) throws JSONException {
        Log.d(TAG, "readItems: start");
        ArrayList<SignalData> list = new ArrayList<>();
        InputStream inputStream = getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        Log.d(TAG, "readItems: json -> " + json);
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new SignalData(1, lat, lng));
        }
        Log.d(TAG, "readItems: end");
        return list;
    }

    private ArrayList<SignalData> readItems(String resource) throws JSONException {
        Log.d(TAG, "readItems: start");
        ArrayList<SignalData> list = new ArrayList<>();
        String json = new Scanner(resource).useDelimiter("\\A").next();
        Log.d(TAG, "readItems: json -> " + json);
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double signalLevel = object.getDouble("signalData");
            double lat = object.getDouble("latitude");
            double lng = object.getDouble("longitude");
            Log.d(TAG, "readItems: signalLevel -> " + signalLevel + ", lat -> " + lat + ", lng -> " + lng);
            list.add(new SignalData(signalLevel, lat, lng));
        }
        Log.d(TAG, "readItems: list -> " + list);
        Log.d(TAG, "readItems: end");
        return list;
    }

    private class DataSet {
        private ArrayList<SignalData> mDataset;

        public DataSet(ArrayList<SignalData> dataSet) {
            Log.d(TAG, "DataSet: start");
            this.mDataset = dataSet;
            Log.d(TAG, "DataSet: end");
        }

        public ArrayList<SignalData> getData() {
            return mDataset;
        }
    }

}
