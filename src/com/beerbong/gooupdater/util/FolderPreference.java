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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.Preference;
import android.view.View;
import android.widget.Toast;

import com.beerbong.gooupdater.activity.GooActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.manager.PreferencesManager;

public class FolderPreference extends Preference implements View.OnLongClickListener {

    private GooActivity mActivity;
    private String mFolder;
    private boolean mWatchlist;

    public FolderPreference(GooActivity activity, String folder, boolean watchlist) {
        super(activity);
        mActivity = activity;
        mFolder = folder;
        mWatchlist = watchlist;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mWatchlist) {
            removeWatchlist();
        } else {
            addWatchlist();
        }
        return true;
    }

    private void addWatchlist() {
        mActivity.runOnUiThread(new Runnable() {

            public void run() {
                new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.watchlist_add_confirm_title)
                        .setMessage(R.string.watchlist_add_confirm)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();

                                        PreferencesManager pManager = ManagerFactory
                                                .getPreferencesManager();
                                        String str = pManager.getWatchlist();
                                        if (str.indexOf(mFolder) < 0) {
                                            if ("".equals(str)) {
                                                str = mFolder;
                                            } else {
                                                str += PreferencesManager.WATCHLIST_SEPARATOR + mFolder;
                                            }
                                            pManager.setWatchlist(str);
                                            Toast.makeText(mActivity, R.string.watchlist_added,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(mActivity, R.string.watchlist_already,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                        .setNeutralButton(R.string.gapps_folder_select,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ManagerFactory.getPreferencesManager().setGappsFolder(mFolder);
                                        Toast.makeText(mActivity, R.string.gapps_folder_selected,
                                                Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
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

    private void removeWatchlist() {
        mActivity.runOnUiThread(new Runnable() {

            public void run() {
                new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.watchlist_remove_confirm_title)
                        .setMessage(R.string.watchlist_remove_confirm)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        PreferencesManager pManager = ManagerFactory
                                                .getPreferencesManager();
                                        String str = pManager.getWatchlist();
                                        if (str.indexOf(mFolder) >= 0) {
                                            if (str.equals(mFolder)) {
                                                str = "";
                                            } else {
                                                str = str.replace(PreferencesManager.WATCHLIST_SEPARATOR + mFolder, "");
                                                str = str.replace(mFolder + PreferencesManager.WATCHLIST_SEPARATOR, "");
                                            }
                                            pManager.setWatchlist(str);
                                            Toast.makeText(mActivity, R.string.watchlist_removed,
                                                    Toast.LENGTH_LONG).show();
                                            mActivity.refreshWatchlist();
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
        });
    }
}
