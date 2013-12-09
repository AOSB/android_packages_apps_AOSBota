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

package com.probam.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.probam.updater.updater.GappsUpdater;
import com.probam.updater.updater.RomUpdater;
import com.probam.updater.updater.Updater;
import com.probam.updater.util.Constants;

public class NotificationAlarm extends BroadcastReceiver {

    private RomUpdater mRomUpdater;
    private GappsUpdater mGappsUpdater;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Updater.getRomType() != -1) {
            if (mRomUpdater == null) {
                mRomUpdater = Updater.getRomUpdater(context, null, true);
            }
        }
        if (mGappsUpdater == null) {
            mGappsUpdater = new GappsUpdater(context, null, true);
        }

        if (Constants.isNetworkAvailable(context)) {
            if (mRomUpdater != null) {
                mRomUpdater.check();
            }
            mGappsUpdater.check();
        }
    }
}