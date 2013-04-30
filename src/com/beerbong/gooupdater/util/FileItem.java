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

package com.beerbong.gooupdater.util;

import java.io.File;

public class FileItem {

    private String key;
    private String name;
    private String path;

    public FileItem(String str) {
        String[] split = str.split("##");
        this.key = split[0];
        this.path = split[1];
        this.name = split[1].substring(split[1].lastIndexOf(File.separator) + 1);
    }
    public FileItem(String key, String name, String path) {
        this.key = key;
        this.name = name;
        this.path = path;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return this.path;
    }

    public String toString() {
        return key + "##" + path;
    }
}