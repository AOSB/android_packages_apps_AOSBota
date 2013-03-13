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

package com.beerbong.gooupdater.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Constants {

    public static final int NEWVERSION_NOTIFICATION_ID = 122303222;
    public static final int DOWNLOAD_NOTIFICATION_ID = 122303223;

    public static final String PREFERENCE_SETTINGS_DARK_THEME = "darktheme";
    public static final String PREFERENCE_SETTINGS_DOWNLOAD_PATH = "downloadpath";
    public static final String PREFERENCE_SETTINGS_CHECK_TIME = "checktime";

    public static String getProperty(String prop) {
        try {
            String output = null;
            Process p = Runtime.getRuntime().exec("getprop " + prop);
            p.waitFor();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            output = input.readLine();
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
