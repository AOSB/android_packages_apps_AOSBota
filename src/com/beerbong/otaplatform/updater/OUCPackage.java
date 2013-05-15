package com.beerbong.otaplatform.updater;

import java.io.Serializable;

import org.json.JSONObject;

import android.content.Context;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.updater.Updater.PackageInfo;


public class OUCPackage implements PackageInfo, Serializable {

    private String md5 = null;
    private String filename = null;
    private String path = null;
    private String date = null;
    private String rom = null;
    private String changelog = null;
    private long version = -1;

    public OUCPackage(JSONObject result) throws Exception {
        if (result == null) {
            version = -1;
        } else {
            md5 = result.getString("md5");
            filename = result.getString("rom") + "-" + result.getString("date") + ".zip";
            path = result.getString("url");
            rom = result.getString("rom");
            changelog = result.getString("changelog");
            date = result.getString("date");
            String d = date.replace("-", "");
            version = Long.parseLong(d);
        }
    }

    @Override
    public boolean isDelta() {
        return false;
    }

    @Override
    public String getDeltaMd5() {
        return null;
    }

    @Override
    public String getDeltaPath() {
        return null;
    }

    @Override
    public String getMd5() {
        return md5;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFolder() {
        return null;
    }

    @Override
    public String getMessage(Context context) {
        return context.getResources().getString(R.string.ouc_package_description,
                new Object[] { filename, md5, date, changelog});
    }

    @Override
    public long getVersion() {
        return version;
    }

    
    public String getDate() {
        return date;
    }

    
    public String getRom() {
        return rom;
    }

    
    public String getChangelog() {
        return changelog;
    }

}
