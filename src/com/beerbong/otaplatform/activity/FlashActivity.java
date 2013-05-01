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

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.manager.PreferencesManager;
import com.beerbong.otaplatform.ui.UI;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.FileItem;
import com.beerbong.otaplatform.widget.TouchInterceptor;

public class FlashActivity extends FragmentActivity implements OnItemClickListener {

    private static final int REQUEST_PICK_FILE = 203;

    private class FileItemsAdapter extends ArrayAdapter<FileItem> {

        public FileItemsAdapter() {
            super(FlashActivity.this, R.layout.order_item, ManagerFactory.getFileManager(FlashActivity.this)
                    .getFileItems());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout itemView;
            FileItem item = getItem(position);

            if (convertView == null) {
                itemView = new LinearLayout(getContext());
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                vi.inflate(R.layout.order_item, itemView, true);
            } else {
                itemView = (LinearLayout) convertView;
            }
            TextView title = (TextView) itemView.findViewById(R.id.title);
            TextView summary = (TextView) itemView.findViewById(R.id.summary);

            title.setText(item.getName());
            summary.setText(item.getPath());

            return itemView;
        }

    }

    private Button mButtonFlash;
    private Button mButtonAdd;
    private Button mButtonClear;
    private TouchInterceptor mFileList;

    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {

        public void drop(int from, int to) {
            if (from == to)
                return;
            List<FileItem> items = ManagerFactory.getFileManager(FlashActivity.this).getFileItems();
            FileItem toMove = items.get(from);
            while (items.indexOf(toMove) != to) {
                int i = items.indexOf(toMove);
                Collections.swap(items, i, to < from ? i - 1 : i + 1);
            }
            ManagerFactory.getPreferencesManager(FlashActivity.this).setFlashQueue(items);
            redrawItems();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PreferencesManager pManager = ManagerFactory.getPreferencesManager(FlashActivity.this);

        boolean useDarkTheme = pManager.isDarkTheme();
        setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.flash_activity);

        mButtonFlash = (Button) findViewById(R.id.button_flash_now);
        mButtonAdd = (Button) findViewById(R.id.button_add_zip);
        mButtonClear = (Button) findViewById(R.id.button_clear_queue);

        mButtonFlash.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ManagerFactory.getRebootManager(FlashActivity.this).showRebootDialog(FlashActivity.this);
            }
        });

        mButtonAdd.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PackageManager packageManager = getPackageManager();
                Intent test = new Intent(Intent.ACTION_GET_CONTENT);
                test.setType("file/*");
                List<ResolveInfo> list = packageManager.queryIntentActivities(test,
                        PackageManager.GET_ACTIVITIES);
                if (list.size() > 0) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                    intent.setType("file/*");
                    startActivityForResult(intent, REQUEST_PICK_FILE);
                } else {
                    // No app installed to handle the intent - file explorer
                    // required
                    Toast.makeText(FlashActivity.this, R.string.install_file_manager_error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ManagerFactory.getFileManager(FlashActivity.this).clearItems();
                ManagerFactory.getPreferencesManager(FlashActivity.this).setFlashQueue(null);
                UI.getInstance().onListChanged();
                redrawItems();
            }
        });

        mFileList = (TouchInterceptor) findViewById(R.id.file_list);
        mFileList.setOnItemClickListener(this);
        mFileList.setDropListener(mDropListener);

        redrawItems();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        List<FileItem> items = ManagerFactory.getFileManager(FlashActivity.this).getFileItems();
        FileItem item = items.get(position);
        showInfoDialog(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_PICK_FILE) {
            if (data == null) {
                // Nothing returned by user, probably pressed back button in
                // file manager
                return;
            }

            String filePath = data.getData().getPath();

            addItem(filePath);

        }

    }

    private void redrawItems() {
        List<FileItem> items = ManagerFactory.getFileManager(FlashActivity.this).getFileItems();
        mFileList.setAdapter(new FileItemsAdapter());
        mButtonFlash.setEnabled(items.size() > 0);
        mButtonClear.setEnabled(items.size() > 0);
        UI.getInstance().onListChanged();
    }

    private void showInfoDialog(final FileItem item) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getResources().getString(R.string.alert_file_title,
                new Object[] { item.getName() }));

        String path = item.getPath();
        File file = new File(path);

        alert.setMessage(getResources().getString(
                R.string.alert_file_summary,
                new Object[] {
                        (file.getParent() == null ? "" : file.getParent()) + "/",
                        Constants.formatSize(file.length()),
                        new Date(file.lastModified()).toString() }));

        alert.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.setNegativeButton(R.string.alert_file_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                ManagerFactory.getFileManager(FlashActivity.this).removeItem(item);
                redrawItems();
            }
        });

        alert.show();
    }

    private void addItem(String filePath) {

        if (ManagerFactory.getFileManager(FlashActivity.this).addItem(filePath)) {

            redrawItems();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.flash_menu, menu);

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
