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

package com.probam.updater.manager;

import java.util.List;

import com.probam.updater.util.Constants;
import com.probam.updater.util.FileItem;

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
    private static final String PROPERTY_GAPPS_CHECK = "checkgapps";
    private static final String PROPERTY_GAPPS = "gapps";
    private static final String PROPERTY_QUEUE = "queue";
    private static final String PROPERTY_INTERNAL_STORAGE = "internal-storage";
    private static final String PROPERTY_EXTERNAL_STORAGE = "external-storage";
    private static final String PROPERTY_RECOVERY = "recovery";
    private static final String PROPERTY_SHOW_OPTIONS = "show-option";

    private static final String DEFAULT_TIME_NOTIFICATIONS = "18000000"; // five hours
    private static final String DEFAULT_DOWNLOAD_PATH = "/" + SDCARD + "/download/";
    private static final String DEFAULT_RECOVERY = "cwmbased";
    private static final String DEFAULT_INTERNAL_STORAGE = "emmc";
    private static final String DEFAULT_EXTERNAL_STORAGE = "sdcard";
    private static final String DEFAULT_SHOW_OPTIONS = Constants.INSTALL_OPTIONS_DEFAULT;
    private static final boolean DEFAULT_DARK_THEME = false;

    public static final String SEPARATOR = "#-#";

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

    public boolean getGappsCheck() {
        return settings.getBoolean(PROPERTY_GAPPS_CHECK, false);
    }

    public void setGappsCheck(boolean value) {
        savePreference(PROPERTY_GAPPS_CHECK, value);
    }

    public String getGappsFolder() {
        return settings.getString(PROPERTY_GAPPS,
                Constants.getProperty(Constants.OVERLAY_GAPPS_URL));
    }

    public void setGappsFolder(String value) {
        savePreference(PROPERTY_GAPPS, value);
    }

    public String[] getFlashQueue() {
        String queue = settings.getString(PROPERTY_QUEUE, "");
        return queue.equals("") ? new String[0] : queue.split(SEPARATOR);
    }

    public void addFlashQueue(String value) {
        String queue = settings.getString(PROPERTY_QUEUE, "");
        if (queue.indexOf(value) >= 0) {
            return;
        }
        if ("".equals(queue)) {
            queue = value;
        } else {
            queue += SEPARATOR + value;
        }
        savePreference(PROPERTY_QUEUE, queue);
    }

    public void removeFlashQueue(String value) {
        String queue = settings.getString(PROPERTY_QUEUE, "");
        if (queue.indexOf(value) < 0) {
            return;
        }
        if (!"".equals(queue)) {
            if (queue.equals(value)) {
                queue = "";
            } else {
                queue = queue.replace(SEPARATOR + value, "");
                queue = queue.replace(value + SEPARATOR, "");
            }
        }
        savePreference(PROPERTY_QUEUE, queue);
    }

    public int getFlashQueueSize() {
        return getFlashQueue().length;
    }

    public void setFlashQueue(List<FileItem> items) {
        String queue = "";
        if (items != null) {
            for (FileItem item : items) {
                queue += item.toString();
                if (!item.equals(items.get(items.size() - 1))) {
                    queue += SEPARATOR;
                }
            }
        }
        savePreference(PROPERTY_QUEUE, queue);
    }

    public String getInternalStorage() {
        return settings.getString(PROPERTY_INTERNAL_STORAGE, DEFAULT_INTERNAL_STORAGE);
    }

    public void setInternalStorage(String value) {
        savePreference(PROPERTY_INTERNAL_STORAGE, value);
    }

    public String getExternalStorage() {
        return settings.getString(PROPERTY_EXTERNAL_STORAGE, DEFAULT_EXTERNAL_STORAGE);
    }

    public void setExternalStorage(String value) {
        savePreference(PROPERTY_EXTERNAL_STORAGE, value);
    }

    public boolean existsRecovery() {
        return settings.contains(PROPERTY_RECOVERY);
    }

    public String getRecovery() {
        return settings.getString(PROPERTY_RECOVERY, DEFAULT_RECOVERY);
    }

    public void setRecovery(String value) {
        savePreference(PROPERTY_RECOVERY, value);
    }

    public boolean isShowOption(String option) {
        String opts = settings.getString(PROPERTY_SHOW_OPTIONS, DEFAULT_SHOW_OPTIONS);
        return opts.indexOf(option) >= 0;
    }

    public String getShowOptions() {
        return settings.getString(PROPERTY_SHOW_OPTIONS, DEFAULT_SHOW_OPTIONS);
    }

    public void setShowOptions(String options) {
        savePreference(PROPERTY_SHOW_OPTIONS, options);
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