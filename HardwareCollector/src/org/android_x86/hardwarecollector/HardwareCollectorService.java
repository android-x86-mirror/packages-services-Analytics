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

import org.android_x86.analytics.AnalyticsHelper;
import org.android_x86.analytics.GeneralLogs;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.hardware.input.InputManager;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.SystemProperties;
import android.system.Os;
import android.util.Log;
import android.view.InputDevice;

import org.json.JSONObject;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HardwareCollectorService extends IntentService {
    private static final String TAG = "HardwareCollectorService";

    private static final String GA_CATEGORY = "hardware_info";
    private static final String GA_ACTION_GPU_RENDERER = "gpu_renderer";
    private static final String GA_ACTION_CPU_MODEL = "cpu_model";
    private static final String GA_ACTION_TOUCH_SCREEN_NAME = "touch_screen_name";
    private static final String GA_ACTION_HAS_BATTERY = "has_battery";
    private static final String GA_ACTION_HAS_WIFI = "has_wifi";
    private static final String GA_ACTION_HAS_ETHERNET = "has_ethernet";
    private static final String GA_LABEL_HAS_BATTERY = "battery";
    private static final String GA_LABEL_NO_BATTERY = "no_battery";

    private static final String LAST_INFO_FILE_NAME = "lastInfo.json";
    private static final String CPU_INFO_FILE = "/proc/cpuinfo";
    private static final String CPU_INFO_MODEL_NAME_PRE = "model name\t: ";
    private static final String ETHERNET_SYS_FILE = "/sys/class/net/eth0/device/driver/module";
    private static final int TOUCHSCREEN_SOURCE_BIT = 4098;

    private Context mContext;
    private File mInfoFile;
    private JSONObject mInfoJson;

    public HardwareCollectorService() {
        super("HardwareCollectorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getBaseContext();
        mInfoFile = new File(getApplicationContext().getFilesDir(), LAST_INFO_FILE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "handle intent:" + intent);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            uploadHardwareInfo();
        }
    }

    private void uploadHardwareInfo() {
        getLastInfo();
        collectOpenGLInfo();
        collectCPUInfo();
        collectTouchScreenInfo();
        collectBatteryInfo();
        collectNetworkInfo();
    }

    private void collectOpenGLInfo() {
        try {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLSurface eglSurface = null;
            EGLContext eglContext = null;

            // initialize display
            EGLDisplay eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            int[] iparam = new int[2];
            egl.eglInitialize(eglDisplay, iparam);

            // choose config
            EGLConfig[] eglConfigs = new EGLConfig[1];
            final int[] configSpec =
                    {EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE};
            if (egl.eglChooseConfig(eglDisplay, configSpec, eglConfigs, 1, iparam)
                    && iparam[0] > 0) {
                // create surface
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                eglSurface = egl.eglCreateWindowSurface(
                        eglDisplay, eglConfigs[0], surfaceTexture, null);
                if (eglSurface != null && eglSurface != EGL10.EGL_NO_SURFACE) {
                    // create context
                    final int[] attribList = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
                    eglContext = egl.eglCreateContext(
                            eglDisplay, eglConfigs[0], EGL10.EGL_NO_CONTEXT, attribList);
                    egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
                }
            }

            checkAndSend(GA_ACTION_GPU_RENDERER, GLES20.glGetString(GLES20.GL_RENDERER));
        } catch (Exception e) {
            Log.e(TAG, "fail to get GPU renderer", e);
        }
    }

    private void collectCPUInfo() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CPU_INFO_FILE));
            String cpuInfo;
            String model = null;
            while ((cpuInfo = reader.readLine()) != null) {
                if (cpuInfo.contains(CPU_INFO_MODEL_NAME_PRE)) {
                    model = cpuInfo.substring(CPU_INFO_MODEL_NAME_PRE.length());
                    checkAndSend(GA_ACTION_CPU_MODEL, model);
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "fail to get CPU model name", e);
        }
    }

    private void collectTouchScreenInfo() {
        int[] ids = InputManager.getInstance().getInputDeviceIds();
        for (int id : ids) {
            InputDevice device = InputManager.getInstance().getInputDevice(id);
            String name = device.getName();
            if ((device.getSources() & TOUCHSCREEN_SOURCE_BIT) == TOUCHSCREEN_SOURCE_BIT) {
                checkAndSend(GA_ACTION_TOUCH_SCREEN_NAME, name);
                break;
            }
        }
    }

    private void collectBatteryInfo() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = mContext.registerReceiver(null, filter);
        String label = batteryIntent.getBooleanExtra("present", false) ?
                GA_LABEL_HAS_BATTERY : GA_LABEL_NO_BATTERY;
        AnalyticsHelper.CustomEvent customEvent = AnalyticsHelper.newSystemCoreEvent(
                                    mContext, GA_CATEGORY, GA_ACTION_HAS_BATTERY);
        customEvent.setLabel(label);
        customEvent.sendWithSampling();
    }

    private void collectNetworkInfo() {
        String wlan = SystemProperties.get("wlan.modname", "");
        if (!wlan.isEmpty()) {
            checkAndSend(GA_ACTION_HAS_WIFI, wlan);
        }

        try {
            File mod = new File(Os.readlink(ETHERNET_SYS_FILE));
            checkAndSend(GA_ACTION_HAS_ETHERNET, mod.getName());
        } catch (Exception e) {
            Log.d(TAG, "eth0 not found", e);
        }
    }

    private void getLastInfo() {
        try {
            if (mInfoFile.exists()) {
                String fileString = readFileAsString(mInfoFile.getPath());
                mInfoJson = new JSONObject(fileString);
            } else {
                mInfoJson = new JSONObject();
            }
        } catch (Exception e) {
            Log.e(TAG, "fail to get last info file", e);
        }
    }

    private boolean isDifferentInfo(String key, String value) {
        try {
            if (mInfoJson.has(key)) {
                String lastValue = mInfoJson.getString(key);
                return lastValue == null || !lastValue.equals(value);
            }
        } catch (Exception e) {
            Log.e(TAG, "check info failed", e);
        }
        return true;
    }

    private void checkAndSend(String key, String value) {
        if (isDifferentInfo(key, value)) {
            sendToGA(key, value);
            saveInfo(key, value);
        }
    }

    private void sendToGA(String action, String label) {
        AnalyticsHelper.CustomEvent customEvent =
                AnalyticsHelper.newSystemCoreEvent(mContext, GA_CATEGORY, action);
        customEvent.setLabel(label);
        customEvent.sendWithoutSampling();
    }

    private void sendToAnalytics(String key, String value) {
        GeneralLogs logs = new GeneralLogs();
        logs.set(key, value);
        AnalyticsHelper.uploadLogToLogServer(mContext, logs);
    }

    private void saveInfo(String key, String value) {
        try {
            mInfoJson.put(key, value);
            writeStringToFile(mInfoFile.getPath(), mInfoJson.toString());
        } catch (Exception e) {
            Log.e(TAG, "write info file failed", e);
        }
    }

    public static void writeStringToFile(String filePath, String sets) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter out = new PrintWriter(fileWriter);
        out.write(sets);
        out.println();
        fileWriter.close();
        out.close();
    }

    private static String readFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int count = 0;
        while ((count = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, count);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }
}
