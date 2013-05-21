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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.manager.PreferencesManager;
import com.beerbong.otaplatform.manager.RecoveryManager;
import com.beerbong.otaplatform.updater.TWRPUpdater;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.RecoveryInfo;
import com.beerbong.otaplatform.util.URLStringReader;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener,
        TWRPUpdater.TWRPUpdaterListener, URLStringReader.URLStringReaderListener {

    private static final String LOGIN_URL = "http://goo-inside.me/salt";

    private ProgressDialog mProgress;
    private AlertDialog mLoginDialog;
    private CheckBoxPreference mDarkTheme;
    private ListPreference mCheckTime;
    private Preference mDownloadPath;
    private Preference mGappsFolder;
    private Preference mGappsReset;
    private Preference mRecovery;
    private Preference mInternalSdcard;
    private Preference mExternalSdcard;
    private ListPreference mOptions;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(this).isDarkTheme();
        setTheme(useDarkTheme ? R.style.DarkTheme : R.style.AppTheme);

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.settings);

        mDarkTheme = (CheckBoxPreference)findPreference(Constants.PREFERENCE_SETTINGS_DARK_THEME);
        mDownloadPath = findPreference(Constants.PREFERENCE_SETTINGS_DOWNLOAD_PATH);
        mCheckTime = (ListPreference) findPreference(Constants.PREFERENCE_SETTINGS_CHECK_TIME);
        mGappsFolder = findPreference(Constants.PREFERENCE_SETTINGS_GAPPS_FOLDER);
        mGappsReset = findPreference(Constants.PREFERENCE_SETTINGS_GAPPS_RESET);
        mRecovery = findPreference(Constants.PREFERENCE_SETTINGS_RECOVERY);
        mInternalSdcard = findPreference(Constants.PREFERENCE_SETTINGS_INTERNAL_SDCARD);
        mExternalSdcard = findPreference(Constants.PREFERENCE_SETTINGS_EXTERNAL_SDCARD);
        mOptions = (ListPreference) findPreference(Constants.PREFERENCE_SETTINGS_OPTIONS);

        PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);

        mCheckTime.setValue(String.valueOf(pManager.getTimeNotifications()));
        mCheckTime.setOnPreferenceChangeListener(this);

        mDarkTheme.setChecked(pManager.isDarkTheme());

        mOptions.setValue(pManager.getShowOptions());
        mOptions.setOnPreferenceChangeListener(this);

        if (!ManagerFactory.getFileManager(this).hasExternalStorage()) {
            getPreferenceScreen().removePreference(mExternalSdcard);
        }

        updateSummaries();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);
        RecoveryManager rManager = ManagerFactory.getRecoveryManager(this);

        if (Constants.PREFERENCE_SETTINGS_DARK_THEME.equals(key)) {

            boolean darkTheme = ((CheckBoxPreference) preference).isChecked();
            pManager.setDarkTheme(darkTheme);

        } else if (Constants.PREFERENCE_SETTINGS_DOWNLOAD_PATH.equals(key)) {

            ManagerFactory.getFileManager(this).selectDownloadPath(this);
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_GAPPS_FOLDER.equals(key)) {

            selectGappsFolder();
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_GAPPS_RESET.equals(key)) {

            pManager.setGappsFolder("");
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_RECOVERY.equals(key)) {

            rManager.selectRecovery(this);
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_INTERNAL_SDCARD.equals(key)) {

            rManager.selectSdcard(this, true);
            updateSummaries();

        } else if (Constants.PREFERENCE_SETTINGS_EXTERNAL_SDCARD.equals(key)) {

            rManager.selectSdcard(this, false);
            updateSummaries();

        } else if ("installtwrp".equals(key)) {
            
            checkTwrp();

        } else if ("browsegoo".equals(key)) {

            GooActivity.CURRENT_NAVIGATION = null;
            startActivity(new Intent(this, GooActivity.class));
            
        } else if ("logingoo".equals(key)) {

            showLoginDialog();

        } else if ("recoveryactivity".equals(key)) {
            
            startActivity(new Intent(this, RecoveryActivity.class));
            
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);

        if (Constants.PREFERENCE_SETTINGS_OPTIONS.equals(key)) {
            List<String> values = (List<String>) newValue;
            String result = "";
            for (int i = 0; i < values.size(); i++) {
                result += values.get(i);
                if (i < values.size() - 1)
                    result += "|";
            }
            pManager.setShowOptions(result);

        } else if (Constants.PREFERENCE_SETTINGS_CHECK_TIME.equals(key)) {

            long time = Long.parseLong(newValue.toString());
            pManager.setTimeNotifications(time);
            Constants.setAlarm(this, time, false);
            mCheckTime.setValue(newValue.toString());

        }
        return false;
    }

    private void updateSummaries() {
        PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);
        mDownloadPath.setSummary(pManager.getDownloadPath());
        String folder = pManager.getGappsFolder();
        if (folder == null || "".equals(folder)) {
            mGappsReset.setEnabled(false);
            folder = getResources().getString(R.string.gapps_folder_official);
        } else {
            mGappsReset.setEnabled(true);
        }
        mGappsFolder.setSummary(folder);
        RecoveryInfo info = ManagerFactory.getRecoveryManager(this).getRecovery();
        mRecovery.setSummary(getResources().getText(R.string.recovery_summary) + " ("
                + info.getName() + ")");
        mInternalSdcard.setSummary(getResources().getText(R.string.internalsdcard_summary) + " ("
                + pManager.getInternalStorage() + ")");
        mExternalSdcard.setSummary(getResources().getText(R.string.externalsdcard_summary) + " ("
                + pManager.getExternalStorage() + ")");
    }

    public void selectGappsFolder() {
        final PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);

        String folder = pManager.getGappsFolder();
        if (folder == null || "".equals(folder)) {
            folder = "/devs/";
        }

        final EditText input = new EditText(this);
        input.setText(folder);
        input.setSelection(folder.length());

        new AlertDialog.Builder(this).setTitle(R.string.gapps_folder_alert_title)
                .setMessage(R.string.gapps_folder_alert_summary).setView(input)
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

    public void checkTwrp() {
        mProgress = ProgressDialog.show(this, null,
                getResources().getString(R.string.checking_twrp), true, true);
        TWRPUpdater twrpUpdater = new TWRPUpdater(this, this);
        twrpUpdater.check();
    }

    @Override
    public void checkTWRPCompleted(long newVersion) {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
    }

    public void showLoginDialog() {

        final PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.login, null);
        final EditText username = (EditText)view.findViewById(R.id.username);
        final EditText password = (EditText)view.findViewById(R.id.password);

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.menu_login)
                .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        String user = username.getText() == null ? "" : username.getText().toString();
                        String pass = password.getText() == null ? "" : password.getText().toString();
                        mProgress = ProgressDialog.show(SettingsActivity.this, null,
                                getResources().getString(R.string.logging_in), true, true);
                        try {
                            new URLStringReader(SettingsActivity.this).execute(LOGIN_URL + "&username="
                                    + URLEncoder.encode(user, "UTF-8") + "&password=" + URLEncoder.encode(pass, "UTF-8"));
                        } catch (UnsupportedEncodingException ex) {
                            // should never get here
                        }
                    }
                })
                .setNeutralButton(R.string.logout, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        pManager.setLogin("");
                        Constants.showToastOnUiThread(SettingsActivity.this, R.string.logged_out);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        builder.setView(view);
        mLoginDialog = builder.create();
        mLoginDialog.show();

        String login = pManager.getLogin();
        boolean logged = login != null && !"".equals(login);
        mLoginDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(!logged);
        mLoginDialog.getButton(Dialog.BUTTON_NEUTRAL).setEnabled(logged);
    }

    @Override
    public void onReadEnd(String buffer) {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        if (buffer != null && buffer.length() == 32) {
            ManagerFactory.getPreferencesManager(this).setLogin(buffer);
            Constants.showToastOnUiThread(this, R.string.logged_in);
            mLoginDialog.dismiss();
        } else if (buffer != null) {
            Constants.showToastOnUiThread(this, R.string.logged_invalid);
        } else {
            Constants.showToastOnUiThread(this, R.string.logged_down);
        }
    }

    @Override
    public void onReadError(Exception ex) {
    }
}