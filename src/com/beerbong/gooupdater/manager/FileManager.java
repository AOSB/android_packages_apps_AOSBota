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
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.beerbong.gooupdater.MainActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.util.Constants;
import com.beerbong.gooupdater.util.DownloadTask;

public class FileManager extends Manager {

    private static DownloadTask mDownloadFile;

    protected FileManager(Context context) {
        super(context);
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

    public void download(Context context, String url, String fileName, String md5) {

        Resources resources = context.getResources();

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("NOTIFICATION_ID", Constants.DOWNLOAD_NOTIFICATION_ID);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(resources.getString(R.string.downloading))
                .setContentText(
                        resources.getString(R.string.new_rom_name, new Object[] { fileName }))
                .setSmallIcon(R.drawable.ic_launcher).setContentIntent(pendingIntent)
                .setProgress(100, 0, true);

        mDownloadFile = new DownloadTask(notification, context, url, fileName, md5);
        mDownloadFile.execute();
    }

    public void cancelDownload() {
        if (mDownloadFile != null && mDownloadFile.getStatus() != Status.FINISHED) {
            mDownloadFile.cancel(true);
        }
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