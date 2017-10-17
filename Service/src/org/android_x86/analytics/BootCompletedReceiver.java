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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.android_x86.analytics.AnalyticsHelper;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    public static final String ACTION_SEND_LOGS = "org.android_x86.send_logs";

    @Override
    public void onReceive(Context context, Intent data) {
        String action = data.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.w(TAG, "unknow action:" + action);
            return;
        }
        AnalyticsHelper.onBootCompleted(context);

        // Set alarm to send logs periodically
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_SEND_LOGS);
        am.setInexactRepeating(AlarmManager.RTC, getStartTime(), AlarmManager.INTERVAL_HALF_HOUR,
                PendingIntent.getBroadcast(context, 0, intent, 0));
    }

    // begin and end hour that we enable sending logs
    private static final int BEGIN_HOUR = 0;
    private static final int END_HOUR = 24;
    private static final int DELAY_SECONDS = 10;

    /**
     * Gets start time in [BEGIN_HOUR, END_HOUR)
     */
    private static long getStartTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        if (currentHour >= BEGIN_HOUR || currentHour < END_HOUR) {
            // start after DELAY_SECONDS if boot in [BEGIN_HOUR, END_HOUR)
            calendar.add(Calendar.SECOND, DELAY_SECONDS);
        } else {
            // choose time in [BEGIN_HOUR, END_HOUR) randomly
            int startHour = BEGIN_HOUR + new Random().nextInt(END_HOUR + 24 - BEGIN_HOUR);
            calendar.add(Calendar.HOUR_OF_DAY, startHour - currentHour);
        }
        return calendar.getTimeInMillis();
    }
}
