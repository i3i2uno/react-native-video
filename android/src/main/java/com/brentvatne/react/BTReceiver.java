package com.brentvatne.react;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

/**
 * Created by bjr on 8/20/16.
 */

public class BTReceiver extends BroadcastReceiver {
    public static ReactApplicationContext context;
    public RCTEventEmitter mEventEmitter;
    public int id = -1;

    public BTReceiver() {
        super();
    }

    public BTReceiver(ReactApplicationContext rc) {
        super();
        context = rc;
    }

    @Override
    public void onReceive(Context ct, Intent intent) {
        try {
            final String action = intent.getAction();

            WritableMap params = Arguments.createMap();
            params.putString("event", action);

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case 0: //state off
                        params.putString("event", "STATE_CHANGE");
                        params.putString("state", "OFF");
                        sendEvent(params);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case 2:
                        params.putString("event", "STATE_CHANGE");
                        params.putString("state", "ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            } else if (action.equals("android.intent.action.HEADSET_PLUG")) {
                final int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0: //state off
                        params.putString("event", "STATE_CHANGE");
                        params.putString("state", "OFF");
                        sendEvent(params);
                        break;
                    case 1:
                        params.putString("event", "STATE_CHANGE");
                        params.putString("state", "ON");
                        break;
                }
            } else if (action.equals("PREVIOUS")) {
                sendEvent(params);
            } else if (action.equals("PLAYPAUSE")) {
                sendEvent(params);
            } else if (action.equals("NEXT")) {
                sendEvent(params);
            } else if (action.equals("GOTOMAIN")) {
                sendEvent(params);
            } else if (action.equals("android.media.AUDIO_BECOMING_NOISY")) {
                sendEvent(params);
            } else if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                sendEvent(params);
            } else {
                sendEvent(params);
            }
        } catch (Exception err) {
            Log.d("SSPOT", err.toString());
            err.printStackTrace();
            return;
        }
    }

    public void sendEvent(WritableMap params) {
        try {
            if (context != null && context.hasActiveCatalystInstance()) {
                if (mEventEmitter == null) {
                    mEventEmitter = context.getJSModule(RCTEventEmitter.class);
                }

                if (id == -1) {
                    id = ReactVideoView.ViewID;
                }

                mEventEmitter.receiveEvent(id, ReactVideoView.Events.EVENT_REMOTE_CHANGE.toString(), params);
            }
        } catch (Exception err) {
            Log.d("SSPOT", err.toString());
            err.printStackTrace();
            return;
        }
    }
}
