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

package com.beerbong.gooupdater;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.manager.PreferencesManager;
import com.beerbong.gooupdater.util.Constants;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    private CheckBoxPreference mDarkTheme;
    private ListPreference mCheckTime;
    private Preference mDownloadPath;

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

        PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        mCheckTime.setValue(String.valueOf(pManager.getTimeNotifications()));
        mCheckTime.setOnPreferenceChangeListener(this);

        mDarkTheme.setChecked(pManager.isDarkTheme());

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

        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (Constants.PREFERENCE_SETTINGS_CHECK_TIME.equals(key)) {

            ManagerFactory.getPreferencesManager().setTimeNotifications(
                    Long.parseLong(newValue.toString()));
            mCheckTime.setValue(newValue.toString());

        }
        return false;
    }

    private void updateSummaries() {
        mDownloadPath.setSummary(ManagerFactory.getPreferencesManager().getDownloadPath());
    }
}