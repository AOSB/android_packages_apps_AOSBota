/*
 * Copyright (C) 2013 GooUpdater
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

package com.beerbong.gooupdater.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.Toast;

import com.beerbong.gooupdater.MainActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.Service;
import com.beerbong.gooupdater.updater.Updater.PackageInfo;

public class Constants {

    public static final int NEWROMVERSION_NOTIFICATION_ID = 122303222;
    public static final int DOWNLOADROM_NOTIFICATION_ID = 122303223;
    public static final int NEWGAPPSVERSION_NOTIFICATION_ID = 122303224;
    public static final int DOWNLOADGAPPS_NOTIFICATION_ID = 122303225;
    public static final int DOWNLOADTWRP_NOTIFICATION_ID = 122303226;

    public static final String PREFERENCE_SETTINGS_DARK_THEME = "darktheme";
    public static final String PREFERENCE_SETTINGS_DOWNLOAD_PATH = "downloadpath";
    public static final String PREFERENCE_SETTINGS_CHECK_TIME = "checktime";

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

    public static void showNotification(Context context, PackageInfo info, int notificationId,
            int resourceTitle, int resourceText) {
        Resources resources = context.getResources();

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("NOTIFICATION_ID", notificationId);
        intent.putExtra("URL", info.path);
        intent.putExtra("ZIP_NAME", info.filename);
        intent.putExtra("MD5", info.md5);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification noti = new Notification.Builder(context)
                .setContentTitle(resources.getString(resourceTitle))
                .setContentText(resources.getString(resourceText, new Object[] { info.filename }))
                .setSmallIcon(R.drawable.ic_launcher_goo).setContentIntent(pIntent).build();

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
}
