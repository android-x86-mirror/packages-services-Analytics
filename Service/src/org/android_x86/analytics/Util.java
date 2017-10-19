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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Random;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.PowerManager;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.Secure;

public class Util {
    private Util() {
    }

    public static DisplayMetrics getDefaultDisplayMetrics(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        return metrics;
    }

    public static float getDefaultDisplayRefreshRate(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getRefreshRate();
    }

    /* --- DebugUtil --- */

    /**
     * Gets Intent's debug string with extras.
     */
    public static String toString(Intent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append(intent.getAction());
        Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {
            sb.append(" " + extras);
        }
        return sb.toString();
    }

    /* --- PowerUtil --- */

    public static boolean isScreenOn(Context context) {
        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
    }

    /* --- BuildUtil --- */

    public static class BuildUtil {

        public static final String UNKNOWN = "unknown";

        public static final String NIGHTLY = "nightly";
        public static final String BETA = "beta";
        public static final String STABLE = "stable";

        /**
         * Gets locale string.
         */
        public static String getLocale(Context context) {
            return context.getResources().getConfiguration().locale.toString();
        }

        /**
         * Gets build flavor
         */
        public static String getFlavor() {
            String version = Build.VERSION.INCREMENTAL;
            if (!version.isEmpty()) {
                switch (version.charAt(0)) {
                    case 'N':
                        return NIGHTLY;
                    case 'B':
                        return BETA;
                    case 'S':
                        return STABLE;
                }
            }
            return UNKNOWN;
        }

        /**
         * Gets ANDROID_ID or empty string if not present.
         */
        public static String getAndroidID(Context context) {
            String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            return androidId == null ? "" : androidId;
        }

        /**
         * Gets full build version.
         */
        public static String getBuildVersion() {
            if (Build.VERSION.INCREMENTAL.startsWith(Build.VERSION.RELEASE)) {
                return Build.VERSION.INCREMENTAL;
            }
            return Build.VERSION.RELEASE + '-' + Build.VERSION.INCREMENTAL;
        }

        /**
         * Gets product version.
         */
        public static String getProductVersion() {
            return Build.PRODUCT + ' ' + Build.VERSION.RELEASE + ' ' + Build.ID;
        }
    }

    /* --- IOUtil --- */

    public static class IOUtil {

        public static final String UTF8 = "UTF-8";

        private static final int BUFFER_SIZE = 16 * 1024;

        /**
         * Reads input stream to an output stream and close input stream (not close output stream).
         * @param in
         * @param out
         * @throws IOException
         */
        public static void toOutputStream(InputStream in, OutputStream out) throws IOException {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int size;
                while ((size = in.read(buffer)) != -1) {
                    out.write(buffer, 0, size);
                }
            } finally {
                in.close();
            }
        }

        /**
         * Writes byte array to output stream and close output stream.
         * @throws IOException
         */
        public static void toAndCloseOutputStream(byte[] data, OutputStream out) throws IOException {
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }

        /**
         * Writes a string to output stream and close output stream.
         * @throws IOException
         */
        public static void toAndCloseOutputStream(String s, OutputStream out) throws IOException {
            toAndCloseOutputStream(s.getBytes(UTF8), out);
        }

        /**
         * Reads input stream to a string and close input stream.
         * @throws IOException
         */
        public static String toString(InputStream in) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                toOutputStream(in, out);
                return out.toString(UTF8);
            } finally {
                out.close();
            }
        }
    }

    /* --- HttpUtil --- */

    public static class HttpStatusException extends Exception {
        private final int mStatusCode;

        public HttpStatusException(int statusCode) {
            super();
            mStatusCode = statusCode;
        }

        public HttpStatusException(int statusCode, String message) {
            super(message);
            mStatusCode = statusCode;
        }

        @Override
        public String getMessage() {
            return "StatusCode: " + mStatusCode + " " + super.getMessage();
        }

        public int getStatusCode() {
            return mStatusCode;
        }
    }

    public static class HttpStatusLineException extends HttpStatusException {
        private final StatusLine mStatusLine;

        public HttpStatusLineException(StatusLine statusLine) {
            super(statusLine.getStatusCode());
            mStatusLine = statusLine;
        }

        public HttpStatusLineException(StatusLine statusLine, String message) {
            super(statusLine.getStatusCode(), message);
            mStatusLine = statusLine;
        }

        @Override
        public String getMessage() {
            return "StatusLine: " + mStatusLine + " " + super.getMessage();
        }

        public StatusLine getStatusLine() {
            return mStatusLine;
        }
    }

    /**
     * HTTP post to given URL.
     */
    public static HttpEntity doPost(String url, HttpEntity entity)
            throws URISyntaxException, IOException, HttpStatusLineException {
        HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "android");
        HttpPost request = new HttpPost();
        request.setURI(new URI(url));
        request.setEntity(entity);

        HttpResponse response = client.execute(request);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() !=  HttpStatus.SC_OK) {
            throw new HttpStatusLineException(statusLine);
        }
        return response.getEntity();
    }

    /**
     * Gets JSON from URL
     */
    public static JSONObject getJsonFromUrl(String url)
            throws MalformedURLException, JSONException, IOException {
        return new JSONObject(IOUtil.toString(new URL(url).openStream()));
    }

    /**
     * Posts JSON request and get JSON reply.
     */
    public static JSONObject postAndGetJson(String url, String jsonRequest)
            throws MalformedURLException, IOException, HttpStatusException, JSONException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf8");
        conn.setRequestProperty("Accept", "application/json");
        IOUtil.toAndCloseOutputStream(jsonRequest, conn.getOutputStream());

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new HttpStatusException(responseCode);
        }
        return new JSONObject(IOUtil.toString(conn.getInputStream()));
    }

    /**
     * Posts JSON request and get JSON reply.
     */
    public static JSONObject postAndGetJson(String url, JSONObject jsonRequest)
            throws MalformedURLException, IOException, HttpStatusException, JSONException {
        return postAndGetJson(url, jsonRequest.toString());
    }

    /* --- NetUtil --- */

    public static ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Whether network is good to send cloud server requests
     */
    public static boolean isNetworkGood(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    /**
     * Whether network is good to send cloud server requests
     */
    public static boolean isNetworkGood(Context context) {
        return isNetworkGood(getConnectivityManager(context));
    }

    /**
     * Gets MAC address or empty string.
     */
    public static String getMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String mac = wifiInfo.getMacAddress();
            if (mac != null) {
                return mac;
            }
        }
        return "";
    }

    /* --- SecurityUtil --- */

    public static class SecurityUtil {

        /**
         * Converts bytes to hex string.
         */
        public static String byteToHex(byte[] bytes) {
            Formatter formatter = new Formatter();
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
        }

        /**
         * Use SHA-1 algorithm to hash given string and return a hex string.
         */
        public static String sha1(String s)
                throws NoSuchAlgorithmException, UnsupportedEncodingException {
            return byteToHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8")));
        }

    }

    /* --- SignatureBuilder --- */

    public static class SignatureBuilder {
        private final List<String> mInfos;

        private SignatureBuilder(String... infos) {
            mInfos = new ArrayList<String>();
            for (String s : infos) {
                mInfos.add(s);
            }
        }

        /**
         * Gets a random SignatureBuilder
         */
        public static SignatureBuilder of() {
            return new SignatureBuilder(
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(new Random().nextLong()),
                    Build.getSerial());
        }

        /**
         * Gets informations used to generate signature.
         */
        public List<String> getInfos() {
            return mInfos;
        }

        /**
         * Builds signature with secret key and separator.
         */
        public String build(String key, String separator)
                throws NoSuchAlgorithmException, UnsupportedEncodingException {
            List<String> list = new ArrayList<String>(mInfos);
            list.add(key);
            Collections.sort(list);

            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                sb.append(s).append(separator);
            }
            return SecurityUtil.sha1(sb.toString());
        }

        /**
         * Builds signature with secret key and separator, return comma separated string of source
         * informations and signature.
         */
        public String buildToJoinString(String key, String separator)
                throws NoSuchAlgorithmException, UnsupportedEncodingException {
            String signature = build(key, separator);
            StringBuilder sb = new StringBuilder();
            for (String s : mInfos) {
                sb.append(s).append(',');
            }
            sb.append(signature);
            return sb.toString();
        }
    }
}
