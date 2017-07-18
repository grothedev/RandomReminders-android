package grothedev.randomreminders;

/*
   Random Reminders android app. app to randomly notify a line from a text file at random times each day within a certain time range.
   Copyright (C) 2017  Thomas Grothe

      This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.
*/


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //a main concern with this app is the fact that it needs to be running constantly in background (i think).
    // figure out how to do this while using the least amount of batter

    final int FILE_READ_PERMISSION_REQUEST = 10;

    Button buttonFileSelect;
    EditText inputFilePath; //input the path assuming external storage to be roots
    TimePicker inputStartTime;
    TimePicker inputEndTime;
    EditText inputNumTimes; //# times you will get notified each day
    Switch switchActive;

    TextView tvActive;

    InputStream remindersFile;

    ArrayList<String> messages;

    SharedPreferences prefs;

    Intent bgServiceIntent; //intent for the background service which will randomly notify

    //public static Stack<Integer> timeIntervals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sets default notification settings the first time the app is run
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //restore preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        initUIElements();

        bgServiceIntent = new Intent(this, NotificationService.class);

        switchActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    //check for file read permissionsj
                    int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED){
                        requestFileReadPermission();
                    } else {
                        getPhrasesFromFile();
                    }

                    if (Integer.parseInt(inputNumTimes.getText().toString()) < 1){
                        toast("You didn't specify an adequate number of times to be reminded. I will assume 1 time each day.");
                        inputNumTimes.setText("1");
                    }

                    //make sure start time is before end time
                    if (inputStartTime.getCurrentHour() > inputEndTime.getCurrentHour()){
                        if (inputStartTime.getCurrentMinute() >= inputEndTime.getCurrentMinute()){
                            toast("Your start time is after the end time");
                            switchActive.setChecked(false);
                        } else {
                            activate();
                        }
                    } else {
                        activate();
                    }

                } else {
                    if (isServiceActive()){
                        Log.d("switch", "triggered to deactivate");
                        deactivate();
                    }
                }
            }
        });


        /*Button test = (Button) findViewById(R.id.testButton);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isServiceActive()){
                    toast("active");
                } else toast("inactive");
            }
        });*/

    }

    //this method is called after everything is set up, it starts the process of notifying at certain random times, and saves preferences
    private void activate(){

        /*Log.d("check input values", inputStartTime.getCurrentHour() + ": " + inputStartTime.getCurrentMinute()
                                    + "; " + inputEndTime.getCurrentHour() + ": " + inputEndTime.getCurrentMinute()
                                    + "; " + Integer.parseInt(inputNumTimes.getText().toString()));
        */

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("filePath", inputFilePath.getText().toString());
        editor.putInt("startTimeH", inputStartTime.getCurrentHour());
        editor.putInt("startTimeM", inputStartTime.getCurrentMinute());
        editor.putInt("endTimeH", inputEndTime.getCurrentHour());
        editor.putInt("endTimeM", inputEndTime.getCurrentMinute());
        editor.putInt("numTimes", Integer.parseInt(inputNumTimes.getText().toString()));

        //passing relevant data to the background process
        bgServiceIntent.putExtra("startTime", inputStartTime.getCurrentHour() * 60 * 60 * 1000 + inputStartTime.getCurrentMinute() * 60 * 1000);
        bgServiceIntent.putExtra("endTime", inputEndTime.getCurrentHour() * 60 * 60 * 1000 + inputEndTime.getCurrentMinute() * 60 * 1000);
        bgServiceIntent.putExtra("numTimes", Integer.parseInt(inputNumTimes.getText().toString()));
        bgServiceIntent.putExtra("messages", messages);
        bgServiceIntent.setAction("SETUP_BACKGROUND_SERVICE");

        Log.d("", "attempting to start service");
        startService(bgServiceIntent);
        Log.d("", "attempted to start service");
        editor.putBoolean("active", true);
        editor.commit();

        toast("Random Reminders are now active");
        tvActive.setText("Active");
    }

    private void deactivate(){
        stopService(bgServiceIntent);
        prefs.edit().putBoolean("active", false);
        prefs.edit().commit();
        tvActive.setText("Inactive");
    }


    private void requestFileReadPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                FILE_READ_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode){
            case FILE_READ_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getPhrasesFromFile();
                } else {
                    toast("i need to be able to read strings from a text file on your device, otherwise i am useless");
                }
        }
    }

    private void getPhrasesFromFile(){
        //make sure file exists
        File sdcard = Environment.getExternalStorageDirectory();
        File phrasesFile = new File(sdcard, inputFilePath.getText().toString());

        if (phrasesFile.exists()){
            messages = new ArrayList<String>();

            try{
                BufferedReader br = new BufferedReader(new FileReader(phrasesFile));
                String line;

                while ((line = br.readLine()) != null){
                    messages.add(line);
                    //Log.d("lines", messages.get(messages.size()-1));
                }
                br.close();
            } catch (IOException e){
                Log.e("ioexception", e.getMessage());
                Toast t = Toast.makeText(getApplicationContext(), "something went wrong while reading from the file", Toast.LENGTH_SHORT);
                t.show();
            }
        } else {
            Toast t = Toast.makeText(getApplicationContext(), "file does not exist; check your string", Toast.LENGTH_SHORT);
            t.show();
        }
    }

    private void initUIElements(){
        //button to select text file of list of phrases
        //currently this is actually just a string representing the filepath
        /*buttonFileSelect = (Button)findViewById(R.id.buttonFileSelect);
        buttonFileSelect.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //show dialog to input file
            }
        });*/

        //input string for path of file of phrases
        inputFilePath = (EditText) findViewById(R.id.editTextFilePath);

        if (prefs.contains("filePath")){
            inputFilePath.setText(prefs.getString("filePath", null));
        }


        //select start and end time of when reminders should appear each day
        inputStartTime = (TimePicker) findViewById(R.id.timePickerStart);
        inputEndTime = (TimePicker) findViewById(R.id.timePickerEnd);

        if (prefs.contains("startTimeH")){
            inputStartTime.setCurrentHour(prefs.getInt("startTimeH", 0));
        }
        if (prefs.contains("startTimeM")){
            inputStartTime.setCurrentMinute(prefs.getInt("startTimeM", 0));
        }
        if (prefs.contains("endTimeH")){
            inputEndTime.setCurrentHour(prefs.getInt("endTimeH", 0));
        }
        if (prefs.contains("endTimeM")){
            inputEndTime.setCurrentMinute(prefs.getInt("endTimeM", 0));
        }

        //select how many reminders to have each day
        inputNumTimes = (EditText) findViewById(R.id.editTextNumTimesDaily);

        if (prefs.contains("numTimes")){
            inputNumTimes.setText(Integer.toString(prefs.getInt("numTimes", 1)));
        }

        //select to have the app be active or inactive
        switchActive = (Switch) findViewById(R.id.switchActive);
        tvActive = (TextView) findViewById(R.id.textViewActive);


        if (isServiceActive()){
            switchActive.setChecked(true);
            tvActive.setText("Active");
        } else {
            switchActive.setChecked(false);
        }


    }

    //returns whether or not the background service of random notifications is running
    private boolean isServiceActive(){

        return NotificationService.isRunning;
    }

    private void toast(String s){
        Toast t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }

    //settings stuff here
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                if (getFragmentManager().getBackStackEntryCount() == 0){
                    Fragment settingsFrag = new SettingsFragment();

                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, settingsFrag)
                            .addToBackStack("settings_frag")
                            .commit();
                }

                return true;
            case R.id.home:
                if (getFragmentManager().getBackStackEntryCount() >= 1){
                    getFragmentManager().popBackStack();
                }
                return true;
            case R.id.about:
                //show dialog for info about the app
                toast("dialog for about");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
