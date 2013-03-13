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

import com.beerbong.gooupdater.util.HttpStringReader.HttpStringReaderListener;
import com.beerbong.gooupdater.util.URLStringReader.URLStringReaderListener;

public interface Updater extends URLStringReaderListener, HttpStringReaderListener {

    public class RomInfo {

        public int id;
        public String filename;
        public String path;
        public String folder;
        public String md5;
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
        public long version;
        public int gapps_package;
        public int incremental_file;
    }

    public static final String PROPERTY_DEVICE = "ro.product.device";

    public static interface UpdaterListener {

        public void versionFound(RomInfo info);

        public void versionError(String error);
    }

    public String getDeveloperId();

    public String getRomName();

    public int getRomVersion();

    public void searchVersion();

    public boolean isScanning();
}