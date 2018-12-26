package com.example.asads.heatmap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedList;

public class DatabaseHandler {

    private static final String TAG = "DatabaseHandler";
    private static SQLiteDatabase sqLiteDatabase;
    private static String DATABASE_NAME = "SignalData";
    private static String TABLE_NAME = "signalTable";

    public static void initialize(Context context) {
        Log.d(TAG, "initialize: starts");
        try {
            sqLiteDatabase = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
//            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (id INTEGER PRIMARY KEY, signalData VARCHAR, longitude VARCHAR, latitude VARCHAR)");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        Log.d(TAG, "initialize: ends");
    }

    public static void deleteTable(Context context) {
        Log.d(TAG, "deleteTable: starts");
        sqLiteDatabase.execSQL("DELETE FROM " + TABLE_NAME);
        Log.d(TAG, "deleteTable: ends");
    }

    public static void insertData(Context context, SignalData signalData) {
        Log.d(TAG, "insertData: starts");
        sqLiteDatabase.execSQL("INSERT INTO " + TABLE_NAME + " (signalData, longitude, latitude) VALUES (" + SignalData.convertToQuery(signalData) + ")");
        Log.d(TAG, "insertPerson: ends");
    }

    public static LinkedList<SignalData> selectSignalData(Context context, String query) {
        Log.d(TAG, "selectSignalData: start");
        LinkedList<SignalData> matchedPeople = new LinkedList<>();
        String[] values = new String[3];
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + query, null);
        Log.d(TAG, "selectSignalData: cursor.length -> " + cursor.getCount());
        try {
            cursor.moveToFirst();
            if (cursor.getCount() != 0) {
                do {
                    for (int i = 1; i < cursor.getColumnNames().length; i++) {
                        values[i - 1] = cursor.getString(i);
                    }
                    matchedPeople.add(new SignalData(values));
                    Log.d(TAG, "selectSignalData: matchedPeople.last -> " + matchedPeople.getLast());
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(context, "No Signal Data in Database", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        cursor.close();
        Log.d(TAG, "selectSignalData: end");
        return matchedPeople;
    }

}
