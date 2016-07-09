/*
 * Copyright 2016 Jide Technology Ltd.
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

import android.content.Context;
import android.os.SystemClock;

public class PowerStats {
    private static final long MIN_STATS_INTERVAL_MILLIS = 15 * 60 * 1000;
    private static final long TEN_HOUR_MILLIS = 10 * 60 * 60 * 1000;

    private static final String EVENT_CATEGORY_POWER_USAGE = "power_usage";
    private static final String ACTION_DISCHARGE_SCREEN_ON = "discharge_screen_on";
    private static final String ACTION_DISCHARGE_SCREEN_OFF = "discharge_screen_off";

    private final boolean mIsScreenOn;
    private final boolean mIsCharging;
    private final long mTime;
    private final float mPercentage;

    private static PowerStats mPowerStats;

    private PowerStats(boolean isScreenOn, boolean isCharging, long time, float percentage) {
        mIsScreenOn = isScreenOn;
        mIsCharging = isCharging;
        mTime = time;
        mPercentage = percentage;
    }

    @Override
    public String toString() {
        return "isScreenOn: " + mIsScreenOn
                + " isCharging: " + mIsCharging
                + " time: " +  mTime
                + " percentage: " + mPercentage;
    }

    public static void onScreenOn(Context context) {
        onIntent(context, true, null);
    }

    public static void onScreenOff(Context context) {
        onIntent(context, false, null);
    }

    public static void onPowerConnected(Context context) {
        onIntent(context, null, true);
    }

    public static void onPowerDisconnected(Context context) {
        onIntent(context, null, false);
    }

    private static void onIntent(Context context, Boolean isScreenOn, Boolean isCharging) {
        BatteryState state = BatteryState.of(context);
        if (state == null) {
            return;
        }
        float percentage = state.getLevelPercentage();
        if (percentage < 0) {
            return;
        }
        new PowerStats(
                isScreenOn != null ? isScreenOn : Util.isScreenOn(context),
                isCharging != null ? isCharging : state.isCharging(),
                SystemClock.elapsedRealtime(),
                percentage)
        .handle(context);
    }

    private void handle(Context context) {
        PowerStats previous;
        synchronized(PowerStats.class) {
            previous = mPowerStats;
            // only save battery percentage when screen on/off or charging status change.
            if (previous != null
                    && (mIsScreenOn == previous.mIsScreenOn)
                    && (mIsCharging == previous.mIsCharging)) {
                return;
            }
            mPowerStats = this;
        }
        if (previous == null) {
            return;
        }
        long interval = mTime - previous.mTime;
        float percentageReduce = previous.mPercentage - mPercentage;
        if (interval > MIN_STATS_INTERVAL_MILLIS
                && percentageReduce >= 0
                && !previous.mIsCharging) {
            long value = (long) (percentageReduce * TEN_HOUR_MILLIS / interval);
            AnalyticsHelper.newSystemCoreEvent(context,
                    EVENT_CATEGORY_POWER_USAGE,
                    previous.mIsScreenOn ? ACTION_DISCHARGE_SCREEN_ON
                                         : ACTION_DISCHARGE_SCREEN_OFF)
                .setLabel(Long.toString(value))
                .setValue(value)
                .sendWithSampling();
        }
    }
}
