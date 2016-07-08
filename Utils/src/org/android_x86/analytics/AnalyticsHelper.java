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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class AnalyticsHelper {
    private static final String TAG = "AnalyticsHelper";
    private static final int MS_IN_SECONDS = 1000;

    public static final boolean DEBUG = !"user".equals(Build.TYPE);

    public static final String TARGET_PACKAGE_NAME = "org.android_x86.analytics";
    public static final String TARGET_CLASS_NAME = "org.android_x86.analytics.AnalyticsService";

    public static final String ACTION_HIT_SCREEN = "org.android_x86.hit_screen";
    public static final String ACTION_SCREEN_ON = "org.android_x86.screen_on";
    public static final String ACTION_SCREEN_OFF = "org.android_x86.screen_off";
    public static final String ACTION_BOOT_COMPLETED = "org.android_x86.boot_completed";
    public static final String ACTION_SHUTDOWN = "org.android_x86.shutdown";
    public static final String ACTION_EXCEPTION = "org.android_x86.exception";
    public static final String ACTION_CUSTOM_EVENT = "org.android_x86.custom_event";
    public static final String ACTION_GENERAL = "org.android_x86.general";

    public static final String EXTRA_COMPONENT_NAME = "component_name";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_EXCEPTION = "exception";
    public static final String EXTRA_GENERAL = "general";
    public static final String EXTRA_THREAD_NAME = "thread_name";
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_TIME_NOT_COUNTING_SLEEP = "time_not_counting_sleep";
    public static final String EXTRA_TIME_INCLUDE_SLEEP = "time_include_sleep";

    public static final String EXTRA_EVENT_CATEGORY = "event_category";
    public static final String EXTRA_EVENT_ACTION = "event_action";
    public static final String EXTRA_EVENT_LABEL = "event_label";
    public static final String EXTRA_EVENT_VALUE = "event_value";

    public static final String EXTRA_HAS_SAMPLING = "has_sampling";

    private AnalyticsHelper() {}

    public static CharSequence getChineseText(PackageManager pm, ApplicationInfo info, int id)
            throws NameNotFoundException {
        Resources res = pm.getResourcesForApplication(info);
        Configuration config = res.getConfiguration();
        Locale locale = config.locale;
        config.locale = Locale.SIMPLIFIED_CHINESE;
        res.updateConfiguration(config, null);
        CharSequence s;
        try {
            s = res.getText(id);
        } catch (NotFoundException e) {
            s = "";
        }
        config.locale = locale;
        res.updateConfiguration(config, null);
        return s;
    }

    private static String getLabel(PackageManager pm, String packageName)
            throws NameNotFoundException {
        ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
        if (info.nonLocalizedLabel != null) {
            return info.nonLocalizedLabel.toString();
        }
        if (info.labelRes != 0) {
            CharSequence label = getChineseText(pm, info, info.labelRes);
            if (label != null) {
                return label.toString();
            }
        }
        // label is empty
        return "";
    }

    /**
     * Gets package label: {package name} or {package name}/{app Chinese name}
     */
    private static String getPackageLabel(PackageManager pm, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        try {
            String label = getLabel(pm, packageName);
            if (label.isEmpty()) {
                return packageName;
            } else {
                return packageName + '/' + label;
            }
        } catch (NameNotFoundException e) {
            return packageName;
        }
    }

    /**
     * Gets package label: {package name} or {package name}/{app Chinese name}
     */
    static String getPackageLabel(Context context, String packageName) {
        return getPackageLabel(context.getPackageManager(), packageName);
    }

    private static final String WIDGET_PREFIX = "android.widget.";

    /**
     * Gets the android widget class name.
     */
    public static String getWidgetName(View v) {
        if (v instanceof WebView) {
            return "WebView";
        }
        Class<?> cls = v.getClass();
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            String name = c.getName();
            if (name != null && name.startsWith(WIDGET_PREFIX)) {
                return name.substring(WIDGET_PREFIX.length());
            }
        }
        return cls.getName();
    }

    private static Intent getIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(TARGET_PACKAGE_NAME, TARGET_CLASS_NAME));
        return intent;
    }

    public static void hitScreen(Activity activity) {
        String componentName = activity.getComponentName().flattenToShortString();
        Intent intent = getIntent();
        intent.setAction(ACTION_HIT_SCREEN);
        intent.putExtra(EXTRA_COMPONENT_NAME, componentName);
        activity.startService(intent);
    }

    public static void screenOn(Context context) {
        Intent intent = getIntent();
        intent.setAction(ACTION_SCREEN_ON);
        context.startService(intent);
    }

    public static void screenOff(Context context) {
        Intent intent = getIntent();
        intent.setAction(ACTION_SCREEN_OFF);
        context.startService(intent);
    }

    public static void onBootCompleted(Context context) {
        Intent intent = getIntent();
        intent.setAction(ACTION_BOOT_COMPLETED);
        context.startService(intent);
    }

    public static void onShutdown(Context context) {
        Intent intent = getIntent();
        intent.setAction(ACTION_SHUTDOWN);

        // milliseconds since boot, not counting time spent in deep sleep
        intent.putExtra(AnalyticsHelper.EXTRA_TIME_NOT_COUNTING_SLEEP,
                SystemClock.uptimeMillis() / MS_IN_SECONDS);
        intent.putExtra(AnalyticsHelper.EXTRA_TIME_INCLUDE_SLEEP,
                SystemClock.elapsedRealtime() / MS_IN_SECONDS);
        context.startService(intent);
    }

    private static final int MAX_EXCEPTION_DESCRIPTION_LENGTH = 4 * 1024;

    public static String getExceptionDescription(Throwable e, int maxLength) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        String exceptionDescription = sw.toString();
        if (exceptionDescription.length() > maxLength) {
            return exceptionDescription.substring(0, maxLength - 3)
                    + "...";
        }
        return exceptionDescription;
    }

    public static void captureException(Context context,
            Throwable e, String threadName, String packageName) {
        Intent intent = getIntent();
        intent.setAction(ACTION_EXCEPTION);
        intent.putExtra(EXTRA_EXCEPTION,
                getExceptionDescription(e, MAX_EXCEPTION_DESCRIPTION_LENGTH));
        intent.putExtra(EXTRA_THREAD_NAME, threadName);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        context.startService(intent);
    }

    public static void uploadLogToLogServer(Context context, GeneralLogs logs) {
        Intent intent = getIntent();
        intent.setAction(ACTION_GENERAL);
        intent.putExtra(EXTRA_GENERAL, logs);
        context.startService(intent);
    }

    /**
     * Creates App's custom event.
     * The category is "context.getPackageName()"
     */
    public static CustomEvent newAppEvent(Context context, String action) {
        return new CustomEvent(context, context.getPackageName(), action);
    }

    /**
     * Creates App's custom event (include SystemUI).
     * The category is "context.getPackageName():subCategory"
     */
    public static CustomEvent newAppSubEvent(Context context, String subCategory,
            String action) {
        if (subCategory.isEmpty()) {
            Log.e(TAG, "empty subCategory");
        }
        return new CustomEvent(context, context.getPackageName() + ":" + subCategory, action);
    }

    /**
     * Creates system core's (cross multiple packages, e.g. multi-window, input, screenshot)
     * custom event.
     * The category is "system:subCategory"
     */
    public static CustomEvent newSystemCoreEvent(Context context, String subCategory,
            String action) {
        if (subCategory.isEmpty()) {
            Log.e(TAG, "empty subCategory");
        }
        return new CustomEvent(context, "system:" + subCategory, action);
    }

    public static class CustomEvent {
        private final Context mContext;
        private final String mCategory;
        private final String mAction;
        private String mLabel;
        private Long mValue;
        private Boolean mSampling;

        private CustomEvent(Context context, String category, String action) {
            mContext = context;
            mCategory = category;
            mAction = action;
        }

        /**
         * Sets other App package as label.
         */
        public CustomEvent setAppLabel(String packageName) {
            return setLabel(getPackageLabel(mContext, packageName));
        }

        public CustomEvent setLabel(String label) {
            mLabel = label;
            return this;
        }

        public CustomEvent setValue(Long value) {
            mValue = value;
            return this;
        }

        private void send(boolean hasSampling) {
            mSampling = hasSampling;
            if (DEBUG) {
                Log.d(TAG, "send " + this);
            }

            Intent intent = getIntent();
            intent.setAction(ACTION_CUSTOM_EVENT);

            // Google Analytics has the dimension of App ID, name, version to view it
            intent.putExtra(EXTRA_PACKAGE_NAME, mContext.getPackageName());
            intent.putExtra(EXTRA_EVENT_CATEGORY, mCategory);
            intent.putExtra(EXTRA_EVENT_ACTION, mAction);
            intent.putExtra(EXTRA_EVENT_LABEL, mLabel);
            intent.putExtra(EXTRA_EVENT_VALUE, mValue);
            intent.putExtra(EXTRA_HAS_SAMPLING, hasSampling);
            mContext.startService(intent);
        }

        public void sendWithSampling() {
            send(true);
        }

        public void sendWithoutSampling() {
            send(false);
        }

        @Override
        public String toString() {
            return new StringBuilder("CustomEvent {")
               .append("Category: " + mCategory)
               .append(" Action: " + mAction)
               .append(" Package: " + mContext.getPackageName())
               .append(" Label: " + mLabel)
               .append(" Value: " + mValue)
               .append(" Sampling: " + mSampling)
               .append("}").toString();
        }

        @Override
        protected void finalize() {
            if (mSampling == null) {
                Log.e(TAG, "Event is not sent: " + this);
            }
        }
    }
}
