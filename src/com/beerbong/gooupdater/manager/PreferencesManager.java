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

package com.beerbong.gooupdater.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesManager extends Manager {

    private static final String SDCARD = "sdcard";

    private static final String PROPERTY_DARK_THEME = "dark-theme";
    private static final String PROPERTY_DOWNLOAD_PATH = "download_path";
    private static final String PROPERTY_TIME_NOTIFICATIONS = "time_notifications";
    private static final String PROPERTY_LOGIN = "login";
    private static final String PROPERTY_WATCHLIST = "watchlist";
    private static final String PROPERTY_GAPPS = "gapps";

    private static final String DEFAULT_TIME_NOTIFICATIONS = "3600000"; // an hour
    private static final String DEFAULT_DOWNLOAD_PATH = "/" + SDCARD + "/download/";
    private static final boolean DEFAULT_DARK_THEME = true;

    public static final String WATCHLIST_SEPARATOR = "#-#";

    private SharedPreferences settings;

    protected PreferencesManager(Context context) {
        super(context);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isDarkTheme() {
        return settings.getBoolean(PROPERTY_DARK_THEME, DEFAULT_DARK_THEME);
    }

    public void setDarkTheme(boolean value) {
        savePreference(PROPERTY_DARK_THEME, value);
    }

    public long getTimeNotifications() {
        return Long.parseLong(settings.getString(PROPERTY_TIME_NOTIFICATIONS,
                DEFAULT_TIME_NOTIFICATIONS));
    }

    public void setTimeNotifications(long value) {
        savePreference(PROPERTY_TIME_NOTIFICATIONS, String.valueOf(value));
    }

    public String getDownloadPath() {
        return settings.getString(PROPERTY_DOWNLOAD_PATH, DEFAULT_DOWNLOAD_PATH);
    }

    public void setDownloadPath(String value) {
        if (!value.endsWith("/"))
            value = value + "/";
        savePreference(PROPERTY_DOWNLOAD_PATH, value);
    }

    public String getLogin() {
        return settings.getString(PROPERTY_LOGIN, "");
    }

    public void setLogin(String value) {
        savePreference(PROPERTY_LOGIN, value);
    }

    public String getWatchlist() {
        return settings.getString(PROPERTY_WATCHLIST, "");
    }

    public void setWatchlist(String value) {
        savePreference(PROPERTY_WATCHLIST, value);
    }

    public String getGappsFolder() {
        return settings.getString(PROPERTY_GAPPS, "");
    }

    public void setGappsFolder(String value) {
        savePreference(PROPERTY_GAPPS, value);
    }

    private void savePreference(String preference, String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(preference, value);
        editor.commit();
    }

    private void savePreference(String preference, boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(preference, value);
        editor.commit();
    }
}