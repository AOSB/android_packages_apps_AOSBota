/*
 * Copyright (C) 2013 GooUpdater
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

package com.beerbong.gooupdater.updater;

import org.json.JSONObject;

import com.beerbong.gooupdater.updater.Updater.PackageInfo;

public class GooPackage extends PackageInfo {

    public int id;
    public String type;
    public String description;
    public int is_flashable;
    public long modified;
    public int downloads;
    public int status;
    public String additional_info;
    public String short_url;
    public int developer_id;
    public String developerid;
    public String board;
    public String rom;
    public int gapps_package;
    public int incremental_file;

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
}