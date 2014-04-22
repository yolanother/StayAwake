
package com.doubtech.stayawake;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class StayAwakeService extends Service {
    protected static final String ACTION_STAY_AWAKE = "com.doubtech.stayawake.STAY_AWAKE";
    protected static final String ACTION_ALLOW_SLEEP = "com.doubtech.stayawake.SLEEP";
    protected static final String ACTION_QUIT = "com.doubtech.stayawake.QUIT";
    private static final int NOTIFICATION_ID = 1293;
    private static final int REQUEST_QUIT = 0;
    private static final int REQUEST_SLEEP = 0;
    private static final int REQUEST_AWAKE = 0;

    public Binder mBinder = new Binder() {
        
    };
    private WakeLock mWakeLock;
    private BroadcastReceiver mBroadcastReceiver;
    private Builder mNotificationBuilder;
    private Notification mNotification;
    
    
    public StayAwakeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        mWakeLock.acquire();
        return mBinder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AARON", "Starting service...");
        
        if(null == mWakeLock) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getPackageName() + ".StayAwake");
        }
        
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(ACTION_STAY_AWAKE.equals(intent.getAction())) {
                    if(!mWakeLock.isHeld()) {
                        mWakeLock.acquire();
                    }
                    updateNotification(true);
                } else if(ACTION_ALLOW_SLEEP.equals(intent.getAction())) {
                    if(mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    updateNotification(true);
                } else if(ACTION_QUIT.equals(intent.getAction())) {
                    shutdown();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ALLOW_SLEEP);
        filter.addAction(ACTION_STAY_AWAKE);
        filter.addAction(ACTION_QUIT);
        registerReceiver(mBroadcastReceiver, filter);
        updateNotification(false);
        startForeground(NOTIFICATION_ID, mNotification);
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AARON", "Stopping service...");
        unregisterReceiver(mBroadcastReceiver);
    }

    private void shutdown() {
        stopForeground(true);
        stopSelf();
    }

    private void updateNotification(boolean send) {
        Intent quitIntent = new Intent(ACTION_QUIT);
        PendingIntent pendingQuitIntent = PendingIntent.getBroadcast(this, REQUEST_QUIT, quitIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent awakeIntent = new Intent(ACTION_STAY_AWAKE);
        PendingIntent pendingAwakeIntent = PendingIntent.getBroadcast(this, REQUEST_AWAKE, awakeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent sleepIntent = new Intent(ACTION_ALLOW_SLEEP);
        PendingIntent pendingSleepIntent = PendingIntent.getBroadcast(this, REQUEST_SLEEP, sleepIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mNotificationBuilder = new Notification.Builder(this)
            .setOngoing(true)
            .addAction(R.drawable.ic_stat_quit, "Quit", pendingQuitIntent)
            .setContentText(mWakeLock.isHeld() ? "Currently keeping device awake." : "Currently allowing device to sleep")
            .setContentText("Stay Awake Status");

        if(mWakeLock.isHeld()) {
            mNotificationBuilder.addAction(R.drawable.ic_stat_quit, "Sleep", pendingSleepIntent);
        } else {
            mNotificationBuilder.addAction(R.drawable.ic_stat_quit, "Stay Awake", pendingAwakeIntent);
        }

        mNotificationBuilder.setContentText(mWakeLock.isHeld() ? 
                "Currently keeping device awake." : "Device is allowed to sleep");
        mNotification = new Notification(R.drawable.ic_launcher, "Stay Awake Status", System.currentTimeMillis());//mNotificationBuilder.build();
        mNotification.flags |= Notification.FLAG_NO_CLEAR;

        if(mWakeLock.isHeld()) {
            mNotification.setLatestEventInfo(this, "Stay Awake Status",
                    "Currently keeping device awake.", pendingSleepIntent);
        } else {
            mNotification.setLatestEventInfo(this, "Stay Awake Status",
                    "Currently allowing device to sleep", pendingAwakeIntent);            
        }

        if(send) {
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }
}
