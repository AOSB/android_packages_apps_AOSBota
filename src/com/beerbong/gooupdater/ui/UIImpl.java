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

package com.beerbong.gooupdater.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.beerbong.gooupdater.GooActivity;
import com.beerbong.gooupdater.R;
import com.beerbong.gooupdater.SettingsActivity;
import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.updater.GappsUpdater;
import com.beerbong.gooupdater.updater.RomUpdater;
import com.beerbong.gooupdater.util.Constants;

public class UIImpl extends UI implements RomUpdater.RomUpdaterListener,
        GappsUpdater.GappsUpdaterListener {

    private static long mNewRomVersion = -1L;

    private Activity mActivity;
    private RomUpdater mRomUpdater;
    private GappsUpdater mGappsUpdater;
    private ProgressDialog mProgressRom;
    private ProgressDialog mProgressGapps;
    private TextView mRemoteVersionHeader;
    private Button mButtonCheckRom;
    private Button mButtonCheckGapps;

    protected UIImpl(Activity activity) {

        redraw(activity);
    }

    @Override
    public void redraw(Activity activity) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(activity).isDarkTheme();
        activity.setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        mActivity = activity;

        mActivity.setContentView(R.layout.main_activity);

        mRomUpdater = new RomUpdater(mActivity, this, false);

        mGappsUpdater = new GappsUpdater(mActivity, this, false);

        boolean romCanUpdate = mRomUpdater.canUpdate();

        TextView romHeader = (TextView) mActivity.findViewById(R.id.rom_header);
        romHeader.setText(romCanUpdate ? mRomUpdater.getRomName() : mActivity.getResources()
                .getString(R.string.not_available));

        TextView devHeader = (TextView) mActivity.findViewById(R.id.developer_header);
        devHeader.setText(romCanUpdate ? mRomUpdater.getDeveloperId() : mActivity.getResources()
                .getString(R.string.not_available));

        TextView versionHeader = (TextView) mActivity.findViewById(R.id.version_header);
        versionHeader.setText(romCanUpdate ? String.valueOf(mRomUpdater.getRomVersion())
                : mActivity.getResources().getString(R.string.not_available));

        mRemoteVersionHeader = (TextView) mActivity.findViewById(R.id.remoteversion_header);
        if (mNewRomVersion >= 0) {
            mRemoteVersionHeader.setText(String.valueOf(mNewRomVersion));
        }

        mButtonCheckRom = (Button) mActivity.findViewById(R.id.button_checkupdates);
        mButtonCheckRom.setEnabled(romCanUpdate);
        mButtonCheckRom.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkRom(true);
            }
        });

        mButtonCheckGapps = (Button) mActivity.findViewById(R.id.button_checkupdatesgapps);
        mButtonCheckGapps.setEnabled(mGappsUpdater.canUpdate());
        mButtonCheckGapps.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkGapps(true);
            }
        });

        Button buttonGoo = (Button) mActivity.findViewById(R.id.button_browse);
        buttonGoo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                GooActivity.CURRENT_NAVIGATION = null;
                mActivity.startActivity(new Intent(mActivity, GooActivity.class));
            }
        });
    }

    private void checkRom(boolean showProgress) {
        if (showProgress) {
            mProgressRom = ProgressDialog.show(mActivity, null,
                    mActivity.getResources().getString(R.string.checking), true, false);
        } else {
            mRemoteVersionHeader.setText(R.string.checking);
            mButtonCheckRom.setEnabled(false);
        }
        mRomUpdater.check();
    }

    private void checkGapps(boolean showProgress) {
        if (showProgress) {
            mProgressGapps = ProgressDialog.show(mActivity, null, mActivity.getResources()
                    .getString(R.string.checking), true, false);
        } else {
            mButtonCheckGapps.setEnabled(false);
        }
        mGappsUpdater.check();
    }

    @Override
    public void onNewIntent(Context context, Intent intent) {

        int notificationId = Integer.parseInt(intent.getExtras().get("NOTIFICATION_ID").toString());
        if (notificationId == Constants.NEWROMVERSION_NOTIFICATION_ID
                || notificationId == Constants.NEWGAPPSVERSION_NOTIFICATION_ID) {
            String url = intent.getExtras().getString("URL");
            String md5 = intent.getStringExtra("MD5");
            String name = intent.getStringExtra("ZIP_NAME");

            if (notificationId == Constants.NEWROMVERSION_NOTIFICATION_ID) {
                notificationId = Constants.DOWNLOADROM_NOTIFICATION_ID;
            } else {
                notificationId = Constants.DOWNLOADGAPPS_NOTIFICATION_ID;
            }
            ManagerFactory.getFileManager(context)
                    .download(context, url, name, md5, notificationId);
        } else if (notificationId == Constants.DOWNLOADROM_NOTIFICATION_ID
                || notificationId == Constants.DOWNLOADGAPPS_NOTIFICATION_ID) {
            ManagerFactory.getFileManager().cancelDownload(notificationId);
        }

    }

    @Override
    public void checkRomCompleted(long newVersion) {
        if (mProgressRom != null) {
            mProgressRom.dismiss();
            mProgressRom = null;
        }

        mNewRomVersion = newVersion;

        mRemoteVersionHeader.setText(String.valueOf(mNewRomVersion));

        mButtonCheckRom.setEnabled(mRomUpdater.canUpdate());
    }

    @Override
    public void checkGappsCompleted(long newVersion) {
        if (mProgressGapps != null) {
            mProgressGapps.dismiss();
            mProgressGapps = null;
        }

        mButtonCheckGapps.setEnabled(mGappsUpdater.canUpdate());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.layout.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                mActivity.startActivity(new Intent(mActivity, SettingsActivity.class));
                break;
        }

        return true;
    }
}
