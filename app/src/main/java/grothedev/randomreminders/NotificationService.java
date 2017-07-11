package grothedev.randomreminders;

import android.app.IntentService;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Stack;

/**
 * Created by thomas on 7/11/17.
 */

public class NotificationService extends IntentService {

    private ArrayList<String> messages;
    private Stack<Integer> timeIntervals;
    private int startTime, endTime, numTimes;

    public NotificationService(){
        super("NotificationService");

    }

    @Override
    protected void onHandleIntent(Intent intent){

        messages = (ArrayList<String>) intent.getSerializableExtra("messages");
        startTime = (int) intent.getSerializableExtra("startTime");
        endTime = (int) intent.getSerializableExtra("endTime");
        numTimes = (int) intent.getSerializableExtra("numTimes");

        Random r = new Random();
        int i = r.nextInt(messages.size() - 1);
        //maybe setup some list that has the milliseconds set up a head of time along with each message

        //TODO setup RTC wakeup alarm here https://developer.android.com/training/scheduling/alarms.html
        timeIntervals = generateTimeIntervals();
        while (timeIntervals.peek() != null){ //CURRENT STATUS: peek() is returning null for some reason. i might have to rethink the way i deal with time intervals
            Log.d("a time interval", timeIntervals.pop().toString());
        }

        //this is how i will make a simple notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Remember")
                .setContentText(messages.get(i));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        synchronized (notificationManager) {
            notificationManager.notify();
            notificationManager.notify(001, mBuilder.build());
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
