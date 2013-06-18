/*
 * Copyright (C) 2013 OTAPlatform
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

package com.beerbong.otaplatform.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.beerbong.otaplatform.DownloadService;
import com.beerbong.otaplatform.MainActivity;
import com.beerbong.otaplatform.NotificationAlarm;
import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.util.RequestFileActivity.RequestFileCallback;

public class Constants {

    public static final String GOO_SEARCH_URL = "http://goo.im/json2&path=";

    // install options
    public static final String INSTALL_BACKUP = "BACKUP";
    public static final String INSTALL_WIPESYSTEM = "WIPESYSTEM";
    public static final String INSTALL_WIPEDATA = "WIPEDATA";
    public static final String INSTALL_WIPECACHES = "WIPECACHES";
    public static final String INSTALL_FIXPERM = "FIXPERM";
    public static final String[] INSTALL_OPTIONS = {
            INSTALL_BACKUP,
            INSTALL_WIPESYSTEM,
            INSTALL_WIPEDATA,
            INSTALL_WIPECACHES,
            INSTALL_FIXPERM };
    public static final String INSTALL_OPTIONS_DEFAULT = INSTALL_BACKUP + "|" + INSTALL_WIPESYSTEM
            + "|" + INSTALL_WIPEDATA + "|" + INSTALL_WIPECACHES + "|" + INSTALL_FIXPERM;

    public static final int ALARM_ID = 122303221;
    public static final int NEWROMVERSION_NOTIFICATION_ID = 122303222;
    public static final int DOWNLOADROM_NOTIFICATION_ID = 122303223;
    public static final int NEWGAPPSVERSION_NOTIFICATION_ID = 122303224;
    public static final int DOWNLOADGAPPS_NOTIFICATION_ID = 122303225;
    public static final int DOWNLOADTWRP_NOTIFICATION_ID = 122303226;
    public static final String FILE_INFO = "OTAPLATFORM.FILE_INFO";

    public static final String PREFERENCE_SETTINGS_DARK_THEME = "darktheme";
    public static final String PREFERENCE_SETTINGS_DOWNLOAD_PATH = "downloadpath";
    public static final String PREFERENCE_SETTINGS_CHECK_TIME = "checktime";
    public static final String PREFERENCE_SETTINGS_GAPPS_CHECK = "checkgapps";
    public static final String PREFERENCE_SETTINGS_GAPPS_FOLDER = "gapps";
    public static final String PREFERENCE_SETTINGS_GAPPS_RESET = "gapps_reset";
    public static final String PREFERENCE_SETTINGS_RECOVERY = "recovery";
    public static final String PREFERENCE_SETTINGS_INTERNAL_SDCARD = "internalsdcard";
    public static final String PREFERENCE_SETTINGS_EXTERNAL_SDCARD = "externalsdcard";
    public static final String PREFERENCE_SETTINGS_OPTIONS = "showoptions";

    // recovery preferences
    public static final String PREFERENCE_RECOVERY_BACKUP = "recovery_activity_backup";
    public static final String PREFERENCE_RECOVERY_RESTORE = "recovery_activity_restore";
    public static final String PREFERENCE_RECOVERY_DELETE = "recovery_activity_delete";
    public static final String PREFERENCE_RECOVERY_ACTIONS = "recovery_activity_actions";
    public static final String PREFERENCE_RECOVERY_REBOOT = "recovery_activity_reboot";

    // overlays
    public static final String OVERLAY_CHANGELOG = "ro_otaplatform.changelog_url";
    public static final String OVERLAY_GAPPS_URL = "ro_otaplatform.gapps_url";
    public static final String OVERLAY_GAPPS_VERSION = "ro_otaplatform.gapps_version";
    public static final String OVERLAY_BACKUP_FILES = "ro_otaplatform.backup_files";

    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;

    private static List<RequestFileCallback> mRequestFileCallbacks = new ArrayList<RequestFileCallback>();

    private static int isSystemApp = -1;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss");

    public static void addRequestFileCallback(RequestFileCallback observer) {
        mRequestFileCallbacks.add(observer);
    }

    public static void fileRequested(String filePath) {
        for (RequestFileCallback observer : mRequestFileCallbacks) {
            observer.fileRequested(filePath);
        }
    }

    public static String getDateAndTime() {
        return SDF.format(new Date(System.currentTimeMillis()));
    }

    public static String getProperty(String prop) {
        try {
            String output = null;
            Process p = Runtime.getRuntime().exec("getprop " + prop);
            p.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            output = input.readLine();
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void showToastOnUiThread(final Context context, final int resourceId) {
        ((Activity) context).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(context, resourceId, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showToastOnUiThread(final Context context, final String string) {
        ((Activity) context).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showNotification(Context context, Updater.PackageInfo info,
            int notificationId, int resourceTitle, int resourceText) {
        Resources resources = context.getResources();

        Intent intent = new Intent(context, MainActivity.class);
        DownloadService.FileInfo fileInfo = new DownloadService.FileInfo();
        fileInfo.notificationId = notificationId;
        fileInfo.packageInfo = info;
        intent.putExtra(FILE_INFO, fileInfo);
        PendingIntent pIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification noti = new Notification.Builder(context)
                .setContentTitle(
                        resources.getString(resourceTitle, new Object[] { info.getVersion() }))
                .setContentText(
                        resources.getString(resourceText, new Object[] { info.getFilename() }))
                .setSmallIcon(R.drawable.ic_launcher).setContentIntent(pIntent).build();

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Service.NOTIFICATION_SERVICE);

        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(notificationId, noti);
    }

    public static String md5(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String md5 = bigInt.toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            return md5;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void setAlarm(Context context, long time, boolean trigger) {

        Intent i = new Intent(context, NotificationAlarm.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_ID, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        if (time > 0) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, trigger ? 0 : time, time, pi);
        }
    }

    public static boolean alarmExists(Context context) {
        return (PendingIntent.getBroadcast(context, ALARM_ID, new Intent(context,
                NotificationAlarm.class), PendingIntent.FLAG_NO_CREATE) != null);
    }

    public static String formatSize(final long value) {
        final long[] dividers = new long[] { T, G, M, K, 1 };
        final String[] units = new String[] { "TB", "GB", "MB", "KB", "B" };
        if (value < 1)
            throw new IllegalArgumentException("Invalid file size: " + value);
        String result = null;
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(final long value, final long divider, final String unit) {
        final double result = divider > 1 ? (double) value / (double) divider : (double) value;
        return String.format("%.1f %s", Double.valueOf(result), unit);
    }

    public static boolean isSystemApp(Context context) throws Exception {
        if (isSystemApp > -1) {
            return isSystemApp == 1;
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageInfo("com.beerbong.otaplatform",
                PackageManager.GET_ACTIVITIES);
        ApplicationInfo aInfo = info.applicationInfo;
        String path = aInfo.sourceDir.substring(0, aInfo.sourceDir.lastIndexOf("/"));
        isSystemApp = path.contains("system/app") ? 1 : 0;
        return isSystemApp == 1;
    }

    public static void showSimpleDialog(Context context, int titleId, int messageId) {
        new AlertDialog.Builder(context).setTitle(titleId).setMessage(messageId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }

}
