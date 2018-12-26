package com.example.asads.heatmap;

import android.Manifest;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "MainActivity";
    private static final int READ_REQUEST_CODE = 42;
    private static final int FINE_LOCATION_REQUEST_CODE = 43;
    private static final int COARSE_LOCATION_REQUEST_CODE = 44;
    private static final int WRITE_REQUEST_CODE = 45;

    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    ActionBar actionBar;
    private LinkedList<SignalData> signalData = new LinkedList<>();
    private TextView locationTextView, signalTextView;
    private EditText databaseEditText;
    private double latitude = 0, longitude = 0, signalLevel = 0;
    private int locationCount = 0, signalCount = 0;
    private boolean isGettingLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3C5A99")));
        DatabaseHandler.initialize(this);
        setTitle(getResources().getString(R.string.app_name) + " (Not Recording)");

        locationTextView = findViewById(R.id.locationTextView);
        signalTextView = findViewById(R.id.signalTextView);
        databaseEditText = findViewById(R.id.databaseEditText);
        databaseEditText.setFocusable(false);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST_CODE);
        }

        signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
        signalCount = signalData.size();
        locationCount = signalData.size();
        if (signalCount > 0) {
            SignalData lastSignalData = signalData.getLast();
            latitude = lastSignalData.getLatitude();
            longitude = lastSignalData.getLongitude();
            signalLevel = lastSignalData.getSignalLevel();
        }
        locationTextView.setText(new StringBuilder("Count -> " + locationCount + "; Latitude: " + latitude + "; Longitude: " + longitude));
        signalTextView.setText(new StringBuilder("SignalCount -> " + signalCount + "; Signal Level: " + signalLevel));
        databaseEditText.setText(jsonDataToString(signalData));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.startRecording:
                if (!isGettingLocation) {
                    try {
                        isGettingLocation = true;
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, MainActivity.this);
                    } catch (SecurityException e) {
                        Toast.makeText(MainActivity.this, "Error Getting Location", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show();
                    setTitle(getResources().getString(R.string.app_name) + " (Recording)");
                }
                break;
            case R.id.stopRecording:
                if (isGettingLocation) {
                    isGettingLocation = false;
                    locationManager.removeUpdates(this);
                    Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
                    setTitle(getResources().getString(R.string.app_name) + " (Not Recording)");
                }
                break;
            case R.id.showHeatMap:
                locationManager.removeUpdates(this);
                Intent intent = new Intent(this, HeatMapActivity.class);
//                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                intent.putExtra("jsonDataToString", jsonDataToString(signalData));
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                startActivity(intent);
                break;
            case R.id.loadDatabase:
                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                databaseEditText.setText(jsonDataToString(signalData));
                break;
            case R.id.deleteDatabase:
                DatabaseHandler.deleteTable(this);
                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                latitude = 0;
                longitude = 0;
                signalLevel = 0;
                signalCount = 0;
                locationCount = 0;
                locationTextView.setText(new StringBuilder("Count -> " + locationCount + "; Latitude: " + latitude + "; Longitude: " + longitude));
                signalTextView.setText(new StringBuilder("SignalCount -> " + signalCount + "; Signal Level: " + signalLevel));
                databaseEditText.setText(jsonDataToString(signalData));
                break;
            case R.id.saveDataToFile:
                if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
                } else {
                    Calendar calendar = Calendar.getInstance();
                    Log.d(TAG, "onOptionsItemSelected: calender.getTime -> " + calendar.getTime());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String formattedDate = dateFormat.format(calendar.getTime());
                    Toast.makeText(this, formattedDate, Toast.LENGTH_SHORT).show();
                    saveFile(this, formattedDate + ".txt", jsonDataToString(signalData));
                }
                break;
            case R.id.loadDataFromFile:
                if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_REQUEST_CODE);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("*/*");
                        startActivityForResult(intent, READ_REQUEST_CODE);
                    }
                }
                break;
            case R.id.cleanData:
                databaseEditText.setText("");
                break;
            case R.id.about:
                Log.i(TAG, "onOptionsItemSelected : " + "case about");
                //Toast.makeText(this, "Application for showing Heat Map for Cellular Signals.", Toast.LENGTH_SHORT).show();
                Intent aboutintent = new Intent(getApplicationContext(),About.class);
                startActivity(aboutintent);
            default:
                Log.d(TAG, "onOptionsItemSelected: default case");
        }
        return true;
    }

    public String jsonDataToString(LinkedList<SignalData> signalData) {
        StringBuilder databaseText = new StringBuilder();
        databaseText.append("[\n");
        if (signalData.size() > 0) {
            for (int i = 0; i < signalData.size() - 1; i++) {
                databaseText.append(signalData.get(i).getJsonFormat()).append(",\n");
            }
            databaseText.append(signalData.getLast().getJsonFormat());
        }
        databaseText.append("\n]");
        return databaseText.toString();
    }

    public LinkedList<SignalData> stringToJsonData(String jsonString) {
        LinkedList<SignalData> signalDataList = new LinkedList<>();
        LinkedList<String> signalStringList = new LinkedList<>();
        boolean signalStringStart = false;
        StringBuilder signalDataString = new StringBuilder();
        for (int i = 0; i < jsonString.length(); i++) {
            if (jsonString.charAt(i) == '{') {
                signalStringStart = true;
                signalDataString = new StringBuilder();
            } else if (jsonString.charAt(i) == '}') {
                signalStringStart = false;
                signalStringList.add(signalDataString.toString());
            } else if (signalStringStart) {
                signalDataString.append(jsonString.charAt(i));
            }
        }

        String tempString;
        int index1, index2;
        double[] data = new double[3];
        for (int i = 0; i < signalStringList.size(); i++) {
            tempString = signalStringList.get(i);
            Log.d(TAG, "stringToJsonData: " + i + " -> " + tempString);
            for (int j = 0; j < 3; j++) {
                index1 = tempString.indexOf(':');
                index2 = tempString.indexOf(',');
                data[j] = (index2 >= 0) ? Double.parseDouble(tempString.substring(index1 + 1, index2)) : Double.parseDouble(tempString.substring(index1 + 1));
                tempString = tempString.substring(index2 + 1);
            }
            signalDataList.add(new SignalData(data[0], data[1], data[2]));
        }
        return signalDataList;
    }

    public boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED;
    }

    public void saveFile(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Signal HeatMap");
            if (!root.exists()) {
                Toast.makeText(context, "Directory Created -> " + root.mkdir(), Toast.LENGTH_SHORT).show();
            }
            File file = new File(root, sFileName);
            if (!file.exists()) {
                Toast.makeText(context, "File Created -> " + file.createNewFile(), Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "saveFile: file -> " + file);
            FileWriter writer = new FileWriter(file);
            writer.write(sBody);
            writer.flush();
            writer.close();
            Toast.makeText(context, "Saved as " + sFileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.d(TAG, "saveFile: e.getMessage() -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LinkedList<SignalData> loadFile(Context context, Uri fileUri) {
        LinkedList<SignalData> tempSignalData = new LinkedList<>();
        Scanner fileReader;
        StringBuilder fileData = new StringBuilder();
        try {
//            Log.d(TAG, "loadFile: fileUri -> " + fileUri);
//            Log.d(TAG, "loadFile: fileUri.getPath -> " + fileUri.getPath());
//            Log.d(TAG, "loadFile: fileUri.getAuthority -> " + fileUri.getAuthority());
            String pathSegment = fileUri.getLastPathSegment();
//            Log.d(TAG, "loadFile: fileUri.getLastPathSegment -> " + pathSegment);
            pathSegment = pathSegment.substring(pathSegment.indexOf(':') + 1);
//            Log.d(TAG, "loadFile: fileUri.getLastPathSegment -> " + pathSegment);
            File file = new File(Environment.getExternalStorageDirectory(), pathSegment);
//            Log.d(TAG, "loadFile: file -> " + file);
//            Log.d(TAG, "loadFile: file.getAbsolutePath -> " + file.getAbsolutePath());
            if (!file.exists()) {
                Toast.makeText(context, "Error Opening File", Toast.LENGTH_SHORT).show();
            }
            fileReader = new Scanner(file);
            while (fileReader.hasNext()) {
                fileData.append(fileReader.next());
            }
            fileReader.close();
            tempSignalData = stringToJsonData(fileData.toString());
            databaseEditText.setText(jsonDataToString(tempSignalData));
//            Log.d(TAG, "loadFile: fileData -> " + fileData);
        } catch (IOException e) {
            Log.d(TAG, "loadFile: e.getMessage() -> " + e.getMessage());
            e.printStackTrace();
        }
        return tempSignalData;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                Log.d(TAG, "onActivityResult: uri -> " + uri);
                signalData = loadFile(this, uri);
                Log.d(TAG, "onActivityResult: signalData.size() -> " + signalData.size());
                Log.d(TAG, "onActivityResult: signalData -> " + signalData);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: starts");
        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_REQUEST_CODE);
        }
        try {
            CellInfo cellInfo = telephonyManager.getAllCellInfo().get(0);
            CellSignalStrength cellSignalStrength = null;
            if (cellInfo instanceof CellInfoGsm) {
                Log.d(TAG, "onLocationChanged: CellInfoGsm detected");
                cellSignalStrength = ((CellInfoGsm) cellInfo).getCellSignalStrength();
            } else if (cellInfo instanceof CellInfoCdma) {
                Log.d(TAG, "onLocationChanged: CellInfoCdma detected");
                cellSignalStrength = ((CellInfoCdma) cellInfo).getCellSignalStrength();
            } else if (cellInfo instanceof CellInfoLte) {
                Log.d(TAG, "onLocationChanged: CellInfoCdma detected");
                cellSignalStrength = ((CellInfoLte) cellInfo).getCellSignalStrength();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (cellInfo instanceof CellInfoWcdma) {
                    Log.d(TAG, "onLocationChanged: CellInfoWcdma detected");
                    cellSignalStrength = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
                }
            }
            if (cellSignalStrength != null) {
                signalLevel = cellSignalStrength.getLevel();
            }
            Log.d(TAG, "onLocationChanged: signalLevel -> " + signalLevel);
        } catch (Exception e) {
            Toast.makeText(this, "Error Reading Signal Strength", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onLocationChanged: e -> " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "onLocationChanged: signalLevel -> " + signalLevel + ", longitude -> " + longitude + ", latitude -> " + latitude);
        locationTextView.setText(new StringBuilder("Count -> " + (++locationCount) + "; Latitude: " + latitude + "; Longitude: " + longitude));
        signalTextView.setText(new StringBuilder("SignalCount -> " + (++signalCount) + "; Signal Level: " + signalLevel));
        signalData.add(new SignalData(signalLevel, latitude, longitude));
        DatabaseHandler.insertData(MainActivity.this, signalData.getLast());
        signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
        databaseEditText.setText(jsonDataToString(signalData));
        Log.d(TAG, "onLocationChanged: ends");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider + ", status -> " + status + ", extras -> " + extras);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider);
    }

}
