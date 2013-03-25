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
import android.content.Context;
import android.content.DialogInterface;

import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.updater.Updater.PackageInfo;
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

        public void checkRomCompleted(long newVersion);
    }

    public RomUpdater(Context context, RomUpdaterListener listener, boolean fromService) {

        mContext = context;
        mFromService = fromService;

        mUpdater = getUpdater();

        mListener = listener;

        mRomName = mUpdater.getName();
        mRomVersion = mUpdater.getVersion();
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
        return mUpdater.getName();
    }

    public int getRomVersion() {
        return mUpdater.getVersion();
    }

    @Override
    public void versionFound(final PackageInfo info) {
        if (info != null && info.version > mRomVersion) {
            if (!mFromService) {
                showNewRomFound(info);
            } else {
                if (ManagerFactory.getPreferencesManager().isAcceptNotifications()) {
                    Constants.showNotification(mContext, info,
                            Constants.NEWROMVERSION_NOTIFICATION_ID, R.string.new_rom_found_title,
                            R.string.new_package_name);
                }
            }
        } else {
            if (!mFromService) {
                Constants.showToastOnUiThread(mContext, R.string.check_rom_updates_no_new);
            }
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkRomCompleted(info == null ? -1 : info.version);
                }
            });
        }
    }

    @Override
    public void versionError(String error) {
        if (!mFromService) {
            if (error != null) {
                Constants.showToastOnUiThread(mContext,
                        mContext.getResources().getString(R.string.check_rom_updates_error) + ": "
                                + error);
            } else {
                Constants.showToastOnUiThread(mContext, R.string.check_rom_updates_error);
            }
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkRomCompleted(-1);
                }
            });
        }
    }

    private void showNewRomFound(final PackageInfo info) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                try {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.new_rom_found_title)
                            .setMessage(
                                    mContext.getResources().getString(
                                            R.string.new_rom_found_summary,
                                            new Object[] { info.filename, info.folder }))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();

                                            ((Activity) mContext).runOnUiThread(new Runnable() {

                                                public void run() {
                                                    ManagerFactory.getFileManager().download(
                                                            mContext, info.path, info.filename,
                                                            info.md5,
                                                            Constants.DOWNLOADROM_NOTIFICATION_ID);
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
                } catch (Exception ex) {
                    // app closed?
                }
            }
        });
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