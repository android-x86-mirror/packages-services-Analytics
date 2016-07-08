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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class GeneralLogs implements Parcelable {
    private final Map<String, String> mLogMap;

    public GeneralLogs() {
        mLogMap = new HashMap<String, String>();
    }

    public GeneralLogs(Parcel source) {
        mLogMap = new HashMap<String, String>();
        source.readMap(mLogMap, Map.class.getClassLoader());
    }

    public GeneralLogs set(String key, String value) {
        mLogMap.put(key, value);
        return this;
    }

    public Map<String, String> getLogMap() {
        return mLogMap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(mLogMap);
    }

    public static final Parcelable.Creator<GeneralLogs> CREATOR
            = new Parcelable.Creator<GeneralLogs>() {
        @Override
        public GeneralLogs createFromParcel(Parcel source) {
            return new GeneralLogs(source);
        }

        @Override
        public GeneralLogs[] newArray(int size) {
            return new GeneralLogs[size];
        }
    };
}
