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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryState {

    private final Intent mIntent;

    public BatteryState(Intent batteryChangedIntent) {
        mIntent = batteryChangedIntent;
    }

    /**
     * Gets BatteryState or null.
     */
    public static BatteryState of(Context context) {
        Intent intent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return null;
        }
        return new BatteryState(intent);
    }

    /**
     * Gets BatteryManager.EXTRA_STATUS, return BatteryManager.BATTERY_STATUS_UNKNOWN
     * if failed to get.
     */
    public int getStatus() {
        return mIntent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
    }

    /**
     * Gets BatteryManager.EXTRA_PLUGGED, return 0 if failed to get.
     */
    public int getPlugged() {
        return mIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
    }

    /**
     * Gets BatteryManager.EXTRA_LEVEL, return -1 if failed to get.
     */
    public int getLevel() {
        return mIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }

    /**
     * Gets BatteryManager.EXTRA_SCALE, return -1 if failed to get.
     */
    public int getScale() {
        return mIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    }

    /**
     * Gets battery percentage or -1 if failed.
     */
    public float getLevelPercentage() {
        int level = getLevel();
        int scale = getScale();
        if (level < 0 || scale <= 0) {
            return -1;
        }
        return (100.0f * level) / scale;
    }

    /**
     * Whether battery is charging
     */
    public boolean isCharging() {
        return isCharging(getStatus(), getPlugged());
    }

    /**
     * Whether battery is charging
     * @param status corresponds to BatteryManager.EXTRA_STATUS
     * @param plugged corresponds to BatteryManager.EXTRA_PLUGGED
     */
    public static boolean isCharging(int status, int plugged) {
        // fix bug: it's also charging when status is
        // "BATTERY_STATUS_DISCHARGING, BATTERY_PLUGGED_*" after boot machine with plugged
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL ||
               plugged != 0;
    }

    private static final int MIN_BATTERY_PERCENTAGE = 5;

    /**
     * Whether power is sufficient to do some heavy tasks
     */
    public boolean isPowerSufficient() {
        return isPowerSufficient(MIN_BATTERY_PERCENTAGE);
    }

    /**
     * Whether power is sufficient to do some heavy tasks
     */
    private boolean isPowerSufficient(int minBatteryPercentage) {
        return isCharging() || getLevelPercentage() > minBatteryPercentage;
    }

    /**
     * Whether power is sufficient to do some heavy tasks
     */
    public static boolean isPowerSufficient(Context context) {
        BatteryState state = of(context);
        if (state == null) {
            return false;
        }
        return state.isPowerSufficient();
    }

}
