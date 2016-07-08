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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.text.TextUtils;

public class AppNameHelper {
    private final Map<String, String> mAppNameMap = new HashMap<String, String>();

    /**
     * Gets package label: {package name} or {package name}/{app Chinese name}
     */
    public String getPackageLabel(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return "";
        }
        // get from cache
        String label = mAppNameMap.get(packageName);
        if (label != null) {
            return label;
        }

        label = AnalyticsHelper.getPackageLabel(context, packageName);
        if (!label.isEmpty() && !label.equals(packageName)) {
            mAppNameMap.put(packageName, label);
        }
        return label;
    }

    /**
     * Gets current package label: {package name} or {package name}/{app Chinese name}
     */
    public String getCurrentPackageLabel(Context context) {
        return getPackageLabel(context, getCurrentPackageName(context));
    }

    /**
     * Gets current package name
     */
    private static String getCurrentPackageName(Context context) {
        // get current task
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        if (runningTasks != null && runningTasks.size() > 0) {
            ActivityManager.RunningTaskInfo task = runningTasks.get(0);
            if (task.topActivity != null) {
                return task.topActivity.getPackageName();
            }
        }
        return "";
    }

}
