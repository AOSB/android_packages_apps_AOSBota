package com.beerbong.otaplatform;

import java.io.File;
import java.io.Serializable;

import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.DownloadTask;
import com.beerbong.otaplatform.util.DownloadTask.DownloadStatus;
import com.beerbong.otaplatform.util.DownloadTask.DownloadTaskListener;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.os.IBinder;
import android.os.PowerManager;

public class DownloadService extends Service implements DownloadTaskListener {

    public static class FileInfo implements Serializable {

        public int notificationId;
        public String url;
        public String fileName;
        public String path;
        public String md5;
        public boolean isDelta;
        public Status status = Status.PENDING;
        public DownloadStatus downloadStatus;
        public Updater.PackageInfo packageInfo;
        public File file;
    };

    private PendingIntent mPendingIntent;
    private Intent mIntent;
    private DownloadTask mDownloadRom;
    private DownloadTask mDownloadGapps;
    private DownloadTask mDownloadTWRP;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OTAPlatform");
        mWakeLock.acquire();

        if (intent == null || intent.getExtras() == null) {
            return START_STICKY;
        }

        Resources resources = getResources();

        FileInfo info = (FileInfo) intent.getExtras().get(Constants.FILE_INFO);

        mIntent = new Intent(this, MainActivity.class);
        info.status = Status.RUNNING;
        mIntent.putExtra(Constants.FILE_INFO, info);
        mPendingIntent = PendingIntent.getActivity(this, info.notificationId, mIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notification = new Notification.Builder(this)
                .setContentTitle(resources.getString(R.string.downloading))
                .setContentText(
                        resources.getString(R.string.new_package_name,
                                new Object[] { info.fileName }))
                .setSmallIcon(R.drawable.ic_launcher).setContentIntent(mPendingIntent)
                .setProgress(100, 0, true);

        switch (info.notificationId) {
            case Constants.DOWNLOADROM_NOTIFICATION_ID:
                mDownloadRom = new DownloadTask(notification, info.notificationId, this, info.url,
                        info.fileName, info.md5, info.isDelta);
                info.fileName = mDownloadRom.getDestinationFile().getName();
                info.path = mDownloadRom.getDestinationFile().getAbsolutePath();
                mIntent.putExtra(Constants.FILE_INFO, info);
                mPendingIntent = PendingIntent.getActivity(this, info.notificationId, mIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                mDownloadRom.setDownloadTaskListener(this);
                mDownloadRom.execute();
                break;
            case Constants.DOWNLOADGAPPS_NOTIFICATION_ID:
                mDownloadGapps = new DownloadTask(notification, info.notificationId, this,
                        info.url, info.fileName, info.md5, info.isDelta);
                info.fileName = mDownloadGapps.getDestinationFile().getName();
                info.path = mDownloadGapps.getDestinationFile().getAbsolutePath();
                mIntent.putExtra(Constants.FILE_INFO, info);
                mPendingIntent = PendingIntent.getActivity(this, info.notificationId, mIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                mDownloadGapps.setDownloadTaskListener(this);
                mDownloadGapps.execute();
                break;
            case Constants.DOWNLOADTWRP_NOTIFICATION_ID:
                notification.setAutoCancel(true);
                mDownloadTWRP = new DownloadTask(null, info.notificationId, this, info.url,
                        info.fileName, info.md5, false);
                mDownloadTWRP.setDownloadTaskListener(this);
                mDownloadTWRP.attach(notification, info.notificationId, this);
                mDownloadTWRP.execute();
                break;
        }

        return START_STICKY;
    }

    @Override
    public void downloadComplete(DownloadStatus status, File file) {
        FileInfo info = (FileInfo) mIntent.getExtras().get(Constants.FILE_INFO);
        info.status = Status.FINISHED;
        info.downloadStatus = status;
        info.file = file;
        mIntent.putExtra(Constants.FILE_INFO, info);
        mPendingIntent = PendingIntent.getActivity(this, info.notificationId, mIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mDownloadRom != null) {
            mDownloadRom.cancel(true);
        }
        if (mDownloadGapps != null) {
            mDownloadGapps.cancel(true);
        }
        if (mDownloadTWRP != null) {
            mDownloadTWRP.cancel(true);
        }
        mWakeLock.release();
        super.onDestroy();
    }
}
