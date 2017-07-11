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
import java.util.Random;

/**
 * Created by thomas on 7/11/17.
 */

public class NotificationService extends IntentService {

    ArrayList<String> messages;



    public NotificationService(){
        super("NotificationService");

    }

    @Override
    protected void onHandleIntent(Intent intent){

        messages = (ArrayList<String>) intent.getSerializableExtra("messages");
        Random r = new Random();
        int i = r.nextInt(messages.size() - 1);
        //maybe setup some list that has the milliseconds set up a head of time along with each message

        //TODO setup RTC wakeup alarm here https://developer.android.com/training/scheduling/alarms.html

        //TODO setup notification for the alarm to trigger
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


}
