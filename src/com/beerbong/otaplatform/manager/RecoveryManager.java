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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.util.FileItem;
import com.beerbong.otaplatform.util.RecoveryInfo;

public class RecoveryManager extends Manager {

    private SparseArray<RecoveryInfo> recoveries = new SparseArray<RecoveryInfo>();

    protected RecoveryManager(Context context) {
        super(context);

        if (ManagerFactory.getFileManager().hasExternalStorage()) {
            recoveries.put(R.id.cwmbased, new RecoveryInfo(R.id.cwmbased, "cwmbased", "emmc",
                    "sdcard"));
            recoveries.put(R.id.twrp, new RecoveryInfo(R.id.twrp, "twrp", "emmc", "sdcard"));
            recoveries.put(R.id.fourext,
                    new RecoveryInfo(R.id.fourext, "fourext", "emmc", "sdcard"));
        } else {
            recoveries.put(R.id.cwmbased, new RecoveryInfo(R.id.cwmbased, "cwmbased", "sdcard",
                    "sdcard"));
            recoveries.put(R.id.twrp, new RecoveryInfo(R.id.twrp, "twrp", "sdcard", "sdcard"));
            recoveries.put(R.id.fourext, new RecoveryInfo(R.id.fourext, "fourext", "sdcard",
                    "sdcard"));
        }

        if (!ManagerFactory.getPreferencesManager().existsRecovery()) {
            test(R.id.fourext);
        }
    }

    public void selectRecovery(Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.recovery,
                (ViewGroup) activity.findViewById(R.id.recovery_layout));

        RadioButton cbCwmbased = (RadioButton) view.findViewById(R.id.cwmbased);
        RadioButton cbTwrp = (RadioButton) view.findViewById(R.id.twrp);
        RadioButton cb4ext = (RadioButton) view.findViewById(R.id.fourext);

        final RadioGroup mGroup = (RadioGroup) view.findViewById(R.id.recovery_radio_group);

        RecoveryInfo info = getRecovery();
        switch (info.getId()) {
            case R.id.cwmbased:
                cbCwmbased.setChecked(true);
                break;
            case R.id.twrp:
                cbTwrp.setChecked(true);
                break;
            case R.id.fourext:
                cb4ext.setChecked(true);
                break;
        }

        new AlertDialog.Builder(activity).setTitle(R.string.recovery_alert_title)
                .setMessage(R.string.recovery_alert_summary).setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        int id = mGroup.getCheckedRadioButtonId();

                        setRecovery(id);

                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public void selectSdcard(final Activity activity, final boolean internal) {
        final PreferencesManager pManager = ManagerFactory.getPreferencesManager();

        final EditText input = new EditText(activity);
        input.setText(internal ? pManager.getInternalStorage() : pManager.getExternalStorage());

        new AlertDialog.Builder(activity)
                .setTitle(R.string.sdcard_alert_title)
                .setMessage(R.string.sdcard_alert_summary)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();

                        if (value == null || "".equals(value.trim())) {
                            Toast.makeText(activity, R.string.sdcard_alert_error,
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            return;
                        }

                        if (value.startsWith("/")) {
                            value = value.substring(1);
                        }

                        if (internal) {
                            pManager.setInternalStorage(value);
                        } else {
                            pManager.setExternalStorage(value);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public RecoveryInfo getRecovery() {
        String recovery = ManagerFactory.getPreferencesManager().getRecovery();
        for (int i = 0; i < recoveries.size(); i++) {
            int key = recoveries.keyAt(i);
            RecoveryInfo info = recoveries.get(key);
            if (info.getName().equals(recovery)) {
                return info;
            }
        }
        return null;
    }

    public void setRecovery(int id) {
        RecoveryInfo info = recoveries.get(id);
        ManagerFactory.getPreferencesManager().setRecovery(info.getName());
        ManagerFactory.getPreferencesManager().setInternalStorage(info.getInternalSdcard());
        ManagerFactory.getPreferencesManager().setExternalStorage(info.getExternalSdcard());
    }

    public String getCommandsFile() {

        RecoveryInfo info = getRecovery();

        switch (info.getId()) {
            case R.id.cwmbased:
            case R.id.fourext:
                return "extendedcommand";
            case R.id.twrp:
                return "openrecoveryscript";
            default:
                return null;
        }
    }

    public String[] getCommands(boolean wipeData, boolean wipeCaches) throws Exception {
        List<String> commands = new ArrayList<String>();

        List<FileItem> items = ManagerFactory.getFileManager().getFileItems();
        int size = items.size(), i = 0;

        RecoveryInfo info = getRecovery();

        String internalStorage = ManagerFactory.getPreferencesManager().getInternalStorage();

        switch (info.getId()) {
            case R.id.cwmbased:
            case R.id.fourext:

                commands.add("ui_print(\"-------------------------------------\");");
                commands.add("ui_print(\" OTAPlatform "
                        + mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName
                        + "\");");
                commands.add("ui_print(\"-------------------------------------\");");

                if (wipeData) {
                    commands.add("ui_print(\" Wiping data\");");
                    commands.add("format(\"/data\");");
                    commands.add("ui_print(\" Wiping android secure\");");
                    commands.add("format(\"/" + internalStorage + "/.android_secure\");");
                }
                if (wipeCaches) {
                    commands.add("ui_print(\" Wiping cache\");");
                    commands.add("format(\"/cache\");");
                    commands.add("ui_print(\" Wiping dalvik cache\");");
                    commands.add("format(\"/data/dalvik-cache\");");
                    commands.add("format(\"/cache/dalvik-cache\");");
                    commands.add("format(\"/sd-ext/dalvik-cache\");");
                }

                if (size > 0) {
                    for (; i < size; i++) {
                        FileItem item = items.get(i);
                        commands.add("ui_print(\" Installing zip\");");
                        commands.add("assert(install_zip(\"" + item.getKey() + "\"));");
                    }
                }

                commands.add("ui_print(\" Rebooting\");");
                break;

            case R.id.twrp:

                if (wipeData) {
                    commands.add("wipe data");
                }
                if (wipeCaches) {
                    commands.add("wipe cache");
                    commands.add("wipe dalvik");
                }

                for (; i < size; i++) {
                    FileItem item = items.get(i);
                    commands.add("install " + item.getKey());
                }

                break;
        }

        return commands.toArray(new String[commands.size()]);
    }

    private void test(final int id) {

        String name = null, path = null;

        switch (id) {
            case R.id.fourext:
                name = mContext.getString(R.string.recovery_4ext);
                path = "/cache/4ext/";
                break;
            case R.id.twrp:
                name = mContext.getString(R.string.recovery_twrp);
                String sdcard = "sdcard";
                path = "/" + sdcard + "/TWRP/";
                break;
            case R.id.cwmbased:
                setRecovery(R.id.cwmbased);
                Toast.makeText(
                        mContext,
                        mContext.getString(R.string.recovery_changed,
                                mContext.getString(R.string.recovery_cwm)), Toast.LENGTH_LONG)
                        .show();
                return;
        }

        final String recoveryName = name;

        File folder = new File(path);
        if (folder.exists()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setTitle(R.string.recovery_change_alert_title);
            alert.setMessage(mContext.getString(R.string.recovery_change_alert_message,
                    recoveryName));
            alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                    setRecovery(id);
                    Toast.makeText(mContext,
                            mContext.getString(R.string.recovery_changed, recoveryName),
                            Toast.LENGTH_LONG).show();
                }
            });
            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    switch (id) {
                        case R.id.fourext:
                            test(R.id.twrp);
                            break;
                        case R.id.twrp:
                            test(R.id.cwmbased);
                            break;
                    }
                }
            });
            alert.show();
        } else {
            switch (id) {
                case R.id.fourext:
                    test(R.id.twrp);
                    break;
                case R.id.twrp:
                    test(R.id.cwmbased);
                    break;
            }
        }
    }
}