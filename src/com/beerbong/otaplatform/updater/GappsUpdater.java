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

package com.beerbong.otaplatform.updater;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.URLStringReader;

public class GappsUpdater extends Updater implements Updater.UpdaterListener {

    public interface GappsUpdaterListener {

        public void checkGappsCompleted(long newVersion);
    }

    private Context mContext;
    private GappsUpdaterListener mListener;
    private String mId;
    private String mName;
    private String mPlatform;
    private int mVersion = -1;
    private boolean mFromAlarm;
    private boolean mCanUpdate;
    private boolean mScanning;
    private boolean mCustomGapps;

    public GappsUpdater(Context context, GappsUpdaterListener listener, boolean fromAlarm) {
        mContext = context;
        mListener = listener;
        mFromAlarm = fromAlarm;

        File file = new File("/system/etc/g.prop");
        mCanUpdate = file.exists();
        if (mCanUpdate) {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(file));
                String versionProperty = Constants.getProperty(Constants.OVERLAY_GAPPS_VERSION);
                String versionString = properties.getProperty(versionProperty);
                if (versionString == null || "".equals(versionString) || versionProperty == null
                        || "".equals(versionProperty)) {
                    versionProperty = "ro.addon.version";
                    versionString = properties.getProperty(versionProperty);
                }
                mId = properties.getProperty("ro.addon.type");
                mName = properties.getProperty("ro.addon.version");
                mPlatform = properties.getProperty("ro.addon.platform");
                if (versionString == null || "".equals(versionString)) {
                    mCanUpdate = false;
                } else {
                    String[] version = versionString.split("-");
                    for (int i=0;i<version.length;i++) {
                        try {
                            mVersion = Integer.parseInt(version[i]);
                            break;
                        } catch (NumberFormatException ex) {
                            // ignore
                        }
                    }
                }
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
                info = new GooPackage(null, -1);
            } else {
                if (!mCustomGapps) {
                    JSONObject result = (JSONObject) new JSONTokener(buffer).nextValue();
                    info = new GooPackage(result, -1);
                } else {
                    JSONObject object = (JSONObject) new JSONTokener(buffer).nextValue();
                    info = new GooPackage(null, -1);
                    if (!object.isNull("list")) {
                        JSONArray list = object.getJSONArray("list");
                        for (int i=0;i<list.length();i++) {
                            object = list.getJSONObject(i);
                            if (!object.isNull("path")) {
                                info = new GooPackage(object, -1);
                                break;
                            }
                        }
                    }
                }
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
        if (!mFromAlarm) {
            Constants.showToastOnUiThread(mContext, R.string.check_gapps_updates_error);
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkGappsCompleted(-1);
                }
            });
        }
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
        String folder = ManagerFactory.getPreferencesManager(mContext).getGappsFolder();
        if (folder == null || "".equals(folder)) {
            mCustomGapps = false;
            new URLStringReader(this).execute("http://goo.im/json2&action=gapps_update&gapps_platform="
                    + getPlatform() + "&gapps_addon_version=" + getVersion());
        } else {
            mCustomGapps = true;
            if (folder.startsWith("http://goo.im/")) {
                folder = Constants.GOO_SEARCH_URL + folder.substring(folder.indexOf("/devs"));
            }
            new URLStringReader(this).execute(folder);
        }
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    @Override
    public void versionFound(final PackageInfo info) {
        mScanning = false;
        if (info != null && info.getVersion() > mVersion) {
            if (!mFromAlarm) {
                if (mCustomGapps) {
                    showLastGappsFound(info);
                } else {
                    showNewGappsFound(info);
                }
            } else {
                Constants.showNotification(mContext, info,
                        Constants.NEWGAPPSVERSION_NOTIFICATION_ID,
                        R.string.new_gapps_found_title, R.string.new_package_name);
            }
        } else {
            if (!mFromAlarm) {
                Constants.showToastOnUiThread(mContext, R.string.check_gapps_updates_no_new);
            }
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkGappsCompleted(info.getVersion());
                }
            });
        }
    }

    @Override
    public void versionError(String error) {
        mScanning = false;
        if (!mFromAlarm) {
            if (error != null) {
                Constants.showToastOnUiThread(mContext,
                        mContext.getResources().getString(R.string.check_gapps_updates_error)
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
                                    mContext.getResources().getString(
                                            R.string.new_gapps_found_summary,
                                            new Object[] { info.getFilename(), info.getFolder() }))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();

                                            ((Activity) mContext).runOnUiThread(new Runnable() {

                                                public void run() {
                                                    ManagerFactory
                                                            .getFileManager(mContext)
                                                            .download(
                                                                    mContext,
                                                                    info.getPath(),
                                                                    info.getFilename(),
                                                                    info.getMd5(),
                                                                    false,
                                                                    Constants.DOWNLOADGAPPS_NOTIFICATION_ID);
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

    private void showLastGappsFound(final PackageInfo info) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                try {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.last_gapps_found_title)
                            .setMessage(
                                    mContext.getResources().getString(
                                            R.string.last_gapps_found_summary,
                                            new Object[] { info.getFilename(), mVersion }))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();

                                            ((Activity) mContext).runOnUiThread(new Runnable() {

                                                public void run() {
                                                    ManagerFactory
                                                            .getFileManager(mContext)
                                                            .download(
                                                                    mContext,
                                                                    info.getPath(),
                                                                    info.getFilename(),
                                                                    info.getMd5(),
                                                                    false,
                                                                    Constants.DOWNLOADGAPPS_NOTIFICATION_ID);
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
