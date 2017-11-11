/*
 * Copyright (c) 2017 Martin Pfeffer
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

package io.celox.app.flashlights.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import com.pepperonas.andbasx.AndBasx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.celox.app.flashlights.BuildConfig;
import io.celox.app.flashlights.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * The type Utils.
 *
 * @author Martin Pfeffer <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class Utils {

    private static final String TAG = "Utils";

    /**
     * Byte to hex str string.
     *
     * @param bytes the bytes
     * @return the string
     */
    public static String byteToHexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    /**
     * Hex string to byte array byte [ ].
     *
     * @param s the s
     * @return the byte [ ]
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Hex to binary string.
     *
     * @param hex the hex
     * @return the string
     */
    public static String hexToBinary(String hex) {
        String value = new BigInteger(hex, 16).toString(2);
        String formatPad = "%" + (hex.length() * 4) + "s";
        return String.format(formatPad, value).replace(" ", "0");
    }

    /**
     * Change endian of hex str string.
     *
     * @param data the data
     * @return the string
     */
    public static String changeEndianOfHexStr(String data) {
        if (data == null || data.isEmpty() || data.length() % 2 != 0) {
            Log.w(TAG, "Data miss-formed!");
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int ctr = 0;
        for (int i = 0; i < data.length(); i = i + 2) {
            int pre = data.length() - ctr - 2;
            int aft = data.length() - ctr;
            if (pre <= 0) {
                pre = 0;
            }
            if (aft <= 0) {
                aft = 2;
            }
            builder.append(data.substring(pre, aft));
            ctr += 2;
        }
        return builder.toString().toUpperCase();
    }

    /**
     * Convert input stream to string string.
     *
     * @param inputStream the input stream
     * @return the string
     * @throws IOException the io exception
     */
    @SuppressWarnings("unused")
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }

        inputStream.close();
        return result.toString();
    }

    /**
     * Run on background thread.
     *
     * @param callable the callable
     */
    public static void runOnBackgroundThread(final Callable callable) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (Exception e) {
                    Log.e(TAG, "runOnBackgroundThread: ", e);
                }
            }
        });
    }

    /**
     * Run on main ui thread.
     *
     * @param callable the callable
     */
    public static void runOnMainUiThread(final Callable callable) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (Exception e) {
                    Log.e(TAG, "runOnMainUiThread: ", e);
                }
            }
        });
    }

    /**
     * Run delayed.
     *
     * @param callable the callable
     * @param delay    the delay
     */
    public static void runDelayed(final Callable callable, long delay) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    /**
     * Run thread safe.
     *
     * @param callable the callable
     */
    public static void runThreadSafe(final Callable callable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        callable.call();
                    } catch (Exception e) {
                        Log.e(TAG, "runThreadSafe: ", e);
                    }
                }
            });
        }
    }

    /**
     * Gets readable time stamp.
     *
     * @return the readable time stamp
     */
    public static String getReadableTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY);
        Date dt = new Date();
        return sdf.format(dt);
    }

    /**
     * Gets readable time stamp.
     *
     * @param millis the millis
     * @return the readable time stamp
     */
    public static String getReadableTimeStamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY);
        Date dt = new Date(millis);
        return sdf.format(dt);
    }

    /**
     * Load preferences.
     */
    @SuppressWarnings("unused")
    public static void loadPreferences() {
        SharedPreferences preferences = AndBasx.getContext().getSharedPreferences(
                AesConst.AES_PREFS_FILE_NAME, MODE_PRIVATE);
        Map<String, ?> prefs = preferences.getAll();
        for (String key : prefs.keySet()) {
            Object pref = prefs.get(key);
            String printVal = "";
            if (pref instanceof Boolean) {
                printVal = key + " : " + pref;
            }
            if (pref instanceof Float) {
                printVal = key + " : " + pref;
            }
            if (pref instanceof Integer) {
                printVal = key + " : " + pref;
            }
            if (pref instanceof Long) {
                printVal = key + " : " + pref;
            }
            if (pref instanceof String) {
                printVal = key + " : " + pref;
            }
            if (pref instanceof Set<?>) {
                printVal = key + " : " + pref;
            }

            Log.d(TAG, "loadPreferences: " + printVal);
        }
    }

    /**
     * Converts a string to MD5 hash string.
     *
     * @param string value that should be hashed.
     * @return MD5 hash of the given string.
     */
    public static String stringToMd5(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(string.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return String.format("%0" + (messageDigest.length << 1) + "x", number);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Decrypts an AES encrypted message to a readable string.
     *
     * @param encryptedMsg message that should be decrypted.
     * @param iv           IV of the message.
     * @return decrypted string of the message.
     */
    public static String decryptAes(String encryptedMsg, String iv) {
        final Charset ASCII = Charset.forName("US-ASCII");
        byte[] encryptedMsgBytes = Base64.decode(encryptedMsg, Base64.DEFAULT);
        byte[] ivByte = Base64.decode(iv, Base64.DEFAULT);
        byte[] keyBytes = "_password".getBytes(ASCII);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(ivByte));
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
            e.printStackTrace();
            return "";
        }
        byte[] result;
        try {
            result = cipher.doFinal(encryptedMsgBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return "";
        }
        // Padding. Read the last byte. if it is between 0 - 16 (decimal) then it must be cut with that number from right of the String.
        int length = result.length;
        if (result.length == 0) return "";

        int i = result[length - 1];
        if (i <= 16 && i > 0) {
            byte[] paddedResult = new byte[length - i];
            System.arraycopy(result, 0, paddedResult, 0, paddedResult.length);
            return new String(paddedResult);
        } else {
            return new String(result);
        }
    }

    /**
     * Gets device imei.
     *
     * @param context the context
     * @return the device imei
     */
    @SuppressLint({"HardwareIds", "MissingPermission"})
    public static String getDeviceImei(Context context) {
        try {

            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "getDeviceImei: Missing permission to get IMEI.");
                return "-1";
            }
            String imei;
            if (tm != null) {
                try {
                    imei = tm.getDeviceId();
                } catch (Exception e) {
                    Log.e(TAG, "getDeviceImei: ", e);
                    imei = Const.IMEI_NOT_FOUND;
                }
            } else {
                Log.w(TAG, "getDeviceImei: TelephonyManager is null. Can't resolve IMEI.");
                return "-1";
            }
            if (imei == null) {
                imei = Const.IMEI_NOT_FOUND;
            }
            return imei;
        } catch (Exception e) {
            Log.e(TAG, "getDeviceImei: ", e);
            return "-1";
        }
    }

    /**
     * Gets build version.
     *
     * @param context the context
     * @return the build version
     */
    public static String getBuildVersion(Context context) {
        String summary = BuildConfig.VERSION_NAME + "-" + context.getString(R.string.flv_variant);
        Date date = new Date(BuildConfig.APP_CREATED);
        SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd.HH.mm.ss", Locale.GERMANY);
        return summary + "-" + sdf.format(date);
    }

    /**
     * Gets version code.
     *
     * @param context the context
     * @return the version code
     */
    public static int getVersionCode(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @SuppressWarnings("unused")
    private int findMax(int... values) {
        int max = Integer.MIN_VALUE;
        for (int i : values) {
            if (i > max) max = i;
        }
        return max;
    }

    /**
     * Hide soft keyboard.
     *
     * @param fragment the fragment
     */
    public static void hideSoftKeyboard(Fragment fragment) {
        View view = fragment.getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) fragment.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } else {
                Log.w(TAG, "Can't hide soft keyboard.");
            }
        }
    }

    /**
     * Format timestamp string.
     *
     * @param millis the millis
     * @return the string
     */
    public static String formatTimestamp(long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    /**
     * Sets ripple.
     *
     * @param context     the context
     * @param imageButton the image button
     */
    public static void setRipple(Context context, ImageButton imageButton) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        imageButton.setBackgroundResource(outValue.resourceId);
    }

    private String fmt(int number, int places) {
        return String.format("%0" + places + "d", number);
    }

}
