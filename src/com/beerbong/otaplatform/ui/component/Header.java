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

package com.beerbong.otaplatform.ui.component;

import java.util.ArrayList;
import java.util.List;

import com.beerbong.otaplatform.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class Header extends LinearLayout implements HeaderButton.HeaderButtonListener {

    public interface HeaderChangeListener {

        public void onHeaderChange(int id);
    }

    private List<HeaderButton> mButtons = new ArrayList<HeaderButton>();
    private HeaderChangeListener mListener;

    public Header(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.header, this, true);

        addButton(view, R.id.button_update);
        addButton(view, R.id.button_flash);
        addButton(view, R.id.button_settings);
    }

    private void addButton(View view, int id) {
        HeaderButton button = (HeaderButton) view.findViewById(id);
        button.setHeaderButtonListener(this);
        mButtons.add(button);
    }

    @Override
    public void onHeaderButtonSelected(int id, boolean fire) {
        for (int i = 0; i < mButtons.size(); i++) {
            if (mButtons.get(i).getId() != id) {
                mButtons.get(i).unselect();
            }
        }
        if (mListener != null && fire) {
            mListener.onHeaderChange(id);
        }
    }

    public void select(int index) {
        select(index, true);
    }

    public void select(int index, boolean fire) {
        mButtons.get(index).select(fire);
    }
    
    public HeaderChangeListener getHeaderChangeListener() {
        return mListener;
    }

    
    public void setHeaderChangeListener(HeaderChangeListener mListener) {
        this.mListener = mListener;
    }
}
