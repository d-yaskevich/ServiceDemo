package com.myfirst.servicedemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Random;

public class MyService extends Service {

    private static final String TAG = MyService.class.getSimpleName();

    private static final String CHANNEL_ID = "myservice.channel.id";
    private static final long TIME_DELAY_MILLIS = 5000;

    public static final String NUMBER_KEY = "NUMBER_KEY";
    public static final int START_KEY = 1002;
    public static final int STOP_KEY = 1003;

    private Handler handler;
    private Runnable runnable;

    private PendingIntent pendingIntent;
    private NotificationManager notificationManager;

    public int maxNumber;

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        handler = new Handler();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        maxNumber = intent.getIntExtra(MainActivity.MAX_NUMBER_KEY, 100);
        Log.i(TAG, "onStartCommand() - max number = " + maxNumber + ";");

        pendingIntent = intent.getParcelableExtra(MainActivity.PENDING_INTENT_KEY);
        try {
            if (pendingIntent != null) {
                pendingIntent.send(START_KEY);
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        runnable = this::showRandomNumber; // == new Runnable() { ... }
        handler.postDelayed(runnable, TIME_DELAY_MILLIS);

        return START_NOT_STICKY;
    }

    public void showRandomNumber() {
        Random rand = new Random();
        int number = rand.nextInt(maxNumber);
        Log.i(TAG, "showRandomNumber() - number = " + number + ";");

        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(NUMBER_KEY, number);
        sendBroadcast(intent);

        sendNotification(number);

        handler.postDelayed(runnable, TIME_DELAY_MILLIS);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(int number) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(NUMBER_KEY, number);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_ac_unit)
                        .setContentTitle("My Service title")
                        .setContentText("Random number - " + number)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pIntent)
                        .setAutoCancel(true)
                        .build();

        notificationManager.notify(1, notification);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        handler.removeCallbacks(runnable);
        try {
            if (pendingIntent != null) {
                pendingIntent.send(STOP_KEY);
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return new MyBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onRebind()");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");
        return true;
    }

    class MyBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }
}