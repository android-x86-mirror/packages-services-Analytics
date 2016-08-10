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
package org.android_x86.hardwarecollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent data) {
        if (SystemProperties.getBoolean("persist.sys.hw_statistics", true)) {
            String action = data.getAction();
            Intent startIntent = new Intent(context, HardwareCollectorService.class);
            startIntent.setAction(action);
            context.startService(startIntent);
        }
    }
}
