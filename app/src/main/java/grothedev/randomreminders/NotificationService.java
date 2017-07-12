package grothedev.randomreminders;

import android.app.AlarmManager;
import android.app.IntentService;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Stack;

//import static grothedev.randomreminders.MainActivity.timeIntervals;

/**
 * Created by thomas on 7/11/17.
 */

public class NotificationService extends IntentService {

    //these are static cause i can't send stacks to intents (although i can send arraylists i think)
    static private ArrayList<String> messages;
    static private Stack<Integer> timeIntervals;

    private int startTime, endTime, numTimes;

    public NotificationService(){
        super("NotificationService");

    }

    @Override
    protected void onHandleIntent(Intent intent){

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
            Intent notificationIntent = new Intent(getApplicationContext(), NotificationService.class);
            notificationIntent.setAction("NOTIFY");
            AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent alarmIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);

            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeIntervals.pop(), alarmIntent);

        } else if (intent.getAction().equals("NOTIFY")){
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

                Intent notificationIntent = new Intent(getApplicationContext(), NotificationService.class);
                notificationIntent.setAction("NOTIFY");
                AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                PendingIntent alarmIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);

                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeIntervals.pop(), alarmIntent);
            } else {
               stopSelf();
            }

        }






    }


    //generates a stack of ints representing all the time intervals which will be used for this current time frame
    private Stack<Integer> generateTimeIntervals(){
        Stack<Integer> intervals = new Stack<>();
        Random rand = new Random();

        int currentTime = Calendar.getInstance().get(Calendar.MILLISECOND);
        int initialWait = 0; //how long to wait before firing the first notification

        if (startTime > currentTime) {
            startTime = currentTime;
        } else {
            initialWait = startTime - currentTime;
        }

        int timeRange = endTime - startTime;

        int meanInterval = timeRange / numTimes;
        int deviation = rand.nextInt(meanInterval) - meanInterval/2; //possible deviation of half of the interval range seems good
        intervals.push(initialWait + deviation);

        for (int i = 1; i < numTimes; i++){
            deviation = rand.nextInt(meanInterval / 2);
            intervals.push(meanInterval + deviation);
        }

        //NOTE this is currently not strictly within the specified range
        return intervals;
    }

}
