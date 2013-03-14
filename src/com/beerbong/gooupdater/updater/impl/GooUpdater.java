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

package com.beerbong.gooupdater.updater.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.beerbong.gooupdater.updater.Updater;
import com.beerbong.gooupdater.util.Constants;
import com.beerbong.gooupdater.util.URLStringReader;

public class GooUpdater implements Updater {

    public static final String PROPERTY_GOO_DEVELOPER = "ro.goo.developerid";
    public static final String PROPERTY_GOO_ROM = "ro.goo.rom";
    public static final String PROPERTY_GOO_VERSION = "ro.goo.version";

    private UpdaterListener mListener;
    private List<RomInfo> mFoundRoms;
    private int mScanning = 0;

    public GooUpdater(UpdaterListener listener) {
        mListener = listener;
    }

    public String getDeveloperId() {
        return Constants.getProperty(PROPERTY_GOO_DEVELOPER);
    }

    @Override
    public String getRomName() {
        return Constants.getProperty(PROPERTY_GOO_ROM);
    }

    @Override
    public int getRomVersion() {
        String version = Constants.getProperty(PROPERTY_GOO_VERSION);
        if (version != null) {
            try {
                return Integer.parseInt(version);
            } catch (NumberFormatException ex) {
            }
        }
        return -1;
    }

    @Override
    public void searchVersion() {
        mScanning = 0;
        mFoundRoms = new ArrayList<RomInfo>();
        searchGoo("/devs/" + getDeveloperId());
    }

    @Override
    public boolean isScanning() {
        return mScanning > 0;
    }

    private void searchGoo(String path) {
        mScanning++;
        new URLStringReader(this).execute("http://goo.im/json2&path=" + path + "&ro_board="
                + getDevice());
    }

    private String getDevice() {
        return Constants.getProperty(PROPERTY_DEVICE);
    }

    @Override
    public void onReadEnd(String buffer) {
        try {
            String developerId = getDeveloperId();
            String romName = getRomName();
            String device = getDevice();
            mScanning--;
            JSONObject object = (JSONObject) new JSONTokener(buffer).nextValue();
            if (!object.isNull("list")) {
                JSONArray list = object.getJSONArray("list");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject result = list.getJSONObject(i);
                    String fileName = result.optString("filename");
                    if (fileName != null && !"".equals(fileName.trim())) {
                        RomInfo info = new RomInfo();
                        info.developerid = result.optString("ro_developerid");
                        info.board = result.optString("ro_board");
                        info.rom = result.optString("ro_rom");
                        info.version = result.optInt("ro_version");
                        if (!developerId.equals(info.developerid) || !romName.equals(info.rom)
                                || !device.equals(info.board) || info.version <= 0) {
                            continue;
                        }
                        info.id = result.optInt("id");
                        info.filename = result.optString("filename");
                        info.path = "http://goo.im" + result.optString("path");
                        info.folder = result.optString("folder");
                        info.md5 = result.optString("md5");
                        info.type = result.optString("type");
                        info.description = result.optString("description");
                        info.is_flashable = result.optInt("is_flashable");
                        info.modified = result.optLong("modified");
                        info.downloads = result.optInt("downloads");
                        info.status = result.optInt("status");
                        info.additional_info = result.optString("additional_info");
                        info.short_url = result.optString("short_url");
                        info.developer_id = result.optInt("developer_id");
                        info.gapps_package = result.optInt("gapps_package");
                        info.incremental_file = result.optInt("incremental_file");
                        mFoundRoms.add(info);
                    } else {
                        String folder = result.getString("folder");
                        searchGoo(folder);
                    }
                }
            }
            if (mScanning == 0) {
                long newVersion = -2;
                RomInfo newRom = null;
                for (int i = 0; i < mFoundRoms.size(); i++) {
                    RomInfo info = mFoundRoms.get(i);
                    if (info.version > newVersion) {
                        newRom = info;
                    }
                    newVersion = Math.max(newVersion, info.version);
                }
                mListener.versionFound(newRom);
            }
        } catch (Exception ex) {
            mScanning = 0;
            mFoundRoms = new ArrayList<RomInfo>();
            ex.printStackTrace();
            mListener.versionError(null);
        }
    }

    @Override
    public void onReadError(Exception ex) {
        mListener.versionError(null);
    }

}