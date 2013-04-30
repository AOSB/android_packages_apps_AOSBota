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

package com.beerbong.otaplatform.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.EditText;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.manager.PreferencesManager;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.RecoveryInfo;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    private CheckBoxPreference mDarkTheme;
    private ListPreference mCheckTime;
    private Preference mDownloadPath;
    private Preference mGappsFolder;
    private Preference mGappsReset;
    private Preference mRecovery;
    private Preference mInternalSdcard;
    private Preference mExternalSdcard;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager().isDarkTheme();
        setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.settings);

        mDarkTheme = (CheckBoxPreference) findPreference(Constants.PREFERENCE_SETTINGS_DARK_THEME);
        mDownloadPath = findPreference(Constants.PREFERENCE_SETTINGS_DOWNLOAD_PATH);
        mCheckTime = (ListPreference) findPreference(Constants.PREFERENCE_SETTINGS_CHECK_TIME);
        mGappsFolder = findPreference(Constants.PREFERENCE_SETTINGS_GAPPS_FOLDER);
        mGappsReset = findPreference(Constants.PREFERENCE_SETTINGS_GAPPS_RESET);
        mRecovery = findPreference(Constants.PREFERENCE_SETTINGS_RECOVERY);
        mInternalSdcard = findPreference(Constants.PREFERENCE_SETTINGS_INTERNAL_SDCARD);
        mExternalSdcard = findPreference(Constants.PREFERENCE_SETTINGS_EXTERNAL_SDCARD);

        PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        mCheckTime.setValue(String.valueOf(pManager.getTimeNotifications()));
        mCheckTime.setOnPreferenceChangeListener(this);

        mDarkTheme.setChecked(pManager.isDarkTheme());
        
        if (!ManagerFactory.getFileManager().hasExternalStorage()) {
            getPreferenceScreen().removePreference(mExternalSdcard);
        }

        updateSummaries();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        if (Constants.PREFERENCE_SETTINGS_DARK_THEME.equals(key)) {

            boolean darkTheme = ((CheckBoxPreference) preference).isChecked();
            pManager.setDarkTheme(darkTheme);

        } else if (Constants.PREFERENCE_SETTINGS_DOWNLOAD_PATH.equals(key)) {

            ManagerFactory.getFileManager().selectDownloadPath(this);
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_GAPPS_FOLDER.equals(key)) {

            selectGappsFolder();
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_GAPPS_RESET.equals(key)) {

            ManagerFactory.getPreferencesManager().setGappsFolder("");
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_RECOVERY.equals(key)) {

            ManagerFactory.getRecoveryManager().selectRecovery(this);
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_INTERNAL_SDCARD.equals(key)) {

            ManagerFactory.getRecoveryManager().selectSdcard(this, true);
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_EXTERNAL_SDCARD.equals(key)) {

            ManagerFactory.getRecoveryManager().selectSdcard(this, false);
            updateSummaries();

        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (Constants.PREFERENCE_SETTINGS_CHECK_TIME.equals(key)) {

            long time = Long.parseLong(newValue.toString());
            ManagerFactory.getPreferencesManager().setTimeNotifications(time);
            Constants.setAlarm(this, time, false);
            mCheckTime.setValue(newValue.toString());

        }
        return false;
    }

    private void updateSummaries() {
        PreferencesManager pManager = ManagerFactory.getPreferencesManager();
        mDownloadPath.setSummary(pManager.getDownloadPath());
        String folder = ManagerFactory.getPreferencesManager().getGappsFolder();
        if (folder == null || "".equals(folder)) {
            mGappsReset.setEnabled(false);
            folder = getResources().getString(R.string.gapps_folder_official);
        } else {
            mGappsReset.setEnabled(true);
        }
        mGappsFolder.setSummary(folder);
        RecoveryInfo info = ManagerFactory.getRecoveryManager().getRecovery();
        mRecovery.setSummary(getResources().getText(R.string.recovery_summary) + " ("
                + info.getName() + ")");
        mInternalSdcard.setSummary(getResources().getText(R.string.internalsdcard_summary) + " ("
                + pManager.getInternalStorage() + ")");
        mExternalSdcard.setSummary(getResources().getText(R.string.externalsdcard_summary) + " ("
                + pManager.getExternalStorage() + ")");
    }

    public void selectGappsFolder() {
        final PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        String folder = pManager.getGappsFolder();
        if (folder == null || "".equals(folder)) {
            folder = "/devs/";
        }

        final EditText input = new EditText(this);
        input.setText(folder);
        input.setSelection(folder.length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.gapps_folder_alert_title)
                .setMessage(R.string.gapps_folder_alert_summary)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();

                        if (value == null || "".equals(value.trim())) {
                            pManager.setGappsFolder("");
                        } else if (value.endsWith("/")) {
                            value = value.substring(0, value.length() - 1);
                        }

                        pManager.setGappsFolder(value);
                        updateSummaries();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }
}