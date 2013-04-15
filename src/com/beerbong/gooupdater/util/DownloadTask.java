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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StatFs;

import com.beerbong.gooupdater.MainActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.manager.PreferencesManager;

public class DownloadTask extends AsyncTask<Void, Integer, Integer> {

    public interface DownloadTaskListener {
        public void downloadComplete(int status, File file);
    }

    private int mScale = 1048576;

    private DownloadTaskListener mListener;
    private NotificationManager mNotificationManager;
    private Notification.Builder mNotification = null;
    private Context mContext;
    private File mDestFile;
    private String mUrl;
    private String mFileName;
    private String mMd5;
    private final WakeLock mWakeLock;
    private int mNotificationId;

    private boolean mDone = false;

    @SuppressWarnings("deprecation")
    public DownloadTask(Notification.Builder notification, int notificationId, Context context,
            String url, String fileName, String md5) {
        this.attach(notification, notificationId, context);

        PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        File dPath = new File(pManager.getDownloadPath());
        dPath.mkdirs();

        mUrl = url;
        mFileName = fileName;
        mMd5 = md5;

        mDestFile = new File(pManager.getDownloadPath(), mFileName);
        String extension = mFileName.substring(mFileName.lastIndexOf("."));
        String name = mFileName.substring(0, mFileName.lastIndexOf("."));
        int i = 0;
        while (mDestFile.exists()) {
            i++;
            mFileName = name + "(" + i + ")" + extension;
            mDestFile = new File(pManager.getDownloadPath(), mFileName);
        }

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, MainActivity.class.getName());
    }

    public void attach(Notification.Builder notification, int notificationId, Context context) {
        mNotification = notification;
        mNotificationId = notificationId;
        mContext = context;
    }

    public void detach() {
        mNotification = null;
        mContext = null;
    }

    public boolean isDone() {
        return mDone;
    }

    public File getDestinationFile() {
        return mDestFile;
    }

    public String getMd5() {
        return mMd5;
    }

    public void setDownloadTaskListener(DownloadTaskListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        mDone = false;
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, mNotification.build());
        mWakeLock.acquire();
    }

    /**
     * The "goo.im" part is kanged from
     * https://github.com/OTAUpdateCenter/ota-update-centre/blob/master/src/com/otaupdater/OTAUpdaterActivity.java
     */
    @Override
    protected Integer doInBackground(Void... params) {
        PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        String login = pManager.getLogin();
        String url = mUrl;
        if (login != null && !"".equals(login)) {
            url = url + "&hash=" + login;
        }

        if (mMd5 != null) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(ManagerFactory.getPreferencesManager()
                        .getDownloadPath(), mFileName + ".md5sum"));
                fos.write((mMd5 + " " + mFileName).getBytes());
            } catch (Exception ex) {
            } finally {
                if (fos != null)
                    try {
                        fos.close();
                    } catch (Exception ex) {
                    }
            }
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            URL getUrl = new URL(url);
            URLConnection conn = getUrl.openConnection();
            if (getUrl.toString().contains("goo.im") && (login == null || "".equals(login))) {
                conn.connect();
                publishProgress(-1);
                is = new BufferedInputStream(conn.getInputStream());
                os = new FileOutputStream(mDestFile);
                byte[] buf = new byte[4096];
                int nRead = -1;
                while ((nRead = is.read(buf)) != -1) {
                    if (this.isCancelled())
                        break;
                    os.write(buf, 0, nRead);
                }
                try {
                    Thread.sleep(10500);
                } catch (InterruptedException e) {
                }
                getUrl = new URL(url);
                conn = getUrl.openConnection();
            }
            final int lengthOfFile = conn.getContentLength();
            StatFs stat = new StatFs(pManager.getDownloadPath());
            long availSpace = ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());
            if (lengthOfFile >= availSpace) {
                mDestFile.delete();
                if (mListener != null) {
                    mListener.downloadComplete(3, null);
                }
                return 3;
            }
            if (lengthOfFile < 10000000)
                mScale = 1024;
            publishProgress(0, lengthOfFile);
            conn.connect();
            is = new BufferedInputStream(conn.getInputStream());
            os = new FileOutputStream(mDestFile);
            byte[] buf = new byte[4096];
            int nRead = -1;
            int totalRead = 0;
            while ((nRead = is.read(buf)) != -1) {
                if (this.isCancelled())
                    break;
                os.write(buf, 0, nRead);
                totalRead += nRead;
                publishProgress(totalRead, lengthOfFile);
            }

            if (isCancelled()) {
                mDestFile.delete();
                if (mListener != null) {
                    mListener.downloadComplete(2, null);
                }
                return 2;
            }

            if (mListener != null) {
                mListener.downloadComplete(0, mDestFile);
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            mDestFile.delete();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Exception e) {
                }
            }
        }
        if (mListener != null) {
            mListener.downloadComplete(-1, null);
        }
        return -1;
    }

    @Override
    protected void onCancelled(Integer result) {
        mDone = true;
        mWakeLock.release();
        mWakeLock.acquire(30000);
        int resource = -1;
        if (result == null) {
            resource = R.string.downloading_error;
        } else {
            switch (result) {
                case 0:
                    resource = R.string.downloading_complete;
                    break;
                case 2:
                    resource = R.string.downloading_interrupted;
                    break;
                case 3:
                    resource = R.string.downloading_nospace;
                    break;
                default:
                    resource = R.string.downloading_error;
            }
        }
        mNotification.setContentTitle(mContext.getResources().getText(resource)).setProgress(0, 0,
                false).setContentInfo("");
        mNotificationManager.notify(mNotificationId, mNotification.build());
    }

    @Override
    protected void onPostExecute(Integer result) {
        mDone = true;
        mWakeLock.release();
        mWakeLock.acquire(30000);

        int resource = -1;
        if (result == null) {
            resource = R.string.downloading_error;
        } else {
            switch (result) {
                case 0:
                    resource = R.string.downloading_complete;
                    break;
                case 2:
                    resource = R.string.downloading_interrupted;
                    break;
                case 3:
                    resource = R.string.downloading_nospace;
                    break;
                default:
                    resource = R.string.downloading_error;
            }
        }
        mNotification.setContentTitle(mContext.getResources().getText(resource)).setProgress(0, 0,
                false).setContentInfo("");
        mNotificationManager.notify(mNotificationId, mNotification.build());
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mNotification == null)
            return;
        if (values[0] == -1) {
            mNotification.setProgress(100, -1, true);
            return;
        }
        if (values.length == 0)
            return;
        int progress = values[0] / mScale;
        mNotification.setProgress(100, progress, false);
        if (values.length > 0) {
            int total = values[1] / mScale;
            mNotification.setProgress(total, progress, false);
            mNotification.setContentInfo(String.valueOf(progress * 100 / total) + "%");
            mNotificationManager.notify(mNotificationId, mNotification.build());
        }
    }
}