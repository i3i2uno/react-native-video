package com.brentvatne.react;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;

public class PlayerService extends Service {

    public static PlayerService instance;
    static NotificationManagerCompat mManager;
    static Notification.Builder mNote;
    static int noteId = 1326;
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
        super.onCreate();

        instance = this;

        mManager = NotificationManagerCompat.from(this);
        mReceiver = new BTReceiver(mReactContext);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter filterHeadset = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

        IntentFilter filterCommands = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        filterCommands.addAction("android.intent.action.MEDIA_BUTTON");
        filterCommands.setPriority(1000000000);

        Intent prevIntent = new Intent("PREVIOUS");
        previous = PendingIntent.getBroadcast(mReactContext, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playPauseIntent = new Intent("PLAYPAUSE");
        playPause = PendingIntent.getBroadcast(mReactContext, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent("NEXT");
        next = PendingIntent.getBroadcast(mReactContext, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        registerReceiver(mReceiver, filter);
        registerReceiver(mReceiver, filterHeadset);
        registerReceiver(mReceiver, filterCommands);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public void onDestroy() {
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
            instance.onDestroy();
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
    }

    public void start() {
        Notification note = mNote.build();
        mManager.notify(noteId, note);

        if (!mStarted) {
            Intent intent = new Intent(mReactContext, PlayerService.class);
            ContextCompat.startForegroundService(mReactContext, intent);
            startForeground(noteId, note);
            mStarted = true;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}