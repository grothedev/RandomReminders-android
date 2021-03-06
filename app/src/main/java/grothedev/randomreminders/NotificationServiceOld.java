package grothedev.randomreminders;


/*
   Random Reminders android app. app to randomly notify a line from a text file at random times each day within a certain time range.
   Copyright (C) 2017  Thomas Grothe

      This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.
*/

import android.app.AlarmManager;
import android.app.IntentService;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Stack;

//import static grothedev.randomreminders.MainActivity.timeIntervals;

/**
 * Created by thomas on 7/11/17.
 */

public class NotificationServiceOld extends IntentService {

    //these are static cause i can't send stacks to intents (although i can send arraylists i think)
    static private ArrayList<String> messages;
    static private Stack<Integer> timeIntervals;

    SharedPreferences prefs;

    static private int startTime, endTime, numTimes;

    public static boolean isRunning;

    public NotificationServiceOld(){
        super("NotificationServiceOld");

    }

    @Override
    protected void onHandleIntent(Intent intent){

        isRunning = true;

        if (intent.getAction().equals("SETUP_BACKGROUND_SERVICE")){

            messages = (ArrayList<String>) intent.getSerializableExtra("messages");
            startTime = (int) intent.getSerializableExtra("startTime");
            endTime = (int) intent.getSerializableExtra("endTime");
            numTimes = (int) intent.getSerializableExtra("numTimes");


            //TODO setup RTC wakeup alarm here https://developer.android.com/training/scheduling/alarms.html
            timeIntervals = generateTimeIntervals();
            /*while (!timeIntervals.isEmpty()){
                Log.d("a time interval", timeIntervals.pop().toString());
            }*/


            //setting up alarm
            Log.d("now setting up alarm", "");
            Intent notificationIntent = new Intent(getApplicationContext(), NotificationServiceOld.class);
            notificationIntent.setAction("NOTIFY");
            AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent alarmIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeIntervals.pop(), alarmIntent);
            Log.d("alarm is now set up", "");



        } else if (intent.getAction().equals("NOTIFY")){
            Log.d("t", "t");
            if (!timeIntervals.isEmpty()){
                Random r = new Random();
                int i = r.nextInt(messages.size() - 1);

                //this is how i will make a simple notification
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Remember")
                        .setContentText(messages.get(i));

                if (Settings.notificationLED) mBuilder.setLights(80, 700, 1200);
                if (Settings.notificationVibrate) mBuilder.setVibrate(new long[]{10, 10, 10, 10, 20, 20, 30, 30, 50, 50, 80, 80, 130, 130, 210,210, 130, 130, 50, 50, 30, 30, 20, 20, 10, 10, 10, 10});

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                synchronized (notificationManager) {
                    notificationManager.notify();
                    notificationManager.notify(001, mBuilder.build());
                }

                setAlarm();
            } else {
               //this is the first notification for today. need to populate time intervals stack again
                timeIntervals = generateTimeIntervals();
                setAlarm();
            }

        }






    }


    //generates a stack of ints representing all the time intervals which will be used for this current time frame
    private Stack<Integer> generateTimeIntervals(){
        Stack<Integer> intervals = new Stack<>();
        Random rand = new Random();

        int currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60*60*1000 + Calendar.getInstance().get(Calendar.MINUTE) * 60 * 1000;
        int initialWait = 0; //how long to wait before firing the first notification

        int timeRange = endTime - startTime;

        if (startTime < currentTime) {
            initialWait = 0;
        } else {
            initialWait = startTime - currentTime;
        }

        int meanInterval = timeRange / numTimes;
        int deviation = rand.nextInt(meanInterval/2); //possible deviation of half of the interval range seems good (since this is the first interval, i'm making sure it won't be negative)

        /*
        Log.d("checking times", "in ms");
        Log.d("current time", Integer.toString(currentTime));
        Log.d("start time", Integer.toString(startTime));
        Log.d("end time", Integer.toString(endTime));
        Log.d("time range", Integer.toString(timeRange));
        Log.d("initial wait", Integer.toString(initialWait));
        */


        intervals.push(initialWait + deviation);
        for (int i = 1; i < numTimes; i++){
            deviation = rand.nextInt(meanInterval / 2);

            intervals.push(meanInterval + deviation);
            Log.d("interval " + i, "" + (meanInterval + deviation));
            //WTF what is going wrong here? the program stops here
        }

        Log.d("what going wrong here?", "");
        intervals.push(24 * 60 * 60 * 1000 - timeRange); //add 24 hours minus the time range to start again tomorrow
        Log.d("" + intervals.peek(), "");

        //NOTE this is currently not strictly within the specified range
        return intervals;
    }

    private void setAlarm(){
        Intent notificationIntent = new Intent(getApplicationContext(), NotificationServiceOld.class);
        notificationIntent.setAction("NOTIFY");
        AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeIntervals.pop(), alarmIntent);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Log.d("notification service", "onDestroy() called");
    }
}
