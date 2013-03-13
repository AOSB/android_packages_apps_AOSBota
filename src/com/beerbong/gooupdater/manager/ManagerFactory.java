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

import android.app.Activity;
import android.content.Context;

public class ManagerFactory {

    private static FileManager mFileManager;
    private static PreferencesManager mPreferencesManager;

    public static void start(Activity mActivity) {
        mFileManager = new FileManager(mActivity);
        if (mPreferencesManager == null)
            mPreferencesManager = new PreferencesManager(mActivity);
    }

    public static FileManager getFileManager() {
        return mFileManager;
    }

    public static FileManager getFileManager(Context context) {
        if (mFileManager == null)
            mFileManager = new FileManager(context);
        return mFileManager;
    }

    public static PreferencesManager getPreferencesManager() {
        return mPreferencesManager;
    }

    public static PreferencesManager getPreferencesManager(Context context) {
        if (mPreferencesManager == null)
            mPreferencesManager = new PreferencesManager(context);
        return mPreferencesManager;
    }
}
