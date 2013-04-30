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

import android.content.Context;
import android.database.AbstractCursor;

import com.beerbong.gooupdater.R;

public class InstallOptionsCursor extends AbstractCursor {

    private static final String[] INSTALL_OPTIONS = {"WIPEDATA", "WIPECACHES"};
    private static final String[] COLUMN_NAMES = { "_id", "TEXT", "CHECKED" };

    private String[] mOption;
    private String[] mText;
    private int[] mChecked;

    public InstallOptionsCursor(Context context) {
        mOption = new String[INSTALL_OPTIONS.length];
        mText = new String[INSTALL_OPTIONS.length];
        mChecked = new int[INSTALL_OPTIONS.length];
        for (int i = 0; i < INSTALL_OPTIONS.length; i++) {
            mOption[i] = INSTALL_OPTIONS[i];
            mText[i] = context.getResources().getString(
                    getText(INSTALL_OPTIONS[i]));
            mChecked[i] = 0;
        }
    }

    @Override
    public int getCount() {
        return INSTALL_OPTIONS.length;
    }

    @Override
    public String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @Override
    public String getString(int column) {
        switch (column) {
            case 0:
                return mOption[getPosition()];
            case 1:
                return mText[getPosition()];
        }
        return null;
    }

    @Override
    public short getShort(int column) {
        return 0;
    }

    @Override
    public int getInt(int column) {
        if (column == 2) {
            return mChecked[getPosition()];
        }
        return 0;
    }

    @Override
    public long getLong(int column) {
        return 0;
    }

    @Override
    public float getFloat(int column) {
        return 0;
    }

    @Override
    public double getDouble(int column) {
        return 0;
    }

    @Override
    public boolean isNull(int column) {
        return false;
    }

    public void setOption(int which, boolean isChecked) {
        mChecked[which] = isChecked ? 1 : 0;
    }

    public boolean isWipeData() {
        for (int i = 0; i < getCount(); i++) {
            if ("WIPEDATA".equals(mOption[i])) {
                return mChecked[i] == 1;
            }
        }
        return false;
    }

    public boolean isWipeCaches() {
        for (int i = 0; i < getCount(); i++) {
            if ("WIPECACHES".equals(mOption[i])) {
                return mChecked[i] == 1;
            }
        }
        return false;
    }

    public String getIsCheckedColumn() {
        return "CHECKED";
    }

    public String getLabelColumn() {
        return "TEXT";
    }

    private int getText(String option) {
        if ("WIPEDATA".equals(option)) {
            return R.string.wipe_data;
        } else if ("WIPECACHES".equals(option)) {
            return R.string.wipe_caches;
        }
        return -1;
    }
}