/*
 * Copyright 2016 Jide Technology Ltd.
 * Copyright 2017 Android-x86 Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.android_x86.analytics;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import org.android_x86.analytics.AnalyticsHelper;
import org.android_x86.analytics.GeneralLogs;
import org.android_x86.analytics.HardwareCollector;
import org.android_x86.analytics.ImmortalIntentService;

import java.util.HashMap;

public class AnalyticsService extends ImmortalIntentService {
    private static final String TAG = "AnalyticsService";
    private static final boolean LOG = false;

    public static final String sSharedPreferencesKey = "org.android_x86.analytics.prefs";

    private static final int MS_IN_SECOND = 1000;
    // ga event
    private static final String EVENT_CATEGORY_POWER = "power";

    private static final String EVENT_BOOT_COMPLETED = "boot_completed";
    private static final String EVENT_SHUTDOWN = "shutdown";
    private static final String EVENT_SCREEN_ON = "screen_on";
    private static final String EVENT_SCREEN_OFF = "screen_off";
    // SharedPreferences_KEY
    private static final String SHARED_PREFS_KEY_SCREEN_CHANGE_TIME = "screen_change_time";
    private static final String SHARED_PREFS_KEY_LATEST_SEND_TIME = "latest_send_time";
    // System property for usage_statistics
    private static final String PROPERTY_USAGE_STATISTICS = "persist.sys.usage_statistics";

    private boolean mEnable;
    private SharedPreferences mSharedPrefs;
    private BroadcastReceiver mReceiver;

    private final HashMap<String, EventHandler> mStaticEventHandlers =
            new HashMap<String, EventHandler>();

    private LogHelper mLogHelper;

    public AnalyticsService() {
        super("AnalyticsService");
        initEventHandlers();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LOG) {
            Log.d(TAG, "AnalyticsService onCreate");
        }
        mEnable = SystemProperties.getBoolean(PROPERTY_USAGE_STATISTICS, true);

        mSharedPrefs = getSharedPreferences(sSharedPreferencesKey,
                Context.MODE_PRIVATE);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (LOG) {
                    Log.d(TAG, "Receive Intent: " + Util.toString(intent));
                }
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    AnalyticsHelper.screenOff(getBaseContext());
                    PowerStats.onScreenOff(context);
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    AnalyticsHelper.screenOn(getBaseContext());
                    PowerStats.onScreenOn(context);
                } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                    AnalyticsHelper.onShutdown(getBaseContext());
                    PowerStats.onScreenOff(context);
                } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    PowerStats.onPowerConnected(context);
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    PowerStats.onPowerDisconnected(context);
                }
            }
        };

        // Register for Intent broadcasts for...
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(BootCompletedReceiver.ACTION_SEND_LOGS);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        getBaseContext().registerReceiver(mReceiver, filter);

        mLogHelper = new LogHelper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (!mEnable){
            if (LOG) {
                Log.d(TAG, "USAGE STATISTICS not enable");
            }
            return;
        }
        EventHandler eventHandler = mStaticEventHandlers.get(action);
        if (eventHandler != null){
            eventHandler.onEvent(intent);
        } else if (!Intent.ACTION_BOOT_COMPLETED.equals(action)){
            Log.w(TAG, "unknow action :" + action);
        } else {
            // save boot completed time
            saveScreenChangeTime(getCurrentTimeInSeconds());
            onBootCompleted(intent);
            PowerStats.onScreenOn(getBaseContext());
        }
        if (LOG) {
            Log.d(TAG, "Handle Intent: " + Util.toString(intent));
        }
    }

    @Override
    public void onDestroy() {
        if (LOG){
            Log.d(TAG, "onDestroy");
        }
        super.onDestroy();
    }

    private void onHitScreen(Intent data) {
        String componentName;
        try {
            componentName = data.getStringExtra(AnalyticsHelper.EXTRA_COMPONENT_NAME);
        } catch (BadParcelableException e) {
            Log.w(TAG, "ignore BadParcelableException", e);
            return;
        }
        if (componentName == null) {
            Log.w(TAG, "onHitScreen, cannot get data");
            return;
        }
        if (LOG) {
            Log.v(TAG, "hitScreen:" + componentName);
        }
        ComponentName component = ComponentName.unflattenFromString(componentName);
        if (component == null) {
            Log.e(TAG, "onHitScreen, invalid ComponentName: " + componentName);
            return;
        }

        mLogHelper.newAppViewBuilder()
            .setActivityDimensions(component)
            .send();
    }

    private static long getCurrentTimeInSeconds() {
        return System.currentTimeMillis() / MS_IN_SECOND;
    }

    private void saveScreenChangeTime(long nowSeconds) {
        mSharedPrefs.edit()
                .putLong(SHARED_PREFS_KEY_SCREEN_CHANGE_TIME, nowSeconds)
                .commit();
    }

    private void removeScreenChangeTime() {
        mSharedPrefs.edit()
                .remove(SHARED_PREFS_KEY_SCREEN_CHANGE_TIME)
                .commit();
    }

    private Long getDurationAndSaveScreenChangeTime() {
        long nowSeconds = getCurrentTimeInSeconds();
        long latestChangeTime = mSharedPrefs.getLong(SHARED_PREFS_KEY_SCREEN_CHANGE_TIME, -1);

        saveScreenChangeTime(nowSeconds);
        if (latestChangeTime == -1) {
            return null;
        }
        return nowSeconds - latestChangeTime;
    }

    private void onBootCompleted(Intent data) {
        long bootTime = SystemClock.elapsedRealtime() / MS_IN_SECOND;
        mLogHelper.newEventBuilder(EVENT_CATEGORY_POWER, EVENT_BOOT_COMPLETED, null, bootTime)
                .send();

        if (SystemProperties.getBoolean("persist.sys.hw_statistics", true)) {
            new HardwareCollector(this).uploadHardwareInfo();
        }
    }

    private void onShutdown(Intent data) {
        long powerOnIncludeSleep;
        long powerOnNotSleep;
        try {
            powerOnIncludeSleep = data.
                    getLongExtra(AnalyticsHelper.EXTRA_TIME_INCLUDE_SLEEP, -1);
            powerOnNotSleep = data.
                    getLongExtra(AnalyticsHelper.EXTRA_TIME_NOT_COUNTING_SLEEP, -1);
        } catch (BadParcelableException e) {
            Log.w(TAG, "ignore BadParcelableException", e);
            return;
        }
        if (powerOnIncludeSleep == -1 || powerOnNotSleep == -1) {
            Log.w(TAG, "onShutdown, cannot get data");
            return;
        }
        mLogHelper.newEventBuilder(
                EVENT_CATEGORY_POWER, EVENT_SHUTDOWN, null, powerOnIncludeSleep)
                .setPower(powerOnNotSleep)
                .send();
    }

    private void onScreenOn(Intent data) {
        Long screenOffDuration = getDurationAndSaveScreenChangeTime();

        mLogHelper.newEventBuilder(
                EVENT_CATEGORY_POWER, EVENT_SCREEN_ON, null, screenOffDuration)
                .send();
    }

    private void onScreenOff(Intent data) {
        Long screenOnDuration = getDurationAndSaveScreenChangeTime();
        mLogHelper.newEventBuilder(
                EVENT_CATEGORY_POWER, EVENT_SCREEN_OFF, null, screenOnDuration)
                .send();
    }

    private static final String MAIN_THREAD = "main";

    private void onException(Intent data) {
        String exceptionDescription;
        String threadName;
        String packageName;
        try {
            exceptionDescription = data.getStringExtra(AnalyticsHelper.EXTRA_EXCEPTION);
            threadName = data.getStringExtra(AnalyticsHelper.EXTRA_THREAD_NAME);
            packageName = data.getStringExtra(AnalyticsHelper.EXTRA_PACKAGE_NAME);
        } catch (BadParcelableException e) {
            Log.w(TAG, "ignore BadParcelableException", e);
            return;
        }
        if (exceptionDescription == null) {
            Log.e(TAG, "onException, cannot get data");
            return;
        }

        if (threadName != null &&
            !threadName.isEmpty() &&
            !threadName.equals(MAIN_THREAD)) {
            exceptionDescription = "Thread: " + threadName + " " + exceptionDescription;
        }

        mLogHelper.newExceptionBuilder(exceptionDescription)
            .setPackageDimensions(packageName)
            .send();
    }

    private void onCustomEvent(Intent data) {
        String event_category;
        String event_action;
        String event_label;
        Long event_value;
        String packageName;
        boolean hasSampling;
        try {
            event_category = data.getStringExtra(AnalyticsHelper.EXTRA_EVENT_CATEGORY);
            event_action = data.getStringExtra(AnalyticsHelper.EXTRA_EVENT_ACTION);
            event_label = data.getStringExtra(AnalyticsHelper.EXTRA_EVENT_LABEL);
            event_value = (Long) data.getSerializableExtra(AnalyticsHelper.EXTRA_EVENT_VALUE);
            packageName = data.getStringExtra(AnalyticsHelper.EXTRA_PACKAGE_NAME);
            hasSampling = data.getBooleanExtra(AnalyticsHelper.EXTRA_HAS_SAMPLING, true);
        } catch (BadParcelableException e) {
            Log.w(TAG, "ignore BadParcelableException", e);
            return;
        }
        mLogHelper.newEventBuilder(
                event_category, event_action, event_label, event_value)
            .setPackageDimensions(packageName)
            .send();
    }

    private void initEventHandlers() {
        mStaticEventHandlers.put(AnalyticsHelper.ACTION_HIT_SCREEN, new EventHandler() {
            @Override
            void onEvent(Intent intent) {
                onHitScreen(intent);
            }
        });
        mStaticEventHandlers.put(AnalyticsHelper.ACTION_SHUTDOWN, new EventHandler() {
            @Override
            void onEvent(Intent intent) {
                onShutdown(intent);

                // Note: add one extra screen off event to calculate correct screen on duration
                // time
                onScreenOff(null);
                removeScreenChangeTime();
            }
        });
        mStaticEventHandlers.put(AnalyticsHelper.ACTION_SCREEN_ON, new EventHandler() {
            @Override
            void onEvent(Intent intent) {
                onScreenOn(intent);
            }
        });
        mStaticEventHandlers.put(AnalyticsHelper.ACTION_SCREEN_OFF, new EventHandler() {
            @Override
            void onEvent(Intent intent) {
                onScreenOff(intent);
            }
        });
        mStaticEventHandlers.put(AnalyticsHelper.ACTION_EXCEPTION, new EventHandler() {
            @Override
            void onEvent(Intent intent) {
                onException(intent);
            }
        });
        mStaticEventHandlers.put(AnalyticsHelper.ACTION_CUSTOM_EVENT, new EventHandler() {
            @Override
            void onEvent(Intent intent) {
                onCustomEvent(intent);
            }
        });
    }
    static abstract class EventHandler {
        abstract void onEvent(Intent intent);
    }
}
