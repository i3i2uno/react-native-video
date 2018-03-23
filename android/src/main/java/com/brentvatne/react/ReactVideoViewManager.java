package com.brentvatne.react;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.KeyEvent;

import com.brentvatne.react.ReactVideoView.Events;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.yqritc.scalablevideoview.ScalableType;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReactVideoViewManager extends SimpleViewManager<ReactVideoView> {

    public static final String REACT_CLASS = "RCTVideo";

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_URI = "uri";
    public static final String PROP_SRC_TYPE = "type";
    public static final String PROP_SRC_IS_NETWORK = "isNetwork";
    public static final String PROP_SRC_MAINVER = "mainVer";
    public static final String PROP_SRC_PATCHVER = "patchVer";
    public static final String PROP_SRC_IS_ASSET = "isAsset";
    public static final String PROP_RESIZE_MODE = "resizeMode";
    public static final String PROP_REPEAT = "repeat";
    public static final String PROP_PAUSED = "paused";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_VOLUME = "volume";
    public static final String PROP_PROGRESS_UPDATE_INTERVAL = "progressUpdateInterval";
    public static final String PROP_SEEK = "seek";
    public static final String PROP_RATE = "rate";
    public static final String PROP_PLAY_IN_BACKGROUND = "playInBackground";
    public static final String PROP_CONTROLS = "controls";

    //CUSTOM
    public static final String PROP_METADATA = "metadata";
    public static final String PROP_PRELOAD = "preload";
    public static PlayerService mPlayerService;
    public static RCTEventEmitter mEventEmitter;
    public Boolean mAudioCall = false;
    public float prevVol;

    public ReactVideoViewManager(ReactApplicationContext reactContext) {
        super();

        PlayerService.mReactContext = reactContext;
        mPlayerService = new PlayerService();

        Intent i = new Intent(PlayerService.mReactContext, PlayerService.class);
        PlayerService.mReactContext.startService(i);

        PlayerService.aManager = (AudioManager) PlayerService.mReactContext
                .getSystemService(PlayerService.mReactContext.AUDIO_SERVICE);
        PlayerService.aManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                WritableMap params = Arguments.createMap();
                if (focusChange == -2) { //phone
                    //LOSS -> PAUSE
                    mAudioCall = true;
                    params.putBoolean("focus", false);
                    params.putString("event", "PLAYPAUSE");
                    PlayerService.mReceiver.sendEvent(params);
                } else if (focusChange == 1 && mAudioCall == true) {
                    //GAIN -> PLAY
                    mAudioCall = false;
                    params.putBoolean("focus", true);
                    params.putString("event", "PLAYPAUSE");
                    PlayerService.mReceiver.sendEvent(params);
                } else if (focusChange == 1) {
                    params.putString("event", "NOT_NOISY");
                    PlayerService.mReceiver.sendEvent(params);
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) { //other app playing volume
                    params.putString("event", "IS_NOISY");
                    PlayerService.mReceiver.sendEvent(params);
                }
            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }
    //CUSTOM END

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactVideoView createViewInstance(ThemedReactContext themedReactContext) {
        return new ReactVideoView(themedReactContext);
    }

    @Override
    public void onDropViewInstance(ReactVideoView view) {
        super.onDropViewInstance(view);
        view.cleanupMediaPlayerResources();
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @Override
    @Nullable
    public Map getExportedViewConstants() {
        return MapBuilder.of("ScaleNone", Integer.toString(ScalableType.LEFT_TOP.ordinal()), "ScaleToFill",
                Integer.toString(ScalableType.FIT_XY.ordinal()), "ScaleAspectFit",
                Integer.toString(ScalableType.FIT_CENTER.ordinal()), "ScaleAspectFill",
                Integer.toString(ScalableType.CENTER_CROP.ordinal()));
    }

    @ReactProp(name = PROP_SRC)
    public void setSrc(final ReactVideoView videoView, @Nullable ReadableMap src) {
        int mainVer = src.getInt(PROP_SRC_MAINVER);
        int patchVer = src.getInt(PROP_SRC_PATCHVER);
        if (mainVer < 0) {
            mainVer = 0;
        }
        if (patchVer < 0) {
            patchVer = 0;
        }
        if (mainVer > 0) {
            videoView.setSrc(src.getString(PROP_SRC_URI), src.getString(PROP_SRC_TYPE),
                    src.getBoolean(PROP_SRC_IS_NETWORK), src.getBoolean(PROP_SRC_IS_ASSET), mainVer, patchVer);
        } else {
            videoView.setSrc(src.getString(PROP_SRC_URI), src.getString(PROP_SRC_TYPE),
                    src.getBoolean(PROP_SRC_IS_NETWORK), src.getBoolean(PROP_SRC_IS_ASSET));
        }
    }

    @ReactProp(name = PROP_RESIZE_MODE)
    public void setResizeMode(final ReactVideoView videoView, final String resizeModeOrdinalString) {
        videoView.setResizeModeModifier(ScalableType.values()[Integer.parseInt(resizeModeOrdinalString)]);
    }

    @ReactProp(name = PROP_REPEAT, defaultBoolean = false)
    public void setRepeat(final ReactVideoView videoView, final boolean repeat) {
        videoView.setRepeatModifier(repeat);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final ReactVideoView videoView, final boolean paused) {
        videoView.setPausedModifier(paused);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(final ReactVideoView videoView, final boolean muted) {
        videoView.setMutedModifier(muted);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(final ReactVideoView videoView, final float volume) {
        videoView.setVolumeModifier(volume);
    }

    @ReactProp(name = PROP_PROGRESS_UPDATE_INTERVAL, defaultFloat = 250.0f)
    public void setProgressUpdateInterval(final ReactVideoView videoView, final float progressUpdateInterval) {
        videoView.setProgressUpdateInterval(progressUpdateInterval);
    }

    @ReactProp(name = PROP_SEEK)
    public void setSeek(final ReactVideoView videoView, final float seek) {
        videoView.seekTo(Math.round(seek * 1000.0f));
    }

    @ReactProp(name = PROP_RATE)
    public void setRate(final ReactVideoView videoView, final float rate) {
        videoView.setRateModifier(rate);
    }

    @ReactProp(name = PROP_PLAY_IN_BACKGROUND, defaultBoolean = false)
    public void setPlayInBackground(final ReactVideoView videoView, final boolean playInBackground) {
        videoView.setPlayInBackground(playInBackground);
    }

    @ReactProp(name = PROP_CONTROLS, defaultBoolean = false)
    public void setControls(final ReactVideoView videoView, final boolean controls) {
        videoView.setControls(controls);
    }

    //CUSTOM
    private class DownloadFilesTask extends AsyncTask<String, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        protected void onPostExecute(Bitmap result) {
            mImage = result;
            PlayerService.mNote.setLargeIcon(mImage);
            PlayerService.mNote.setSmallIcon(R.drawable.icon);

            MediaMetadata.Builder mdB = new MediaMetadata.Builder();
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, cName);
            mdB.putString(MediaMetadata.METADATA_KEY_TITLE, cName);
            mdB.putString(MediaMetadata.METADATA_KEY_ARTIST, cArtist);
            mdB.putString(MediaMetadata.METADATA_KEY_ALBUM, cAlbum);
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, cArtist);
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, cName);
            mdB.putString(MediaMetadata.METADATA_KEY_ART_URI, cImg);
            mdB.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, cImg);
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, cImg);
            mdB.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mImage);
            mdB.putBitmap(MediaMetadata.METADATA_KEY_ART, mImage);
            mdB.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mImage);

            PlayerService.mSession.setMetadata(mdB.build());

            PlayerService.instance.start();
        }
    }

    public static PendingIntent mContentIntent;
    public static Notification.MediaStyle mStyle;
    public static Bitmap mImage;
    public static List<Notification.Action> mActions;

    public String cName;
    public String cArtist;
    public String cAlbum;
    public String cImg;
    public double cDuration;
    public Boolean cState;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @ReactProp(name = PROP_METADATA)
    public void setMetadata(final ReactVideoView videoView, @Nullable ReadableMap metadata) {
        String name = metadata.getString("name");
        String artist = metadata.getString("artist");
        String album = metadata.getString("album");
        String img = metadata.getString("arturl");
        double duration = metadata.getDouble("dur");
        Boolean state = metadata.getBoolean("isPlaying");

        if (!name.equals(cName) || !artist.equals(cArtist) || !img.equals(cImg) || !state.equals(cState)) {

            cName = name;
            cArtist = artist;
            cAlbum = album != null ? album : "none";
            cImg = img;
            cState = state;
            cDuration = duration;

            new DownloadFilesTask().execute(img);

            PlayerService.mNote = new Notification.Builder(PlayerService.mReactContext);

            MediaMetadata.Builder mdB = new MediaMetadata.Builder();
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, cName);
            mdB.putString(MediaMetadata.METADATA_KEY_TITLE, cName);
            mdB.putString(MediaMetadata.METADATA_KEY_ARTIST, cArtist);
            mdB.putString(MediaMetadata.METADATA_KEY_ALBUM, cAlbum);
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, cArtist);
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, cName);
            mdB.putString(MediaMetadata.METADATA_KEY_ART_URI, cImg);
            mdB.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, cImg);
            mdB.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, cImg);
            mdB.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mImage);
            mdB.putBitmap(MediaMetadata.METADATA_KEY_ART, mImage);
            mdB.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mImage);

            if (PlayerService.mSession == null) {
                PlayerService.mSession = new MediaSession(PlayerService.mReactContext, "Sunspot");

                PlayerService.mSession.setFlags(
                        MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

                PlayerService.mSession.setCallback(new MediaSession.Callback() {

                    @Override
                    public boolean onMediaButtonEvent(final Intent mediaButtonIntent) {
                        String intentAction = mediaButtonIntent.getAction();

                        WritableMap params = Arguments.createMap();

                        KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        final int code = event.getKeyCode();
                        final int action = event.getAction();

                        if (action == 1) { //full press
                            switch (code) {
                            case KeyEvent.KEYCODE_HEADSETHOOK:
                                params.putString("event", "PLAYPAUSE");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                params.putString("event", "NEXT");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                params.putString("event", "PLAYPAUSE");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PLAY:
                                params.putString("event", "PLAYPAUSE");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                params.putString("event", "PLAYPAUSE");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                params.putString("event", "PREVIOUS");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_STOP:
                                params.putString("event", "PLAYPAUSE");
                                PlayerService.mReceiver.sendEvent(params);
                                break;
                            }
                        }

                        return true;
                    }
                });

                PlayerService.mSession.setActive(true);
            }

            mActions = new ArrayList<>();
            mActions.add(new Notification.Action(R.drawable.ic_fast_rewind_black_24dp, "Previous Song",
                    PlayerService.previous));
            mActions.add(new Notification.Action(
                    state ? R.drawable.ic_pause_black_24dp : R.drawable.ic_play_arrow_black_24dp,
                    state ? "Pause" : "Play", PlayerService.playPause));
            mActions.add(
                    new Notification.Action(R.drawable.ic_fast_forward_black_24dp, "Next Song", PlayerService.next));

            if (mContentIntent == null || mStyle == null) {
                mContentIntent = PendingIntent.getActivity(PlayerService.mReactContext, 0,
                        new Intent(PlayerService.mReactContext,
                                PlayerService.mReactContext.getCurrentActivity().getClass()),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                int actionInNote = mActions != null && mActions.size() > 2 ? 1 : 0;
                mStyle = new Notification.MediaStyle().setShowActionsInCompactView(actionInNote)
                        .setMediaSession(mPlayerService.mSession.getSessionToken());
            }

            PlaybackState pbState = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_STOP
                            | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT
                            | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                    .setState(state ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, 1.0f).build();

            PlayerService.mSession.setPlaybackState(pbState);

            PlayerService.mNote.setStyle(mStyle).setVisibility(Notification.VISIBILITY_PUBLIC).setUsesChronometer(true)
                    .setContentTitle(name).setSmallIcon(R.drawable.icon).setLargeIcon(mImage).setContentText(artist)
                    .setContentIntent(mContentIntent).setWhen(0).setPriority(Notification.PRIORITY_MAX)
                    .setShowWhen(false).setOngoing(false);

            PlayerService.mSession.setMetadata(mdB.build());

            for (Notification.Action act : mActions) {
                PlayerService.mNote.addAction(act);
            }

            PlayerService.instance.start();
        }
    }

    @ReactProp(name = PROP_PRELOAD)
    public void setPreload(final ReactVideoView videoView, final String preurl) throws IOException {
        videoView.setPreload(preurl);
    }

    @ReactMethod
    public void setTimeout(final int delay, final Callback cb) {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cb.invoke();
            }
        }.start();
    }

    @ReactMethod
    public void playLocal(final String file, final Callback cb) {
        //play local file
    }

    //CUSTOM END
}