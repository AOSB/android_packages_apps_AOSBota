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

package com.beerbong.otaplatform;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.beerbong.otaplatform.activity.SettingsActivity;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.ui.component.Header;
import com.beerbong.otaplatform.ui.fragment.InstallFragment;
import com.beerbong.otaplatform.ui.fragment.UpdateFragment;
import com.beerbong.otaplatform.util.Constants;

public class MainActivity extends FragmentActivity implements Header.HeaderChangeListener {

    private static final String HEADER_SELECT = "HEADER_SELECT";

    private Header mHeader;
    private Map<Integer, Fragment> mFragments = new HashMap<Integer, Fragment>();
    private int mSelectedHeaderButton = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(this).isDarkTheme();
        setTheme(useDarkTheme ? R.style.DarkTheme : R.style.AppTheme);

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mHeader = (Header) findViewById(R.id.header);

        if (savedInstanceState == null) {

            mHeader.setHeaderChangeListener(this);

            mHeader.select(0);
        } else {
            mSelectedHeaderButton = savedInstanceState.getInt(HEADER_SELECT);
            mHeader.select(mSelectedHeaderButton);
        }

        ManagerFactory.start(this);

        if (!Constants.alarmExists(this)) {
            Constants.setAlarm(this, ManagerFactory.getPreferencesManager(this)
                    .getTimeNotifications(), true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        ManagerFactory.getFileManager(this).onNewIntent(this, intent);
    }

    public void headerSelect(int index) {
        mHeader.select(index);
    }

    @Override
    public void onHeaderChange(int id) {
        Fragment fragment = mFragments.get(id);
        if (fragment == null) {
            switch (id) {
                case R.id.button_update:
                    fragment = new UpdateFragment();
                    break;
                case R.id.button_flash:
                    fragment = new InstallFragment();
                    break;
                case R.id.button_settings:
                    startActivity(new Intent(this, SettingsActivity.class));
                    mHeader.select(mSelectedHeaderButton, false);
                    return;
            }
            mFragments.put(id, fragment);
        }
        switch (id) {
            case R.id.button_update:
                mSelectedHeaderButton = 0;
                break;
            case R.id.button_flash:
                mSelectedHeaderButton = 1;
                break;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.frame_layout, fragment);

        transaction.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(HEADER_SELECT, mSelectedHeaderButton);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);

        mFragments.clear();
        mHeader = (Header) findViewById(R.id.header);
        mHeader.setHeaderChangeListener(this);
        mHeader.select(mSelectedHeaderButton);
    }
}
