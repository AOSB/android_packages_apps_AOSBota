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

package com.beerbong.gooupdater.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public abstract class UI {

    private static UI instance = null;

    public static synchronized void create(Activity activity) {
        if (instance == null) {
            instance = new UIImpl(activity);
        } else {
            instance.redraw(activity);
        }
    }

    public static synchronized UI getInstance() {
        return instance;
    }

    public interface OnNewIntentListener {

        public void onNewIntent(Context context, Intent intent);
    }

    public abstract void redraw(Activity activity);

    public abstract boolean onCreateOptionsMenu(Menu menu);

    public abstract boolean onOptionsItemSelected(MenuItem item);

    public abstract void setOnNewIntentListener(OnNewIntentListener listener);

    public abstract void onNewIntent(Context context, Intent intent);

    public abstract void onListChanged();
}
