package com.beerbong.otaplatform.util;

import java.io.Serializable;
import java.util.List;

import com.beerbong.otaplatform.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;

public class RequestFileActivity extends Activity {

    private static final int REQUEST_PICK_FILE = 203;

    public interface RequestFileCallback extends Serializable {

        public void fileRequested(String filePath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PackageManager packageManager = getPackageManager();
        Intent test = new Intent(Intent.ACTION_GET_CONTENT);
        test.setType("file/*");
        List<ResolveInfo> list = packageManager.queryIntentActivities(test,
                PackageManager.GET_ACTIVITIES);
        if (list.size() > 0) {
            Intent intent = new Intent();
            intent.setType("file/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, REQUEST_PICK_FILE);
        } else {
            // No app installed to handle the intent - file explorer
            // required
            Toast.makeText(this, R.string.install_file_manager_error, Toast.LENGTH_SHORT).show();
            finish();
        }
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

            Constants.fileRequested(filePath);

        }
        finish();
    }
}
