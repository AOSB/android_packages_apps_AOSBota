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

package com.probam.updater.updater.impl;

import org.json.JSONObject;

import com.probam.updater.updater.GooPackage;
import com.probam.updater.updater.Updater;
import com.probam.updater.util.Constants;
import com.probam.updater.util.URLStringReader;

public class GooUpdater extends Updater {

    public static final String PROPERTY_GOO_DEVELOPER = "ro.goo.developerid";
    public static final String PROPERTY_GOO_ROM = "ro.goo.rom";
    public static final String PROPERTY_GOO_VERSION = "ro.goo.version";

    private UpdaterListener mListener;
    private boolean mScanning = false;

    public GooUpdater(UpdaterListener listener) {
        mListener = listener;
    }

    @Override
    public String getDeveloperId() {
        return Constants.getProperty(PROPERTY_GOO_DEVELOPER);
    }

    @Override
    public String getName() {
        return Constants.getProperty(PROPERTY_GOO_ROM);
    }

    @Override
    public int getVersion() {
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
        mScanning = false;
        searchGoo("/devs/" + getDeveloperId());
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    private void searchGoo(String path) {
        mScanning = true;
        new URLStringReader(this).execute("http://goo.im/json2&action=update&ro_developerid="
                + getDeveloperId() + "&ro_board=" + getDevice() + "&ro_rom=" + getName()
                + "&ro_version=" + getVersion());
    }

    private String getDevice() {
        return Constants.getProperty(PROPERTY_DEVICE);
    }

    @Override
    public void onReadEnd(String buffer) {
        try {
            mScanning = false;
            PackageInfo newRom = null;
            if (buffer != null && !"".equals(buffer)) {
                JSONObject update_info = new JSONObject(buffer);
                newRom = new GooPackage(update_info, getVersion());
            }
            mListener.versionFound(newRom);
        } catch (Exception ex) {
            mScanning = false;
            ex.printStackTrace();
            mListener.versionError(null);
        }
    }

    @Override
    public void onReadError(Exception ex) {
        mListener.versionError(null);
    }

}
