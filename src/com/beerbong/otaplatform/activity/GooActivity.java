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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.manager.PreferencesManager;
import com.beerbong.otaplatform.ui.component.FolderPreference;
import com.beerbong.otaplatform.updater.GooPackage;
import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.updater.Updater.PackageInfo;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.URLStringReader;
import com.beerbong.otaplatform.util.URLStringReader.URLStringReaderListener;

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

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(this).isDarkTheme();
        setTheme(useDarkTheme ? R.style.DarkTheme : R.style.AppTheme);

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
            preference.setKey(Constants.GOO_SEARCH_URL + "/devs&ro_board=" + mDevice);
            preference.setTitle(R.string.goo_browse_all_compatible);
            pScreen.addPreference(preference);

            preference = new Preference(this);
            preference.getExtras().putBoolean("BROWSING_ALL", true);
            preference.getExtras().putBoolean("FOLDER", true);
            preference.getExtras().putString("PATH", "/devs");
            preference.setKey(Constants.GOO_SEARCH_URL + "/devs");
            preference.setTitle(R.string.goo_browse_all);
            pScreen.addPreference(preference);

            preference = new Preference(this);
            preference.getExtras().putBoolean("BROWSING_ALL", false);
            preference.getExtras().putBoolean("FOLDER", false);
            preference.getExtras().putString("PATH", "");
            preference.setKey("watchlist");
            preference.setTitle(R.string.goo_browse_watchlist);
            pScreen.addPreference(preference);

        } else {

            if ("watchlist".equals(CURRENT_FOLDER)) {

                PreferenceCategory category = new PreferenceCategory(this);
                category.setKey("category");
                category.setTitle(getResources().getString(R.string.goo_category_title,
                        new Object[] { CURRENT_FOLDER }));
                pScreen.addPreference(category);
                refreshWatchlist();
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

                            GooPackage info = new GooPackage(result, -1);
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

                            Preference preference = new FolderPreference(this, folder, false);
                            preference.getExtras().putBoolean("BROWSING_ALL", BROWSING_ALL);
                            preference.getExtras().putBoolean("FOLDER", true);
                            preference.getExtras().putString("PATH", folder);
                            if (!BROWSING_ALL) {
                                preference.setKey(Constants.GOO_SEARCH_URL + folder + "&ro_board="
                                        + mDevice);
                            } else {
                                preference.setKey(Constants.GOO_SEARCH_URL + folder);
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
        }
        if (DIALOG != null)
            DIALOG.dismiss();

        DIALOG = null;
        
        ListView listView = getListView();
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                ListAdapter listAdapter = listView.getAdapter();
                Object obj = listAdapter.getItem(position);
                if (obj != null && obj instanceof View.OnLongClickListener) {
                    View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
                    return longListener.onLongClick(view);
                }
                return false;
            }
        });
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        boolean folder = preference.getExtras().getBoolean("FOLDER");

        if ("watchlist".equals(key)) {

            CURRENT_FOLDER = "watchlist";
            CURRENT_NAVIGATION = "watchlist";
            startActivity(new Intent(this, this.getClass()));
        } else {

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
                                                new Object[] { info.getFilename(), info.getFolder() }))
                                .setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {

                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                dialog.dismiss();

                                                GooActivity.this.runOnUiThread(new Runnable() {

                                                    public void run() {

                                                        ManagerFactory
                                                                .getFileManager(GooActivity.this)
                                                                .download(
                                                                        GooActivity.this,
                                                                        info.getPath(),
                                                                        info.getFilename(),
                                                                        info.getMd5(),
                                                                        false,
                                                                        Constants.DOWNLOADROM_NOTIFICATION_ID);
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
        }

        return true;
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
        DIALOG.setCancelable(true);
        DIALOG.setCanceledOnTouchOutside(true);
        DIALOG.show();

        new URLStringReader(this).execute(path);
    }

    private String[] getWatchlist() {
        String str = ManagerFactory.getPreferencesManager(this).getWatchlist();
        return str.split(PreferencesManager.SEPARATOR);
    }

    @SuppressWarnings("deprecation")
    public void refreshWatchlist() {
        PreferenceCategory category = (PreferenceCategory)getPreferenceScreen().findPreference("category");
        category.removeAll();
        String[] watchlist = getWatchlist();
        for (String folder : watchlist) {
            if (!"".equals(folder)) {
                Preference preference = new FolderPreference(this, folder, true);
                preference.getExtras().putBoolean("BROWSING_ALL", true);
                preference.getExtras().putBoolean("FOLDER", true);
                preference.getExtras().putString("PATH", folder);
                preference.setKey(Constants.GOO_SEARCH_URL + folder);
                preference.setTitle(folder);
                preference.setSummary(folder);
                category.addPreference(preference);
            }
        }
    }
}
