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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.updater.GooPackage;
import com.beerbong.gooupdater.updater.Updater;
import com.beerbong.gooupdater.updater.Updater.PackageInfo;
import com.beerbong.gooupdater.util.Constants;
import com.beerbong.gooupdater.util.URLStringReader;
import com.beerbong.gooupdater.util.URLStringReader.URLStringReaderListener;

public class GooActivity extends PreferenceActivity implements URLStringReaderListener {

    public static String CURRENT_NAVIGATION = null;

    private static ProgressDialog DIALOG;
    private static String CURRENT_FOLDER;
    private static boolean BROWSING_ALL;

    private Map<String, PackageInfo> mInfos;
    private String mDevice = Constants.getProperty(Updater.PROPERTY_DEVICE);

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager().isDarkTheme();
        setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.empty_pref_screen);

        PreferenceScreen pScreen = getPreferenceScreen();
        mInfos = new HashMap<String, PackageInfo>();

        if (CURRENT_NAVIGATION == null) {

            Preference preference = null;

            preference = new Preference(this);
            preference.getExtras().putBoolean("BROWSING_ALL", false);
            preference.getExtras().putBoolean("FOLDER", true);
            preference.getExtras().putString("PATH", "/devs");
            preference.setKey("http://goo.im/json2&path=/devs&ro_board=" + mDevice);
            preference.setTitle(R.string.goo_browse_all_compatible);
            pScreen.addPreference(preference);

            preference = new Preference(this);
            preference.getExtras().putBoolean("BROWSING_ALL", true);
            preference.getExtras().putBoolean("FOLDER", true);
            preference.getExtras().putString("PATH", "/devs");
            preference.setKey("http://goo.im/json2&path=/devs");
            preference.setTitle(R.string.goo_browse_all);
            pScreen.addPreference(preference);

        } else {

            PreferenceCategory category = new PreferenceCategory(this);
            category.setTitle(getResources().getString(R.string.goo_category_title,
                    new Object[] { CURRENT_FOLDER }));
            pScreen.addPreference(category);

            try {

                JSONObject object = (JSONObject) new JSONTokener(CURRENT_NAVIGATION).nextValue();
                JSONArray list = object.getJSONArray("list");

                for (int i = 0; i < list.length(); i++) {

                    JSONObject result = list.getJSONObject(i);
                    String fileName = result.optString("filename");

                    if (fileName != null && !"".equals(fileName.trim())) {

                        String path = result.optString("path");

                        if (!BROWSING_ALL && !mDevice.equals(result.optString("ro_board"))) {
                            continue;
                        }

                        GooPackage info = new GooPackage(result);
                        mInfos.put(path, info);

                        Preference preference = new Preference(this);
                        preference.getExtras().putBoolean("FOLDER", false);
                        preference.setKey(path);
                        preference.setTitle(fileName);
                        preference.setSummary(path);
                        category.addPreference(preference);

                    } else {

                        String folder = result.optString("folder");
                        String folderName = folder.substring(folder.lastIndexOf("/") + 1);

                        Preference preference = new Preference(this);
                        preference.getExtras().putBoolean("BROWSING_ALL", BROWSING_ALL);
                        preference.getExtras().putBoolean("FOLDER", true);
                        preference.getExtras().putString("PATH", folder);
                        if (!BROWSING_ALL) {
                            preference.setKey("http://goo.im/json2&path=" + folder + "&ro_board="
                                    + mDevice);
                        } else {
                            preference.setKey("http://goo.im/json2&path=" + folder);
                        }
                        preference.setTitle(folderName);
                        preference.setSummary(folder);
                        category.addPreference(preference);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, R.string.goo_browse_error, Toast.LENGTH_LONG).show();
            }
        }
        if (DIALOG != null)
            DIALOG.dismiss();

        DIALOG = null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        boolean folder = preference.getExtras().getBoolean("FOLDER");

        if (folder) {

            CURRENT_FOLDER = preference.getExtras().getString("PATH");
            BROWSING_ALL = preference.getExtras().getBoolean("BROWSING_ALL");
            search(key);

        } else {

            final PackageInfo info = mInfos.get(key);

            runOnUiThread(new Runnable() {

                public void run() {
                    new AlertDialog.Builder(GooActivity.this)
                            .setTitle(R.string.goo_download_title)
                            .setMessage(
                                    GooActivity.this.getResources().getString(
                                            R.string.goo_download_summary,
                                            new Object[] { info.filename, info.folder }))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();

                                            GooActivity.this.runOnUiThread(new Runnable() {

                                                public void run() {

                                                    Intent intent = new Intent(GooActivity.this,
                                                            MainActivity.class);
                                                    intent.putExtra("NOTIFICATION_ID",
                                                            Constants.NEWROMVERSION_NOTIFICATION_ID);
                                                    intent.putExtra("URL", info.path);
                                                    intent.putExtra("ZIP_NAME", info.filename);
                                                    intent.putExtra("MD5", info.md5);
                                                    startActivity(intent);
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
                }
            });

        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onReadEnd(String buffer) {
        CURRENT_NAVIGATION = buffer;
        startActivity(new Intent(this, this.getClass()));
    }

    @Override
    public void onReadError(Exception ex) {
        DIALOG.dismiss();
        DIALOG = null;
        ex.printStackTrace();
        Toast.makeText(this, R.string.goo_browse_error, Toast.LENGTH_LONG).show();
    }

    private void search(String path) {

        DIALOG = new ProgressDialog(this);
        DIALOG.setIndeterminate(true);
        DIALOG.setMessage(getResources().getString(R.string.goo_browse_searching));
        DIALOG.setCancelable(false);
        DIALOG.setCanceledOnTouchOutside(false);
        DIALOG.show();

        new URLStringReader(this).execute(path);
    }
}
