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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beerbong.otaplatform.R;

public class HeaderButton extends LinearLayout {

    public interface HeaderButtonListener {

        public void onHeaderButtonSelected(int id, boolean fire);
    }

    private Context mContext;
    private HeaderButtonListener mHeaderButtonListener;

    public HeaderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.header_button, this, true);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.HeaderButton,
                0, 0);

        String text;
        Drawable icon;
        try {
            text = a.getString(R.styleable.HeaderButton_text);
            icon = a.getDrawable(R.styleable.HeaderButton_icon);
        } finally {
            a.recycle();
        }

        TextView textView = (TextView) view.findViewById(R.id.text);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);

        textView.setText(text);
        imageView.setImageDrawable(icon);

        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        select(true);
                        break;
                }
                return true;
            }
        });
    }

    public void select(boolean fire) {
        TypedValue typedValue = new TypedValue(); 
        ((Activity)mContext).getTheme().resolveAttribute(R.attr.headerButtonBackground, typedValue, true);
        try {
            setBackground(mContext.getResources().getDrawable(typedValue.resourceId));
        } catch (Throwable t) {
            setBackgroundColor(typedValue.resourceId);
        }
        if (mHeaderButtonListener != null) {
            mHeaderButtonListener.onHeaderButtonSelected(getId(), fire);
        }
    }

    public void unselect() {
        this.setBackgroundColor(android.R.color.transparent);
    }

    public HeaderButtonListener getHeaderButtonListener() {
        return mHeaderButtonListener;
    }

    public void setHeaderButtonListener(HeaderButtonListener mHeaderButtonListener) {
        this.mHeaderButtonListener = mHeaderButtonListener;
    }
}
