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

package com.beerbong.otaplatform.updater.impl;

import java.util.ArrayList;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.updater.OUCPackage;
import com.beerbong.otaplatform.updater.Updater;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.HttpStringReader;

public class OUCUpdater extends Updater {

    public static final String URL = "https://www.otaupdatecenter.pro/pages/romupdate.php";
    public static final String PROPERTY_OTA_ID = "otaupdater.otaid";
    public static final String PROPERTY_OTA_VER = "otaupdater.otaver";
    public static final String PROPERTY_OTA_TIME = "otaupdater.otatime";

    private UpdaterListener mListener;
    private boolean mScanning = false;

    public OUCUpdater(UpdaterListener listener) {
        mListener = listener;
    }

    @Override
    public String getName() {
         return Constants.getProperty(PROPERTY_OTA_ID);
    }

    @Override
    public String getDeveloperId() {
        return Constants.getProperty(PROPERTY_OTA_ID);
    }

    @Override
    public int getVersion() {
        String version = Constants.getProperty(PROPERTY_OTA_TIME);
        if (version != null) {
            try {
                version = version.replace("-", "");
                return Integer.parseInt(version);
            } catch (NumberFormatException ex) {
            }
        }
        return -1;
    }

    @Override
    public void onReadEnd(String buffer) {
        mScanning = false;
        try {
            final JSONObject json = new JSONObject(buffer);

            if (json.has("error")) {
                String error = json.getString("error");
                mListener.versionError(error);
                return;
            }

            PackageInfo info = new OUCPackage(json);

            mListener.versionFound(info);
        } catch (Exception ex) {
            ex.printStackTrace();
            mListener.versionError(null);
        }
    }

    @Override
    public void onReadError(Exception ex) {
        mListener.versionError(null);
    }

    @Override
    public void searchVersion() {
        mScanning = true;
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("device", android.os.Build.DEVICE.toLowerCase()));
        params.add(new BasicNameValuePair("rom", getName()));
        new HttpStringReader(this).execute(URL + "?" + URLEncodedUtils.format(params, "UTF-8"));
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    @Override
    public int getDrawable() {
        return R.drawable.ouc;
    }

}