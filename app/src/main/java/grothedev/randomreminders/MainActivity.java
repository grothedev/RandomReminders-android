package grothedev.randomreminders;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

//CURRENT STATUS: dealing with times
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

    InputStream remindersFile;

    ArrayList<String> messages;

    @TargetApi(23) //this is for getting the time from the time pickers
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUIElements();
        //TODO check whether or not the thing is active already


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

                    //make sure start time is before end time
                    //TODO get time from timepickers
                    /*if (inputStartTime.getHour() > inputEndTime.getHour()){
                        if (inputStartTime.getMinute() >= inputEndTime.getMinute()){
                            Toast t = Toast.makeText(getApplicationContext(), "your start time is after the end time", Toast.LENGTH_SHORT);
                            t.show();
                            switchActive.setChecked(false);
                        } else {
                            doNotifications();
                        }
                    } else {
                        doNotifications();
                    }*/
                    doNotifications();

                    //do the random notifications
                } else {
                    //deactivate
                }
            }
        });
    }

    //this method is called after everything is set up, it starts the process of notifying at certain random times, which i will research next
    private void doNotifications(){
        NotificationService notificationService = new NotificationService();
        Intent notificationIntent = new Intent(this, NotificationService.class);
        notificationIntent.putExtra("messages", messages);
        startService(notificationIntent);
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
                    Toast t = Toast.makeText(getApplicationContext(), "i need to be able to read strings from a text file on your device, otherwise i am useless", Toast.LENGTH_LONG);
                    t.show();
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

        //select start and end time of when reminders should appear each day
        inputStartTime = (TimePicker) findViewById(R.id.timePickerStart);
        inputEndTime = (TimePicker) findViewById(R.id.timePickerEnd);

        //select how many reminders to have each day
        inputNumTimes = (EditText) findViewById(R.id.editTextNumTimesDaily);


        //select to have the app be active or inactive
        switchActive = (Switch) findViewById(R.id.switchActive);
    }
}
