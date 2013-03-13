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
import com.beerbong.gooupdater.updater.RomUpdater;
import com.beerbong.gooupdater.util.Constants;

public class UIImpl extends UI implements RomUpdater.RomUpdaterListener {

    private Activity mActivity;
    private RomUpdater mUpdater;
    private ProgressDialog mProgress;
    private Button mButtonCheck;
    private boolean mCanUpdate;

    protected UIImpl(Activity activity) {

        redraw(activity);
    }

    @Override
    public void redraw(Activity activity) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(activity).isDarkTheme();
        activity.setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        mActivity = activity;

        mActivity.setContentView(R.layout.main_activity);

        mUpdater = new RomUpdater(mActivity, this, false);

        mCanUpdate = mUpdater.canUpdate();

        TextView romHeader = (TextView) mActivity.findViewById(R.id.rom_header);
        romHeader.setText(mCanUpdate ? mUpdater.getRomName() : mActivity.getResources().getString(
                R.string.not_available));

        TextView devHeader = (TextView) mActivity.findViewById(R.id.developer_header);
        devHeader.setText(mCanUpdate ? mUpdater.getDeveloperId() : mActivity.getResources()
                .getString(R.string.not_available));

        TextView versionHeader = (TextView) mActivity.findViewById(R.id.version_header);
        versionHeader.setText(mCanUpdate ? String.valueOf(mUpdater.getRomVersion()) : mActivity
                .getResources().getString(R.string.not_available));

        mButtonCheck = (Button) mActivity.findViewById(R.id.button_checkupdates);
        mButtonCheck.setEnabled(mCanUpdate);
        mButtonCheck.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                check(true);
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

        Intent intent = activity.getIntent();

        if (intent.getExtras() != null && intent.getExtras().containsKey("NOTIFICATION_ID")) {
            if (Integer.parseInt(intent.getExtras().get("NOTIFICATION_ID").toString()) == Constants.NEWVERSION_NOTIFICATION_ID) {
                String url = intent.getExtras().getString("URL");
                String md5 = intent.getStringExtra("MD5");
                String name = intent.getStringExtra("ZIP_NAME");

                ManagerFactory.getFileManager(activity).download(activity, url, name, md5);
            } else if (Integer.parseInt(intent.getExtras().get("NOTIFICATION_ID").toString()) == Constants.DOWNLOAD_NOTIFICATION_ID) {
                ManagerFactory.getFileManager().cancelDownload();
            }
        } else {
            check(false);
        }
    }

    private void check(boolean showProgress) {
        if (showProgress) {
            mProgress = ProgressDialog.show(mActivity, null,
                    mActivity.getResources().getString(R.string.checking), true, false);
        } else {
            TextView remoteVersionHeader = (TextView) mActivity
                    .findViewById(R.id.remoteversion_header);
            remoteVersionHeader.setText(R.string.checking);
            mButtonCheck.setEnabled(false);
        }
        mUpdater.check();
    }

    @Override
    public void checkCompleted(long newVersion) {
        if (mProgress != null) {
            mProgress.dismiss();
        }

        TextView remoteVersionHeader = (TextView) mActivity.findViewById(R.id.remoteversion_header);
        remoteVersionHeader.setText(String.valueOf(newVersion));
        
        mButtonCheck.setEnabled(mCanUpdate);
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
