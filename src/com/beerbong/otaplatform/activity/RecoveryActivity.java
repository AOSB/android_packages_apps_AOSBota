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

package com.beerbong.otaplatform.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.util.Constants;

public class RecoveryActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean useDarkTheme = ManagerFactory.getPreferencesManager(this).isDarkTheme();
        setTheme(useDarkTheme ? R.style.DarkTheme : R.style.AppTheme);

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.recovery_activity);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (Constants.PREFERENCE_RECOVERY_BACKUP.equals(key)) {
            ManagerFactory.getRebootManager(this).showBackupDialog(this);
        } else if (Constants.PREFERENCE_RECOVERY_RESTORE.equals(key)) {
            ManagerFactory.getRebootManager(this).showRestoreDialog(this);
        } else if (Constants.PREFERENCE_RECOVERY_DELETE.equals(key)) {
            ManagerFactory.getFileManager(this).showDeleteDialog(this);
        } else if (Constants.PREFERENCE_RECOVERY_ACTIONS.equals(key)) {
            showAlert();
        } else if (Constants.PREFERENCE_RECOVERY_REBOOT.equals(key)) {
            ManagerFactory.getRebootManager(this).simpleReboot(this);
        }
        return true;
    }

    private void showAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.recovery_activity_actions);
        
        String[] items = {
                getResources().getString(R.string.wipe_data),
                getResources().getString(R.string.wipe_caches),
                getResources().getString(R.string.fix_permissions)
        };
        final boolean[] checkedItems = new boolean[3];
        alert.setMultiChoiceItems(items, checkedItems, new OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedItems[which] = isChecked;
            }
            
        });
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                dialog.dismiss();
                
                ManagerFactory.getRebootManager(RecoveryActivity.this).simpleReboot(
                        RecoveryActivity.this, checkedItems[0], checkedItems[1], checkedItems[2]);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }
}