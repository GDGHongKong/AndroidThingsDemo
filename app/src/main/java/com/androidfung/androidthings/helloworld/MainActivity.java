package com.androidfung.androidthings.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.pio.PeripheralManagerService;

import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private AlphanumericDisplay mSegment ;
    private Bmx280 mSensor;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRefMessage;
    private DatabaseReference mRefTemperature;
    private DatabaseReference mRefPressure;
    private Button mButtonA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> portList = manager.getGpioList();
        if (portList.isEmpty()) {
            Log.i(TAG, "No GPIO port available on this device.");
        } else {
            Log.i(TAG, "List of available ports: " + portList);
        }

        try {
            mSegment = RainbowHat.openDisplay();
            mSegment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            mSensor = RainbowHat.openSensor();
            mSensor.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            mSensor.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
        }catch (IOException e){
            e.printStackTrace();
        }

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRefMessage = mFirebaseDatabase.getReference("message");
        mRefTemperature = mFirebaseDatabase.getReference("temperature");
        mRefPressure = mFirebaseDatabase.getReference("pressure");

        // Read from the database
        mRefMessage.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + value);
                try {
                    mSegment.display(value);
                    mSegment.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        try {
            // Detect button press.
            mButtonA = RainbowHat.openButton(RainbowHat.BUTTON_A);
            mButtonA.setOnButtonEventListener((button, pressed) -> {
                Log.d(TAG, "button A pressed:" + pressed);
                updateRecord();
            });



        }catch (IOException e){
            e.printStackTrace();
        }

        Log.d(TAG, "Hello Android Things!");
    }

    public void onPause(){
        super.onPause();
        try {
            // Close the device when done.
            mSegment.setEnabled(false);
            mSegment.close();
            mSensor.close();
            mButtonA.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void updateRecord(){
        // Display the temperature on the segment display.
        try {
            mRefTemperature.setValue(mSensor.readTemperature());
            mRefPressure.setValue(mSensor.readPressure());
        }catch (IOException e){
            e.printStackTrace();
        }
    }


}
