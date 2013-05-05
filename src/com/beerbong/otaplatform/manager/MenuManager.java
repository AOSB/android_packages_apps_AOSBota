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

package com.beerbong.otaplatform.manager;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.activity.GooActivity;
import com.beerbong.otaplatform.activity.LoginActivity;
import com.beerbong.otaplatform.activity.RecoveryActivity;
import com.beerbong.otaplatform.activity.SettingsActivity;
import com.beerbong.otaplatform.ui.UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MenuManager extends Manager {
    
    protected MenuManager(Context context) {
        super(context);
    }

    public boolean onCreateOptionsMenu(Activity activity, Menu menu, int resId) {

        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(resId, menu);

        return true;
    }

    public boolean onOptionsItemSelected(Activity activity, MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                break;
            case R.id.twrp:
                UI.getInstance().checkTwrp(activity);
                break;
            case R.id.recovery:
                activity.startActivity(new Intent(activity, RecoveryActivity.class));
                break;
            case R.id.login:
                activity.startActivity(new Intent(activity, LoginActivity.class));
                break;
            case R.id.goo:
                GooActivity.CURRENT_NAVIGATION = null;
                activity.startActivity(new Intent(activity, GooActivity.class));
                break;
        }

        return true;
    }
}
