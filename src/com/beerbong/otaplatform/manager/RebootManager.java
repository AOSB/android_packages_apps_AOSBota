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
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.InstallOptionsCursor;

public class RebootManager extends Manager {

    private int mSelectedBackup;

    protected RebootManager(Context context) {
        super(context);

        mContext = context;
    }

    public void showBackupDialog(Context context) {
        showBackupDialog(context, true, false, false, false, false);
    }

    public void showRestoreDialog(final Context context) {

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.alert_restore_title);

        final String backupFolder = ManagerFactory.getRecoveryManager(context).getBackupDir(false);
        final String[] backups = ManagerFactory.getRecoveryManager(context).getBackupList();
        mSelectedBackup = backups.length > 0 ? 0 : -1;

        alert.setSingleChoiceItems(backups, mSelectedBackup, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                mSelectedBackup = which;
            }
        });

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                if (mSelectedBackup >= 0) {
                    reboot(context, false, false, false, false, null, null, backupFolder
                            + backups[mSelectedBackup]);
                }
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();

    }

    public void simpleReboot(Context context) {
        reboot(context, false, false, false, false, null, null, null, true);
    }

    public void simpleReboot(Context context, boolean wipeData, boolean wipeCaches,
            boolean fixPermissions) {
        ManagerFactory.getFileManager(mContext).clearItems();
        ManagerFactory.getPreferencesManager(mContext).setFlashQueue(null);
        // UI.getInstance().onListChanged();
        reboot(context, false, wipeData, wipeCaches, fixPermissions, null, null, null, false);
    }

    public void fixPermissions(Context context) {
        ManagerFactory.getFileManager(mContext).clearItems();
        ManagerFactory.getPreferencesManager(mContext).setFlashQueue(null);
        // UI.getInstance().onListChanged();
        reboot(context, false, false, false, true, null, null, null, false);
    }

    private void showBackupDialog(final Context context, final boolean removePreferences,
            final boolean wipeSystem, final boolean wipeData, final boolean wipeCaches,
            final boolean fixPermissions) {

        double checkSpace = 1.0;// ManagerFactory.getPreferencesManager().getSpaceLeft();
        if (checkSpace > 0) {
            double spaceLeft = ManagerFactory.getFileManager(context).getSpaceLeft();
            if (spaceLeft < checkSpace) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle(R.string.alert_backup_space_title);
                alert.setMessage(context.getResources().getString(
                        R.string.alert_backup_space_message, checkSpace));

                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();

                        reallyShowBackupDialog(context, removePreferences, wipeSystem, wipeData,
                                wipeCaches, fixPermissions);
                    }
                });

                alert.setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alert.show();
            } else {
                reallyShowBackupDialog(context, removePreferences, wipeSystem, wipeData,
                        wipeCaches, fixPermissions);
            }
        } else {
            reallyShowBackupDialog(context, removePreferences, wipeSystem, wipeData, wipeCaches,
                    fixPermissions);
        }
    }

    private void reallyShowBackupDialog(final Context context, boolean removePreferences,
            final boolean wipeSystem, final boolean wipeData, final boolean wipeCaches,
            final boolean fixPermissions) {
        if (removePreferences) {
            ManagerFactory.getFileManager(context).clearItems();
            ManagerFactory.getPreferencesManager(context).setFlashQueue(null);
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.alert_backup_title);
        View view = LayoutInflater.from(context).inflate(R.layout.backup_dialog,
                (ViewGroup) ((Activity) context).findViewById(R.id.backup_dialog_layout));
        alert.setView(view);

        final CheckBox cbSystem = (CheckBox) view.findViewById(R.id.system);
        final CheckBox cbData = (CheckBox) view.findViewById(R.id.data);
        final CheckBox cbCache = (CheckBox) view.findViewById(R.id.cache);
        final CheckBox cbRecovery = (CheckBox) view.findViewById(R.id.recovery);
        final CheckBox cbBoot = (CheckBox) view.findViewById(R.id.boot);
        final CheckBox cbSecure = (CheckBox) view.findViewById(R.id.androidsecure);
        final CheckBox cbSdext = (CheckBox) view.findViewById(R.id.sdext);
        final EditText input = (EditText) view.findViewById(R.id.backupname);

        input.setText(Constants.getDateAndTime());
        input.selectAll();

        final RecoveryManager rManager = ManagerFactory.getRecoveryManager(context);
        if (rManager.getRecovery().getId() == R.id.twrp) {
            if (!rManager.hasAndroidSecure()) {
                cbSecure.setVisibility(View.GONE);
            }
            if (!rManager.hasSdExt()) {
                cbSdext.setVisibility(View.GONE);
            }
        } else {
            cbSystem.setVisibility(View.GONE);
            cbData.setVisibility(View.GONE);
            cbCache.setVisibility(View.GONE);
            cbRecovery.setVisibility(View.GONE);
            cbBoot.setVisibility(View.GONE);
            cbSecure.setVisibility(View.GONE);
            cbSdext.setVisibility(View.GONE);
        }

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                String text = input.getText().toString();
                text = text.replace(" ", "");

                String backupOptions = null;
                if (rManager.getRecovery().getId() == R.id.twrp) {
                    backupOptions = "";
                    if (cbSystem.isChecked()) {
                        backupOptions += "S";
                    }
                    if (cbData.isChecked()) {
                        backupOptions += "D";
                    }
                    if (cbCache.isChecked()) {
                        backupOptions += "C";
                    }
                    if (cbRecovery.isChecked()) {
                        backupOptions += "R";
                    }
                    if (cbBoot.isChecked()) {
                        backupOptions += "B";
                    }
                    if (cbSecure.isChecked()) {
                        backupOptions += "A";
                    }
                    if (cbSdext.isChecked()) {
                        backupOptions += "E";
                    }

                    if ("".equals(backupOptions)) {
                        return;
                    }
                }

                reboot(context, wipeSystem, wipeData, wipeCaches, false, text, backupOptions, null);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    public void showRebootDialog(final Context context) {

        if (ManagerFactory.getPreferencesManager(context).getFlashQueueSize() == 0)
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

                if (cursor.isBackup()) {
                    showBackupDialog(context, false, cursor.isWipeSystem(), cursor.isWipeData(),
                            cursor.isWipeCaches(), cursor.isFixPermissions());
                } else {
                    reboot(context, cursor.isWipeSystem(), cursor.isWipeData(),
                            cursor.isWipeCaches(), cursor.isFixPermissions(), null, null, null);
                }

            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void reboot(Context context, boolean wipeSystem, boolean wipeData, boolean wipeCaches,
            boolean fixPermissions, String backupFolder, String backupOptions, String restore) {
        reboot(context, wipeSystem, wipeData, wipeCaches, fixPermissions, backupFolder,
                backupOptions, restore, false);
    }

    private void reboot(Context context, final boolean wipeSystem, final boolean wipeData,
            final boolean wipeCaches, final boolean fixPermissions, final String backupFolder,
            final String backupOptions, final String restore, final boolean skipCommands) {

        if (wipeSystem) {// && ManagerFactory.getPreferencesManager().isShowSystemWipeAlert()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle(R.string.alert_wipe_system_title);
            alert.setMessage(R.string.alert_wipe_system_message);

            alert.setPositiveButton(R.string.alert_reboot_now,
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();

                            _reboot(wipeSystem, wipeData, wipeCaches, fixPermissions, backupFolder,
                                    backupOptions, restore, skipCommands);

                        }
                    });

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.show();
        } else {
            _reboot(wipeSystem, wipeData, wipeCaches, fixPermissions, backupFolder, backupOptions,
                    restore, skipCommands);
        }

    }

    private void _reboot(boolean wipeSystem, boolean wipeData, boolean wipeCaches,
            boolean fixPermissions, String backupFolder, String backupOptions, String restore,
            boolean skipCommands) {

        try {

            if (fixPermissions) {
                fixPermissions = prepareFixPermissions();
            }

            RecoveryManager manager = ManagerFactory.getRecoveryManager(mContext);

            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            os.writeBytes("rm -f /cache/recovery/command\n");
            os.writeBytes("rm -f /cache/recovery/extendedcommand\n");
            os.writeBytes("rm -f /cache/recovery/openrecoveryscript\n");

            if (!skipCommands) {

                String flashExtraFiles = Constants.getProperty(Constants.OVERLAY_BACKUP_FILES);
                String backupFiles = null;
                if (flashExtraFiles != null && !"".equals(flashExtraFiles)) {
                    backupFiles = prepareFlashFiles(flashExtraFiles);
                }

                String file = manager.getCommandsFile();

                String[] commands = manager.getCommands(wipeSystem, wipeData, wipeCaches,
                        fixPermissions, backupFolder, backupOptions, restore, backupFiles);
                if (commands != null) {
                    int size = commands.length, i = 0;
                    for (; i < size; i++) {
                        os.writeBytes("echo '" + commands[i] + "' >> /cache/recovery/" + file
                                + "\n");
                    }
                }
            }

            os.writeBytes("/system/bin/touch /cache/recovery/boot\n");
            os.writeBytes("reboot recovery\n");

            ManagerFactory.getFileManager(mContext).clearItems();
            ManagerFactory.getPreferencesManager(mContext).setFlashQueue(null);

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

    private boolean prepareFixPermissions() {

        FileManager fManager = ManagerFactory.getFileManager(mContext);

        String data = fManager.readAssets(mContext, "fix_permissions.sh");
        File folder = mContext.getFilesDir();

        if (data != null
                && fManager.writeToFile(data, folder.getAbsolutePath(), "fix_permissions.sh")) {

            ManagerFactory.getSUManager(mContext).runWaitFor(
                    "cp " + folder.getAbsolutePath()
                            + "/fix_permissions.sh /cache/fix_permissions.sh");

            return true;
        }
        return false;
    }

    private String prepareFlashFiles(String flashExtraFiles) {

        String[] files = flashExtraFiles.split(",");
        if (files.length <= 0) {
            return null;
        }

        try {
            FileManager fManager = ManagerFactory.getFileManager(mContext);
            SUManager sManager = ManagerFactory.getSUManager(mContext);

            File folder = mContext.getFilesDir();
            folder.mkdirs();

            File inFile = new File(folder, "backup_files_tmp.zip");
            OutputStream out = new FileOutputStream(inFile);
            fManager.read(mContext.getResources().openRawResource(R.raw.files), out);

            folder = new File(folder, "backup/");
            fManager.recursiveDelete(folder);
            folder.mkdirs();

            String path = folder.getAbsolutePath();
            sManager.runWaitFor("chmod 777 " + path);

            List<File> backupFiles = new ArrayList<File>();

            for (int i = 0; i < files.length; i++) {
                File file = new File(files[i]);
                String filePath = file.getAbsolutePath();
                filePath = filePath.substring(0, filePath.lastIndexOf(File.separator));
                File fileFolder = new File(path + filePath);
                fileFolder.mkdirs();
                filePath = fileFolder + "/" + file.getName();
                sManager.runWaitFor("cp " + files[i] + " " + filePath);
                sManager.runWaitFor("chmod 777 " + filePath);
                backupFiles.add(new File(filePath));
            }

            String dlPath = ManagerFactory.getPreferencesManager(mContext).getDownloadPath();

            File finalFile = new File(dlPath, "backup_files.zip");
            fManager.addFilesToZip(inFile, finalFile,
                    backupFiles.toArray(new File[backupFiles.size()]), folder.getAbsolutePath());

            return finalFile.getAbsolutePath();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}