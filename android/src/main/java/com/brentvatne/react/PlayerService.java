package com.brentvatne.react;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;

public class PlayerService extends Service {

    public static PlayerService instance;
    static NotificationManager mManager;
    static NotificationCompat.Builder mNote;
    static int noteId = 1326;
    public static String CHANNEL_ID = "Sunspot_01";
    public static BTReceiver mReceiver;
    public static MediaSession mSession;
    public static AudioManager aManager;

    public static PendingIntent previous;
    public static PendingIntent playPause;
    public static PendingIntent next;

    public static boolean mStarted;

    public static ReactApplicationContext mReactContext;

    @Override
    public void onCreate() {
        try {
            super.onCreate();

            instance = this;

            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mReceiver = new BTReceiver(mReactContext);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            filter.addAction(Intent.ACTION_MEDIA_BUTTON);
            filter.addAction("android.intent.action.MEDIA_BUTTON");
            filter.setPriority(1000000000);

            Intent prevIntent = new Intent("PREVIOUS");
            previous = PendingIntent.getBroadcast(mReactContext, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            filter.addAction("PREVIOUS");

            Intent playPauseIntent = new Intent("PLAYPAUSE");
            playPause = PendingIntent.getBroadcast(mReactContext, 1, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            filter.addAction("PLAYPAUSE");

            Intent nextIntent = new Intent("NEXT");
            next = PendingIntent.getBroadcast(mReactContext, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            filter.addAction("NEXT");

            registerReceiver(mReceiver, filter);
        } catch (Exception err) {
            Log.d("SSPOT", err.toString());
            err.printStackTrace();
            return;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        try {
            stopSelf();
        } catch (Exception err) {
            Log.d("SSPOT", err.toString());
            err.printStackTrace();
            return;
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (mNote != null) {
                mNote.setOngoing(false);
                mNote = null;
            }
            if (mManager != null) {
                mManager.cancel(noteId);
            }
            if (mReceiver != null && mReactContext != null) {
                unregisterReceiver(mReceiver);
                mReactContext = null;
            }
            if (instance != null) {
                instance = null;
            }
            if (mSession != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mSession.release();
                }
                mSession = null;
            }

            aManager = null;

            super.onDestroy();
        } catch (Exception err) {
            Log.d("SSPOT", err.toString());
            err.printStackTrace();
            return;
        }
    }

    public void start() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                CharSequence name = "Sunspot";
                String Description = "Sunspot Channel";
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mChannel.setDescription(Description);
                mChannel.setShowBadge(false);
                mManager.createNotificationChannel(mChannel);
            }

            Notification note = mNote.build();
            mManager.notify(noteId, note);

            if (!mStarted) {
                Intent intent = new Intent(mReactContext, PlayerService.class);
                ContextCompat.startForegroundService(mReactContext, intent);
                startForeground(noteId, note);
                mStarted = true;
            }
        } catch (Exception err) {
            Log.d("SSPOT", err.toString());
            err.printStackTrace();
            return;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}