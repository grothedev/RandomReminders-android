package grothedev.randomreminders;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by thomas on 7/14/17.
 */

public class NotificationService extends Service {

    public static boolean isRunning = false;

    @Override
    public void onCreate() {

        Log.d("notif service", "created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("notif service", "started");
        isRunning = true;

        return START_NOT_STICKY;
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
