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

package com.beerbong.otaplatform.updater;

import java.io.Serializable;

import org.json.JSONObject;

import android.content.Context;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.updater.Updater.PackageInfo;

public class GooPackage implements PackageInfo, Serializable {

    private String md5 = null;
    private String filename = null;
    private String path = null;
    private String folder = null;
    private long version = -1;
    private int id;
    private String type;
    private String description;
    private int is_flashable;
    private long modified;
    private int downloads;
    private int status;
    private String additional_info;
    private String short_url;
    private int developer_id;
    private String developerid;
    private String board;
    private String rom;
    private int gapps_package;
    private int incremental_file;

    public GooPackage(JSONObject result) {
        if (result == null) {
            version = -1;
        } else {
            developerid = result.optString("ro_developerid");
            board = result.optString("ro_board");
            rom = result.optString("ro_rom");
            version = result.optInt("ro_version");
            id = result.optInt("id");
            filename = result.optString("filename");
            path = "http://goo.im" + result.optString("path");
            folder = result.optString("folder");
            md5 = result.optString("md5");
            type = result.optString("type");
            description = result.optString("description");
            is_flashable = result.optInt("is_flashable");
            modified = result.optLong("modified");
            downloads = result.optInt("downloads");
            status = result.optInt("status");
            additional_info = result.optString("additional_info");
            short_url = result.optString("short_url");
            developer_id = result.optInt("developer_id");
            gapps_package = result.optInt("gapps_package");
            incremental_file = result.optInt("incremental_file");
        }
    }

    @Override
    public String getMessage(Context context) {
        return context.getResources().getString(R.string.goo_package_description,
                new Object[] { filename, md5, folder, description });
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
        return folder;
    }

    @Override
    public long getVersion() {
        return version;
    }

    
    public int getId() {
        return id;
    }

    
    public String getType() {
        return type;
    }

    
    public String getDescription() {
        return description;
    }

    
    public int getIs_flashable() {
        return is_flashable;
    }

    
    public long getModified() {
        return modified;
    }

    
    public int getDownloads() {
        return downloads;
    }

    
    public int getStatus() {
        return status;
    }

    
    public String getAdditional_info() {
        return additional_info;
    }

    
    public String getShort_url() {
        return short_url;
    }

    
    public int getDeveloper_id() {
        return developer_id;
    }

    
    public String getDeveloperid() {
        return developerid;
    }

    
    public String getBoard() {
        return board;
    }

    
    public String getRom() {
        return rom;
    }

    
    public int getGapps_package() {
        return gapps_package;
    }

    
    public int getIncremental_file() {
        return incremental_file;
    }
}