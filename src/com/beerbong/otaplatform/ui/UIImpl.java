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

package com.beerbong.otaplatform.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.activity.FlashActivity;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.manager.PreferencesManager;
import com.beerbong.otaplatform.updater.CancelPackage;
import com.beerbong.otaplatform.updater.GappsUpdater;
import com.beerbong.otaplatform.updater.RomUpdater;
import com.beerbong.otaplatform.updater.TWRPUpdater;
import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.updater.Updater.PackageInfo;
import com.beerbong.otaplatform.util.Constants;

public class UIImpl extends UI implements RomUpdater.RomUpdaterListener,
        GappsUpdater.GappsUpdaterListener, TWRPUpdater.TWRPUpdaterListener {

    private static PackageInfo mNewRom = null;
    private static boolean mReCheck = true;

    private Activity mActivity;
    private OnNewIntentListener mOnNewIntentListener;
    private RomUpdater mRomUpdater;
    private GappsUpdater mGappsUpdater;
    private ProgressDialog mProgress;
    private TextView mRemoteVersionHeader;
    private TextView mRemoteVersionBody;
    private Button mButtonCheckRom;
    private Button mButtonCheckGapps;
    private Button mButtonFlashQueue;
    private Button mButtonDownload;
    private boolean mRomCanUpdate = true;
    private boolean mShowProgress = true;

    protected UIImpl(Activity activity) {

        redraw(activity);
    }

    @Override
    public void redraw(Activity activity) {

        PreferencesManager pManager = ManagerFactory.getPreferencesManager(activity);

        setOnNewIntentListener(ManagerFactory.getFileManager(activity));

        boolean useDarkTheme = pManager.isDarkTheme();
        activity.setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        mActivity = activity;

        TextView romHeader = null;
        TextView devHeader = null;
        TextView versionHeader = null;
        ImageView updaterImage = null;

        mActivity.setContentView(R.layout.main);

        mRomUpdater = Updater.getRomUpdater(mActivity, this, false);

        mGappsUpdater = new GappsUpdater(mActivity, this, false);

        mRomCanUpdate = mRomUpdater != null && mRomUpdater.canUpdate();

        romHeader = (TextView) mActivity.findViewById(R.id.rom_header);
        devHeader = (TextView) mActivity.findViewById(R.id.developer_header);
        versionHeader = (TextView) mActivity.findViewById(R.id.version_header);
        updaterImage = (ImageView) mActivity.findViewById(R.id.updaterImage);
        mRemoteVersionHeader = (TextView) mActivity.findViewById(R.id.remoteversion_header);
        mRemoteVersionBody = (TextView) mActivity.findViewById(R.id.remoteversion_body);
        mButtonCheckRom = (Button) mActivity.findViewById(R.id.button_checkupdates);
        mButtonCheckGapps = (Button) mActivity.findViewById(R.id.button_checkupdatesgapps);
        mButtonFlashQueue = (Button) mActivity.findViewById(R.id.button_flashqueue);
        mButtonDownload = (Button) mActivity.findViewById(R.id.button_download);

        Resources res = mActivity.getResources();

        romHeader.setText(mRomCanUpdate ? mRomUpdater.getRomName() : res
                .getString(R.string.not_available));

        devHeader.setText(mRomCanUpdate ? mRomUpdater.getDeveloperId() : res
                .getString(R.string.not_available));

        versionHeader.setText(mRomCanUpdate ? String.valueOf(mRomUpdater.getRomVersion())
                : res.getString(R.string.not_available));

        updaterImage.setImageDrawable(mRomCanUpdate ? res.getDrawable(mRomUpdater.getDrawable())
                : res.getDrawable(R.drawable.ic_launcher_goo));

        mRemoteVersionBody.setMovementMethod(new ScrollingMovementMethod());

        mButtonCheckRom.setEnabled(mRomCanUpdate);
        mButtonCheckRom.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkRom();
            }
        });

        mButtonCheckGapps.setEnabled(mGappsUpdater.canUpdate());
        mButtonCheckGapps.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                checkGapps();
            }
        });

        mButtonFlashQueue.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mActivity.startActivity(new Intent(mActivity, FlashActivity.class));
            }
        });
        updateFlashQueueText();

        mButtonDownload.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ManagerFactory.getFileManager(mActivity).download(
                        mActivity, mNewRom.getPath(), mNewRom.getFilename(),
                        mNewRom.getMd5(),
                        Constants.DOWNLOADROM_NOTIFICATION_ID);
            }
        });

        Intent intent = activity.getIntent();
        if (intent != null && intent.getExtras() != null
                && intent.getExtras().get("NOTIFICATION_ID") != null) {
            if (mOnNewIntentListener != null) {
                PackageInfo info = mOnNewIntentListener.onNewIntent(activity, intent);
                if (info instanceof CancelPackage) {
                    mShowProgress = false;
                    mReCheck = true;
                } else {
                    mNewRom = info;
                }
            }
        }

        if (!Constants.alarmExists(activity)) {
            Constants.setAlarm(mActivity, pManager.getTimeNotifications(), true);
        }
        if (mNewRom != null || !mReCheck) {
            checkRomCompleted(mNewRom);
        } else if (mRomCanUpdate) {
            checkRom();
        }

        if (!mRomCanUpdate) {
            Constants.showSimpleDialog(mActivity, R.string.unsupported_rom_title, R.string.unsupported_rom_message);
        }
    }

    @Override
    public void onListChanged() {
        updateFlashQueueText();
    }

    private void checkRom() {
        if (mShowProgress) {
            mProgress = ProgressDialog.show(mActivity, null,
                    mActivity.getResources().getString(R.string.checking), true, true);
        }
        mShowProgress = true;
        mRomUpdater.check();
    }

    private void checkGapps() {
        mProgress = ProgressDialog.show(mActivity, null,
                mActivity.getResources().getString(R.string.checking), true, true);
        mGappsUpdater.check();
    }

    public void checkTwrp(Context context) {
        mProgress = ProgressDialog.show(context, null,
                mActivity.getResources().getString(R.string.checking_twrp), true, true);
        TWRPUpdater twrpUpdater = new TWRPUpdater(context, this);
        twrpUpdater.check();
    }

    @Override
    public void setOnNewIntentListener(OnNewIntentListener listener) {
        mOnNewIntentListener = listener;
    }

    @Override
    public void onNewIntent(Context context, Intent intent) {

        if (mOnNewIntentListener != null) {
            PackageInfo info = mOnNewIntentListener.onNewIntent(context, intent);
            if (info instanceof CancelPackage) {
                mShowProgress = false;
                mReCheck = true;
            } else {
                mNewRom = info;
            }
            if (mNewRom != null || mReCheck) {
                checkRomCompleted(mNewRom);
            } else if (mRomCanUpdate) {
                checkRom();
            }
        }
    }

    @Override
    public void checkRomCompleted(PackageInfo info) {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }

        mReCheck = false;
        if (info == null) {
            mNewRom = null;
            mRemoteVersionHeader.setText("");
            mRemoteVersionBody.setText(R.string.no_new_rom_found);
            mRemoteVersionHeader.setVisibility(View.GONE);
            mRemoteVersionBody.setVisibility(View.VISIBLE);
            mButtonDownload.setVisibility(View.GONE);
        } else {
            mNewRom = info;
            mRemoteVersionHeader.setText(mActivity.getResources().getString(R.string.new_rom_found_title, new Object[] {info.getVersion()}));
            mRemoteVersionBody.setText(info.getMessage(mActivity));
            mRemoteVersionHeader.setVisibility(View.VISIBLE);
            mRemoteVersionBody.setVisibility(View.VISIBLE);
            mButtonDownload.setVisibility(View.VISIBLE);
        }
        mButtonCheckRom.setEnabled(mRomUpdater != null && mRomUpdater.canUpdate());
    }

    @Override
    public void checkGappsCompleted(long newVersion) {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }

        mButtonCheckGapps.setEnabled(mGappsUpdater.canUpdate());
    }

    @Override
    public void checkTWRPCompleted(long newVersion) {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return ManagerFactory.getMenuManager(mActivity).onCreateOptionsMenu(mActivity, menu, R.layout.menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return ManagerFactory.getMenuManager(mActivity).onOptionsItemSelected(mActivity, item);
    }

    private void updateFlashQueueText() {
        mButtonFlashQueue.setText(mActivity.getResources().getString(
                R.string.flash_queue_number,
                new Object[] { String.valueOf(ManagerFactory.getPreferencesManager(mActivity)
                        .getFlashQueueSize()) }));
    }

    @Override
    public int getOrientation() {
        return mActivity.getResources().getConfiguration().orientation;
    }
}
