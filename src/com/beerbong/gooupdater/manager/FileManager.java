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

package com.beerbong.gooupdater.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.beerbong.gooupdater.MainActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.ui.UI;
import com.beerbong.gooupdater.util.Constants;
import com.beerbong.gooupdater.util.DownloadTask;
import com.beerbong.gooupdater.util.DownloadTask.DownloadTaskListener;

public class FileManager extends Manager implements UI.OnNewIntentListener {

    private static DownloadTask mDownloadRom;
    private static DownloadTask mDownloadGapps;
    private static DownloadTask mDownloadTWRP;

    protected FileManager(Context context) {
        super(context);

        UI.getInstance().setOnNewIntentListener(this);
    }

    @Override
    public boolean onNewIntent(Context context, Intent intent, boolean preRedraw) {
        int notificationId = intent.getExtras() != null
                && intent.getExtras().get("NOTIFICATION_ID") != null ? Integer.parseInt(intent
                .getExtras().get("NOTIFICATION_ID").toString()) : -1;
        if (notificationId == Constants.NEWROMVERSION_NOTIFICATION_ID
                || notificationId == Constants.NEWGAPPSVERSION_NOTIFICATION_ID) {
            String url = intent.getExtras().getString("URL");
            String md5 = intent.getStringExtra("MD5");
            String name = intent.getStringExtra("ZIP_NAME");

            NotificationManager nMgr = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nMgr.cancel(notificationId);

            if (notificationId == Constants.NEWROMVERSION_NOTIFICATION_ID) {
                notificationId = Constants.DOWNLOADROM_NOTIFICATION_ID;
            } else {
                notificationId = Constants.DOWNLOADGAPPS_NOTIFICATION_ID;
            }
            ManagerFactory.getFileManager(context)
                    .download(context, url, name, md5, notificationId);
        } else if (notificationId == Constants.DOWNLOADROM_NOTIFICATION_ID
                || notificationId == Constants.DOWNLOADGAPPS_NOTIFICATION_ID
                || notificationId == Constants.DOWNLOADTWRP_NOTIFICATION_ID) {
            if (preRedraw) {
                DownloadTask task = null;
                switch (notificationId) {
                    case Constants.DOWNLOADROM_NOTIFICATION_ID:
                        if (mDownloadRom != null && mDownloadRom.getStatus() == Status.FINISHED) {
                            task = mDownloadRom;
                        }
                        break;
                    case Constants.DOWNLOADGAPPS_NOTIFICATION_ID:
                        if (mDownloadGapps != null && mDownloadGapps.getStatus() == Status.FINISHED) {
                            task = mDownloadGapps;
                        }
                        break;
                }
                if (task != null) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(task.getDestinationFile()));
                    sendIntent.setType("application/zip");
                    context.startActivity(Intent.createChooser(sendIntent, context.getResources().getString(R.string.open_with)));

                    NotificationManager nMgr = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    nMgr.cancel(notificationId);

                    return false;
                }
            }
            ManagerFactory.getFileManager().cancelDownload(notificationId, intent.getExtras());
        }
        return true;
    }

    public void selectDownloadPath(final Activity activity) {
        final EditText input = new EditText(activity);
        input.setText(ManagerFactory.getPreferencesManager().getDownloadPath());

        new AlertDialog.Builder(activity)
                .setTitle(R.string.download_alert_title)
                .setMessage(R.string.download_alert_summary)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();

                        if (value == null || "".equals(value.trim()) || !value.startsWith("/")) {
                            Toast.makeText(activity, R.string.download_alert_error,
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            return;
                        }

                        ManagerFactory.getPreferencesManager().setDownloadPath(value);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public void download(Context context, String url, String fileName, String md5,
            int notificationId) {
        download(context, url, fileName, md5, notificationId, null);
    }

    public void download(Context context, String url, String fileName, String md5,
            int notificationId, DownloadTaskListener listener) {

        Resources resources = context.getResources();

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("NOTIFICATION_ID", notificationId);
        intent.putExtra("MD5", md5);
        if (notificationId == Constants.DOWNLOADTWRP_NOTIFICATION_ID) {
            mDownloadTWRP = new DownloadTask(null, notificationId, context, url, fileName, md5);
            mDownloadTWRP.setDownloadTaskListener(listener);
            intent.putExtra("DESTINATION_FILE", mDownloadTWRP.getDestinationFile());
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(resources.getString(R.string.downloading))
                .setContentText(
                        resources.getString(R.string.new_package_name, new Object[] { fileName }))
                .setSmallIcon(R.drawable.ic_launcher_goo).setContentIntent(pendingIntent)
                .setProgress(100, 0, true);

        switch (notificationId) {
            case Constants.DOWNLOADROM_NOTIFICATION_ID:
                mDownloadRom = new DownloadTask(notification, notificationId, context, url,
                        fileName, md5);
                mDownloadRom.execute();
                break;
            case Constants.DOWNLOADGAPPS_NOTIFICATION_ID:
                mDownloadGapps = new DownloadTask(notification, notificationId, context, url,
                        fileName, md5);
                mDownloadGapps.execute();
                break;
            case Constants.DOWNLOADTWRP_NOTIFICATION_ID:
                notification.setAutoCancel(true);
                mDownloadTWRP.attach(notification, notificationId, context);
                mDownloadTWRP.execute();
                break;
        }
    }

    public void cancelDownload(final int notificationId, final Bundle extras) {
        switch (notificationId) {
            case Constants.DOWNLOADROM_NOTIFICATION_ID:
                if (mDownloadRom != null && mDownloadRom.getStatus() == Status.FINISHED) {
                    return;
                }
                break;
            case Constants.DOWNLOADGAPPS_NOTIFICATION_ID:
                if (mDownloadGapps != null && mDownloadGapps.getStatus() == Status.FINISHED) {
                    return;
                }
                break;
            case Constants.DOWNLOADTWRP_NOTIFICATION_ID:
                if (mDownloadTWRP != null && mDownloadTWRP.getStatus() == Status.FINISHED) {
                    return;
                }
                break;
        }
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.download_cancel_title)
                .setMessage(R.string.download_cancel_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        NotificationManager nMgr = (NotificationManager) mContext
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                        nMgr.cancel(notificationId);
                        switch (notificationId) {
                            case Constants.DOWNLOADROM_NOTIFICATION_ID:
                                if (mDownloadRom != null
                                        && mDownloadRom.getStatus() != Status.FINISHED) {
                                    mDownloadRom.cancel(true);
                                }
                                break;
                            case Constants.DOWNLOADGAPPS_NOTIFICATION_ID:
                                if (mDownloadGapps != null
                                        && mDownloadGapps.getStatus() != Status.FINISHED) {
                                    mDownloadGapps.cancel(true);
                                }
                                break;
                            case Constants.DOWNLOADTWRP_NOTIFICATION_ID:
                                if (mDownloadTWRP != null
                                        && mDownloadTWRP.getStatus() != Status.FINISHED) {
                                    mDownloadTWRP.cancel(true);
                                }
                                break;
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    public boolean recursiveDelete(File f) {
        try {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (!recursiveDelete(files[i])) {
                        return false;
                    }
                }
                if (!f.delete()) {
                    return false;
                }
            } else {
                if (!f.delete()) {
                    return false;
                }
            }
        } catch (Exception ignore) {
        }
        return true;
    }

    public boolean writeToFile(String data, String path, String fileName) {

        File folder = new File(path);
        File file = new File(folder, fileName);

        folder.mkdirs();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data.getBytes());
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public String readAssets(Context contex, String fileName) {
        BufferedReader in = null;
        StringBuilder data = null;
        try {
            data = new StringBuilder(2048);
            char[] buf = new char[2048];
            int nRead = -1;
            in = new BufferedReader(new InputStreamReader(contex.getAssets().open(fileName)));
            while ((nRead = in.read(buf)) != -1) {
                data.append(buf, 0, nRead);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        if (TextUtils.isEmpty(data)) {
            return null;
        }
        return data.toString();
    }
}