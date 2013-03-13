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

package com.beerbong.gooupdater.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.Toast;

import com.beerbong.gooupdater.MainActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.Service;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.updater.Updater.RomInfo;
import com.beerbong.gooupdater.updater.impl.GooUpdater;
import com.beerbong.gooupdater.util.Constants;

public class RomUpdater implements Updater.UpdaterListener {

    private Context mContext;
    private Updater mUpdater;
    private RomUpdaterListener mListener;
    private String mRomName;
    private int mRomVersion = -1;
    private boolean mFromService;

    public interface RomUpdaterListener {

        public void checkCompleted(long newVersion);
    }

    public RomUpdater(Context context, RomUpdaterListener listener, boolean fromService) {

        mContext = context;
        mFromService = fromService;

        mUpdater = getUpdater();

        mListener = listener;

        mRomName = mUpdater.getRomName();
        mRomVersion = mUpdater.getRomVersion();
    }

    public boolean canUpdate() {
        if (mRomName != null && mRomVersion > 0) {
            return true;
        }
        return false;
    }

    public void check() {
        if (!canUpdate() || mUpdater.isScanning()) {
            return;
        }
        mUpdater.searchVersion();
    }

    public String getDeveloperId() {
        return mUpdater.getDeveloperId();
    }

    public String getRomName() {
        return mUpdater.getRomName();
    }

    public int getRomVersion() {
        return mUpdater.getRomVersion();
    }

    @Override
    public void versionFound(final RomInfo info) {
        if (info != null && info.version > mRomVersion) {
            if (!mFromService) {
                showNewRomFound(info);
            } else {
                if (ManagerFactory.getPreferencesManager().isAcceptNotifications()) {
                    showNotification(info);
                }
            }
        } else {
            if (!mFromService) {
                showToastOnUiThread(R.string.check_rom_updates_no_new);
            }
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkCompleted(info.version);
                }
            });
        }
    }

    @Override
    public void versionError(String error) {
        if (!mFromService) {
            if (error != null) {
                showToastOnUiThread(mContext.getResources().getString(
                        R.string.check_rom_updates_error)
                        + ": " + error);
            } else {
                showToastOnUiThread(R.string.check_rom_updates_error);
            }
        }
    }

    private void showToastOnUiThread(final int resourceId) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(mContext, resourceId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showToastOnUiThread(final String string) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(mContext, string, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showNewRomFound(final RomInfo info) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.new_rom_found_title)
                        .setMessage(
                                mContext.getResources().getString(R.string.new_rom_found_summary,
                                        new Object[] { info.filename, info.folder }))
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();

                                        ((Activity) mContext).runOnUiThread(new Runnable() {

                                            public void run() {
                                                ManagerFactory.getFileManager().download(mContext,
                                                        info.path, info.filename, info.md5);
                                            }
                                        });
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                }).show();
            }
        });
    }

    private void showNotification(RomInfo info) {
        Resources resources = mContext.getResources();

        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra("NOTIFICATION_ID", Constants.NEWVERSION_NOTIFICATION_ID);
        intent.putExtra("URL", info.path);
        intent.putExtra("ZIP_NAME", info.filename);
        intent.putExtra("MD5", info.md5);
        PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification noti = new Notification.Builder(mContext)
                .setContentTitle(resources.getString(R.string.new_rom_found_title))
                .setContentText(
                        resources.getString(R.string.new_rom_name, new Object[] { info.filename }))
                .setSmallIcon(R.drawable.ic_launcher).setContentIntent(pIntent).build();

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Service.NOTIFICATION_SERVICE);

        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(Constants.NEWVERSION_NOTIFICATION_ID, noti);
    }

    private Updater getUpdater() {
        if (Constants.getProperty(GooUpdater.PROPERTY_GOO_DEVELOPER) != null
                && Constants.getProperty(GooUpdater.PROPERTY_GOO_ROM) != null
                && Constants.getProperty(GooUpdater.PROPERTY_GOO_VERSION) != null) {
            return new GooUpdater(this);
        }
        return null;
    }
}