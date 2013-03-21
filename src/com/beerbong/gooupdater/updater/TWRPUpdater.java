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

import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.util.Constants;
import com.beerbong.gooupdater.util.HttpStringReader;
import com.beerbong.gooupdater.util.DownloadTask.DownloadTaskListener;
import com.beerbong.gooupdater.util.HttpStringReader.HttpStringReaderListener;
import com.beerbong.gooupdater.util.URLStringReader;

public class TWRPUpdater implements Updater, Updater.UpdaterListener {

    public interface TWRPUpdaterListener {

        public void checkTWRPCompleted(long newVersion);
    }

    private Context mContext;
    private TWRPUpdaterListener mListener;
    private String mBoard;
    private String mInstallCommand;
    private boolean mScanning;

    public TWRPUpdater(Context context, TWRPUpdaterListener listener) {
        mContext = context;
        mListener = listener;

        mBoard = Constants.getProperty(PROPERTY_DEVICE);
    }

    @Override
    public void onReadEnd(String buffer) {
        mScanning = false;
        try {
            GooPackage info = null;
            if (!"false".equals(buffer)) {
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
        Constants.showToastOnUiThread(mContext, R.string.check_twrp_updates_error);
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkTWRPCompleted(-1);
                }
            });
        }
    }

    @Override
    public String getDeveloperId() {
        return "OpenRecovery";
    }

    @Override
    public String getName() {
        return "TWRP";
    }

    @Override
    public int getVersion() {
        return 0;
    }

    public void check() {
        if (isScanning()) {
            return;
        }
        searchVersion();
    }

    public void installTWRP(final File file) {
        if (mInstallCommand == null) {
            new HttpStringReader(new HttpStringReaderListener() {

                @Override
                public void onReadEnd(String buffer) {
                    if (buffer == null || "".equals(buffer) || "false".equals(buffer)) {
                        mInstallCommand = null;
                    } else {
                        mInstallCommand = buffer;
                    }
                    reallyInstallTWRP(file);
                }

                @Override
                public void onReadError(Exception ex) {
                    mInstallCommand = null;
                }

            }).execute(new String[] { "http://goo.im/json2&action=get_install_command&ro_board="
                    + mBoard });
        } else {
            reallyInstallTWRP(file);
        }
    }

    private void reallyInstallTWRP(File file) {
        String path = file.getAbsolutePath();
        if (path.endsWith(".img")) {
            if (mInstallCommand != null) {
                String command = mInstallCommand.replace("#FILE#", path);
                ManagerFactory.getSUManager().execute(command);
            } else {
                // TODO flash image throug recovery
            }
        } else if (path.endsWith(".zip")) {
            // TODO install zip throug recovery
        }
    }

    @Override
    public void searchVersion() {
        mScanning = true;
        new URLStringReader(this).execute("http://goo.im/json2&action=recovery&ro_board=" + mBoard);
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    @Override
    public void versionFound(final PackageInfo info) {
        mScanning = false;
        if (info != null) {
            showNewTWRPFound(info);
        } else {
            Constants.showToastOnUiThread(mContext, R.string.check_twrp_updates_no_new);
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkTWRPCompleted(info == null ? -1 : info.version);
                }
            });
        }
    }

    @Override
    public void versionError(String error) {
        mScanning = false;
        if (error != null) {
            Constants.showToastOnUiThread(mContext,
                    mContext.getResources().getString(R.string.check_twrp_updates_error) + ": "
                            + error);
        } else {
            Constants.showToastOnUiThread(mContext, R.string.check_twrp_updates_error);
        }
        if (mListener != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {

                public void run() {
                    mListener.checkTWRPCompleted(-1);
                }
            });
        }
    }

    private void showNewTWRPFound(final PackageInfo info) {
        ((Activity) mContext).runOnUiThread(new Runnable() {

            public void run() {
                try {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.new_twrp_found_title)
                            .setMessage(
                                    mContext.getResources().getString(
                                            R.string.new_twrp_found_summary,
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
                                                            Constants.DOWNLOADTWRP_NOTIFICATION_ID, new DownloadTaskListener() {

                                                                @Override
                                                                public void downloadComplete(int status, File file) {

                                                                    if (status == 0) {
                                                                        String fileMd5 = Constants.md5(file);
                                                                        if (!fileMd5.equals(info.md5)) {
                                                                            Constants.showToastOnUiThread(mContext, R.string.check_twrp_error_md5);
                                                                            return;
                                                                        }
                                                                        installTWRP(file);
                                                                    }
                                                                }
                                                                
                                                            });
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
