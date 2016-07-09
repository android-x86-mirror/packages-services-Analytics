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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import org.android_x86.analytics.AnalyticsHelper;
import org.android_x86.analytics.GeneralLogs;
import org.android_x86.analytics.Fields.Type;
import org.android_x86.analytics.Fields.FieldEnum;
import org.android_x86.analytics.Fields.Metric;
import org.android_x86.analytics.Fields.Dimension;

public class LogHelper {
    private static final String TAG = "LogHelper";
    private static final boolean DEBUG = AnalyticsHelper.DEBUG;

    private static final double SAMPLING_RATE = 0; //TODO

    private final Context mContext;
    private final Tracker mTracker;

    /**
     * Log to Google analytics
     */
    public LogHelper(Context context) {
        mContext = context;
        mTracker = EasyTracker.getInstance(context);
    }

    public LogBuilder newAppViewBuilder() {
        return new LogBuilder(MapBuilder.createAppView());
    }

    public LogBuilder newEventBuilder(
            String category, String action, String label, Long value) {
        MapBuilder mapBuilder = MapBuilder.createEvent(category, action, label, value);
        LogBuilder builder = new LogBuilder(mapBuilder);
        return builder;
    }

    public LogBuilder newExceptionBuilder(String exceptionDescription) {
        return new LogBuilder(MapBuilder.createException(exceptionDescription, true));
    }

    public class LogBuilder {
        private final MapBuilder mBuilder;

        // do not check if field redundant!

        private LogBuilder(MapBuilder builder) {
            mBuilder = builder;
        }

        /**
         * Sets common field
         */
        private void setCommonField(FieldEnum key, String value) {
            if (mBuilder == null) return;
            switch (key) {
            case APP_ID:
                mBuilder.set(Fields.APP_ID, value);
                break;
            case APP_VERSION:
                mBuilder.set(Fields.APP_VERSION, value);
                break;
            case APP_NAME:
                mBuilder.set(Fields.APP_NAME, value);
                break;
            case SCREEN_NAME:
                mBuilder.set(Fields.SCREEN_NAME, value);
                break;
            default:
                throw new UnsupportedOperationException("Invalid field: " + key);
            }
        }

        /**
         * Sets Google Analytics field without check
         */
        private <T> LogBuilder set(Type type, T key, Object value) {
            if (mBuilder == null) return this;
            switch (type) {
            case DEFAULT:
                setCommonField((FieldEnum)key, value.toString());
                break;
            case CUSTOM_DIMENSION:
                mBuilder.set(Fields.customDimension(((Dimension)key).getValue()), value.toString());
                break;
            case CUSTOM_METRIC:
                mBuilder.set(Fields.customMetric(((Metric)key).getValue()), value.toString());
                break;
            default:
                throw new UnsupportedOperationException("Invalid Type: " + type + ", key: "
                        + key + ", value=" + value);
            }
            return this;
        }

        public LogBuilder setPower(long powerOnNotSleep) {
            set(Type.CUSTOM_METRIC, Metric.METRIC_POWER_ON_NOT_INCLUDE_SLEEP, powerOnNotSleep);
            return this;
        }

        public LogBuilder setPackageDimensions(String packageName) {
            // removed a lots of informations!
            set(Type.DEFAULT, FieldEnum.APP_NAME, packageName);
            return this;
        }

        public LogBuilder setActivityDimensions(ComponentName component) {
            // removed a lots of informations!
            set(Type.DEFAULT, FieldEnum.APP_NAME, component.getPackageName());
            set(Type.DEFAULT, FieldEnum.SCREEN_NAME, component.getClassName());
            return this;
        }

        public void send() {
            // removed a lots of informations!

            if (mBuilder != null) {
                // Add common fields to MapBuilder
                set(Type.CUSTOM_DIMENSION, Dimension.DIMENSION_BUILD_TYPE, Build.TYPE);
                set(Type.CUSTOM_DIMENSION, Dimension.DIMENSION_DEVICE, Build.DEVICE);
                set(Type.CUSTOM_DIMENSION, Dimension.DIMENSION_MODEL, Build.MODEL);
                set(Type.CUSTOM_DIMENSION, Dimension.DIMENSION_BUILD_VERSION, Util.BuildUtil.getBuildVersion());

                DisplayMetrics metrics = Util.getDefaultDisplayMetrics(mContext);
                float rate = Util.getDefaultDisplayRefreshRate(mContext);
                set(Type.CUSTOM_DIMENSION, Dimension.DIMENSION_RESOLUTION,
                        metrics.widthPixels + " * " + metrics.heightPixels + " "
                                + Integer.toString((int) rate) + "Hz");
                set(Type.CUSTOM_DIMENSION, Dimension.DIMENSION_DENSITY, metrics.densityDpi);

                Map<String, String> map = mBuilder.build();
                if (DEBUG) {
                    Log.d(TAG, "Google Analytics log entry: " + map);
                }
                mTracker.send(map);
            }
        }
    }
}
