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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.util.Constants;
import com.beerbong.gooupdater.util.URLStringReader;

public class GappsUpdater implements Updater, Updater.UpdaterListener {

    public interface GappsUpdaterListener {

        public void checkGappsCompleted(long newVersion);
    }

    private Context mContext;
    private GappsUpdaterListener mListener;
    private String mId;
    private String mName;
    private String mPlatform;
    private int mVersion;
    private boolean mFromService;
    private boolean mCanUpdate;
    private boolean mScanning;

    public GappsUpdater(Context context, GappsUpdaterListener listener, boolean fromService) {
        mContext = context;
        mListener = listener;
        mFromService = fromService;
        
        File file = new File("/system/etc/g.prop");
        mCanUpdate = file.exists();
        if (mCanUpdate) {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(file));
                mId = properties.getProperty("ro.addon.type");
                mName = properties.getProperty("ro.addon.version");
                mPlatform = properties.getProperty("ro.addon.platform");
                String version = properties.getProperty("ro.addon.version");
                version = version.substring(version.lastIndexOf("-") + 1);
                mVersion = Integer.parseInt(version);
            } catch (Exception ex) {
                ex.printStackTrace();
                mCanUpdate = false;
            }
        }
    }

    public boolean canUpdate() {
        return mCanUpdate;
    }

    @Override
    public void onReadEnd(String buffer) {
        mScanning = false;
        try {
            GooPackage info;
            if ("false".equals(buffer)) {
                info = new GooPackage(null);
            } else {
                JSONObject result = (JSONObject) new JSONTokener(buffer).nextValue();
                info = new GooPackage(result);
            }
            
            versionFound(info);
        } catch (Exception ex) {
            System.out.println(buffer);
            ex.printStackTrace();
            versionError(null);
        }
    }

    @Override
    public void onReadError(Exception ex) {
    }

    @Override
    public String getDeveloperId() {
        return mId;
    }

    @Override
    public String getName() {
        return mName;
    }

    public String getPlatform() {
        return mPlatform;
    }

    @Override
    public int getVersion() {
        return mVersion;
    }

    public void check() {
        if (!canUpdate() || isScanning()) {
            return;
        }
        searchVersion();
    }

    @Override
    public void searchVersion() {
        mScanning = true;
        new URLStringReader(this).execute("http://goo.im/json2&action=gapps_update&gapps_platform="
                + getPlatform() + "&gapps_addon_version=" + getVersion());
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    @Override
    public void versionFound(final PackageInfo info) {
        mScanning = false;
        if (info != null && info.version > mVersion) {
            if (!mFromService) {
                showNewGappsFound(info);
            } else {
                if (ManagerFactory.getPreferencesManager().isAcceptNotifications()) {
                    Constants.showNotification(mContext, info,
                            Constants.NEWGAPPSVERSION_NOTIFICATION_ID, R.string.new_gapps_found_title,
                            R.string.new_package_name);
                }
            }
        } else {
            if (!mFromService) {
                Constants.showToastOnUiThread(mContext, R.string.check_gapps_updates_no_new);
            }
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkGappsCompleted(info.version);
                }
            });
        }
    }

    @Override
    public void versionError(String error) {
        mScanning = false;
        if (!mFromService) {
            if (error != null) {
                Constants.showToastOnUiThread(mContext, mContext.getResources().getString(
                        R.string.check_gapps_updates_error)
                        + ": " + error);
            } else {
                Constants.showToastOnUiThread(mContext, R.string.check_gapps_updates_error);
            }
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkGappsCompleted(-1);
                }
            });
        }
    }

    private void showNewGappsFound(final PackageInfo info) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                try {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.new_gapps_found_title)
                            .setMessage(
                                    mContext.getResources().getString(R.string.new_gapps_found_summary,
                                            new Object[] { info.filename, info.folder }))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
    
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();
    
                                            ((Activity) mContext).runOnUiThread(new Runnable() {
    
                                                public void run() {
                                                    ManagerFactory.getFileManager().download(mContext,
                                                            info.path, info.filename, info.md5, Constants.DOWNLOADGAPPS_NOTIFICATION_ID);
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

}
