package com.beerbong.otaplatform;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.beerbong.otaplatform.activity.SettingsActivity;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.ui.component.Header;
import com.beerbong.otaplatform.ui.fragment.InstallFragment;
import com.beerbong.otaplatform.ui.fragment.UpdateFragment;
import com.beerbong.otaplatform.util.Constants;

public class MainActivity extends FragmentActivity implements Header.HeaderChangeListener {

    private static final String HEADER_SELECT = "HEADER_SELECT";

    private Map<Integer, Fragment> mFragments = new HashMap<Integer, Fragment>();
    private int mSelectedHeaderButton = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Header header = (Header) findViewById(R.id.header);

        if (savedInstanceState == null) {

            header.setHeaderChangeListener(this);

            header.select(0);
        } else {
            mSelectedHeaderButton = savedInstanceState.getInt(HEADER_SELECT);
            header.select(mSelectedHeaderButton);
        }

        ManagerFactory.start(this);
        
        if (!Constants.alarmExists(this)) {
            Constants.setAlarm(this, ManagerFactory.getPreferencesManager(this).getTimeNotifications(), true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        ManagerFactory.getFileManager(this).onNewIntent(this, intent);
    }

    @Override
    public void onHeaderChange(int id) {
        Fragment fragment = mFragments.get(id);
        if (fragment == null) {
            switch (id) {
                case R.id.button_update:
                    fragment = new UpdateFragment();
                    mSelectedHeaderButton = 0;
                    break;
                case R.id.button_flash:
                    fragment = new InstallFragment();
                    mSelectedHeaderButton = 1;
                    break;
                case R.id.button_settings:
//                    fragment = new SettingsFragment();
//                    mSelectedHeaderButton = 2;
                    this.startActivity(new Intent(this, SettingsActivity.class));
                    Header header = (Header) findViewById(R.id.header);
                    header.select(mSelectedHeaderButton, false);
                    return;
            }
            mFragments.put(id, fragment);
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

}
