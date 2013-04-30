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

import java.io.DataOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.InstallOptionsCursor;

public class RebootManager extends Manager {

    protected RebootManager(Context context) {
        super(context);

        mContext = context;
    }

    public void showRebootDialog(Context context) {

        if (ManagerFactory.getPreferencesManager().getFlashQueueSize() == 0)
            return;

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.alert_reboot_title);

        final InstallOptionsCursor cursor = new InstallOptionsCursor(context);

        alert.setMultiChoiceItems(cursor, cursor.getIsCheckedColumn(), cursor.getLabelColumn(),
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        cursor.setOption(which, isChecked);
                    }

                });

        alert.setPositiveButton(R.string.alert_reboot_now, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                reboot(cursor.isWipeData(), cursor.isWipeCaches());

            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void reboot(final boolean wipeData, final boolean wipeCaches) {

        try {

            RecoveryManager manager = ManagerFactory.getRecoveryManager();

            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes("rm -f /cache/recovery/command\n");
            os.writeBytes("rm -f /cache/recovery/extendedcommand\n");
            os.writeBytes("rm -f /cache/recovery/openrecoveryscript\n");

            String file = manager.getCommandsFile();

            String[] commands = manager.getCommands(wipeData, wipeCaches);
            if (commands != null) {
                int size = commands.length, i = 0;
                for (; i < size; i++) {
                    os.writeBytes("echo '" + commands[i] + "' >> /cache/recovery/" + file
                            + "\n");
                }
            }

            os.writeBytes("/system/bin/touch /cache/recovery/boot\n");
            os.writeBytes("reboot recovery\n");

            ManagerFactory.getFileManager().clearItems();
            ManagerFactory.getPreferencesManager().setFlashQueue(null);

            os.writeBytes("sync\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();

            if (Constants.isSystemApp(mContext)) {
                ((PowerManager) mContext.getSystemService(Activity.POWER_SERVICE))
                        .reboot("recovery");
            } else {
                Runtime.getRuntime().exec("/system/bin/reboot recovery");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}