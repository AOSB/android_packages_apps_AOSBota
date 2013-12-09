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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;

import com.probam.updater.activity.SettingsActivity;
import com.probam.updater.manager.ManagerFactory;
import com.probam.updater.ui.fragment.ChangelogFragment;
import com.probam.updater.ui.fragment.InstallFragment;
import com.probam.updater.ui.fragment.UpdateFragment;
import com.probam.updater.updater.RomUpdater;
import com.probam.updater.updater.Updater;
import com.probam.updater.util.Constants;

public class MainActivity extends FragmentActivity implements TabHost.OnTabChangeListener,
        ViewPager.OnPageChangeListener {

    private TabHost mTabHost;
    private ViewPager mViewPager;
    private List<Fragment> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(this).isDarkTheme();
        setTheme(useDarkTheme ? R.style.DarkTheme : R.style.AppTheme);

        RomUpdater romUpdater = Updater.getRomUpdater(this, null, false);
        if (romUpdater == null || !romUpdater.canUpdate()) {
            Constants.showSimpleDialog(this, R.string.unsupported_rom_title,
                    R.string.unsupported_rom_message);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        String romChangelogUrl = null;
        if (romUpdater != null && romUpdater.canUpdate()) {
            romChangelogUrl = Constants.getProperty(Constants.OVERLAY_CHANGELOG);
        }

        initialiseTabHost(savedInstanceState, romChangelogUrl);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        intialiseViewPager(romChangelogUrl);

        if (!Constants.alarmExists(this)) {
            Constants.setAlarm(this, ManagerFactory.getPreferencesManager(this)
                    .getTimeNotifications(), true);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        ((UpdateFragment)mFragments.get(0)).checkIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag());
        super.onSaveInstanceState(outState);
    }

    private void intialiseViewPager(String romChangelogUrl) {

        mFragments = new ArrayList<Fragment>();
        mFragments.add(Fragment.instantiate(this, UpdateFragment.class.getName()));
        mFragments.add(Fragment.instantiate(this, InstallFragment.class.getName()));
        if (romChangelogUrl != null && !"".equals(romChangelogUrl)) {
            mFragments.add(Fragment.instantiate(this, ChangelogFragment.class.getName()));
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }
        });
        mViewPager.setOnPageChangeListener(this);
    }

    private void initialiseTabHost(Bundle args, String romChangelogUrl) {
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        addTab("update", R.string.update);
        addTab("install", R.string.flash);
        if (romChangelogUrl != null && !"".equals(romChangelogUrl)) {
            addTab("changelog", R.string.changelog);
        }

        mTabHost.setOnTabChangedListener(this);
    }

    private void addTab(String id, int resId) {
        TabHost.TabSpec tabSpec = mTabHost.newTabSpec(id);
        tabSpec.setIndicator(getResources().getString(resId));
        tabSpec.setContent(new TabContentFactory() {

            @Override
            public View createTabContent(String tag) {
                View v = new View(MainActivity.this);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        });
        mTabHost.addTab(tabSpec);
    }

    public void onTabChanged(String tag) {
        int pos = mTabHost.getCurrentTab();
        mViewPager.setCurrentItem(pos);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mTabHost.setCurrentTab(position);
        switch (position) {
            case 1:
                ((InstallFragment)mFragments.get(position)).notifyDataChanged();
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }
}
