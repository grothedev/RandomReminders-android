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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

/**
 * Created by thomas on 7/14/17.
 */

public class NotificationService extends Service {

    public static boolean isRunning = false;

    //these are static cause i can't send stacks to intents (although i can send arraylists i think)
    static private ArrayList<String> messages;
    static private PriorityQueue<Integer> timeIntervals;

    SharedPreferences prefs;

    static private int startTime, endTime, numTimes;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("notif service", "created");
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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
            Log.d("", "now setting up alarm");
            /*Intent notificationIntent = new Intent(this, NotificationService.class);
            notificationIntent.setAction("NOTIFY");
            AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent alarmIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
            long time = SystemClock.elapsedRealtime() + timeIntervals.remove();
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, alarmIntent);
            */
            setAlarm();
            Log.d("current time", "" + SystemClock.elapsedRealtime());



        } else if (intent.getAction().equals("NOTIFY")){
            Log.d("t", "t");
            if (!timeIntervals.isEmpty()){
                Random r = new Random();
                int i = r.nextInt(messages.size());

                //this is how i will make a simple notification
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Remember")
                        .setContentText(messages.get(i));


                if (prefs.getBoolean("pref_led", true)) mBuilder.setLights(80, 700, 1200);
                if (prefs.getBoolean("pref_vibrate", true)) {
                    //mBuilder.setVibrate(new long[]{10, 10, 10, 10, 20, 20, 30, 30, 50, 50, 80, 80, 130, 130, 210, 210, 130, 130, 50, 50, 30, 30, 20, 20, 10, 10, 10, 10});
                    mBuilder.setVibrate(new long[]{200, 200, 200, 200, 300});
                }
                if (prefs.getBoolean("pref_sound", false)){
                    Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    mBuilder.setSound(notificationSound);
                }
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                synchronized (notificationManager) {
                    notificationManager.notify();
                    notificationManager.notify(001, mBuilder.build());
                }

                setAlarm();
            } else {
                //this is the first notification for today. need to populate time intervals queue again
                timeIntervals = generateTimeIntervals();
                setAlarm();
            }

        }

        return START_NOT_STICKY;
    }

    private PriorityQueue<Integer> generateTimeIntervals(){
        PriorityQueue<Integer> intervals = new PriorityQueue<>();
        Random rand = new Random();

        int currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60*60*1000 + Calendar.getInstance().get(Calendar.MINUTE) * 60 * 1000;
        int initialWait = 0; //how long to wait before firing the first notification

        int timeRange = endTime - startTime;

        if (startTime < currentTime) {
            timeRange = endTime - currentTime;
        } else {
            initialWait = startTime - currentTime;
        }

        int meanInterval = timeRange / numTimes;
        int deviation = rand.nextInt(meanInterval/2); //possible deviation of half of the interval range seems good (since this is the first interval, i'm making sure it won't be negative)

        intervals.add(deviation);

        /*
        Log.d("checking times", "in ms");
        Log.d("current time", Integer.toString(currentTime));
        Log.d("start time", Integer.toString(startTime));
        Log.d("end time", Integer.toString(endTime));
        Log.d("time range", Integer.toString(timeRange));
        Log.d("initial wait", Integer.toString(initialWait));
        */



        for (int i = 1; i < numTimes; i++){
            deviation = rand.nextInt(meanInterval / 2) - meanInterval/2;

            intervals.add(meanInterval + deviation);
            Log.d("interval " + i, "" + (meanInterval + deviation));
        }
        intervals.add(24 * 60 * 60 * 1000 - timeRange); //add 24 hours minus the time range to start again tomorrow

        //NOTE this is currently not strictly within the specified range
        return intervals;
    }

    private void setAlarm(){
        Intent notificationIntent = new Intent(getApplicationContext(), NotificationService.class);
        notificationIntent.setAction("NOTIFY");
        AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeIntervals.remove(), alarmIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("notif service", "destroyed");
        isRunning = false;
    }
}
