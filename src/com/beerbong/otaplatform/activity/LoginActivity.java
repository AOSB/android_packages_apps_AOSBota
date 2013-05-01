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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.beerbong.otaplatform.R;
import com.beerbong.otaplatform.manager.ManagerFactory;
import com.beerbong.otaplatform.manager.PreferencesManager;
import com.beerbong.otaplatform.util.Constants;
import com.beerbong.otaplatform.util.URLStringReader;

public class LoginActivity extends Activity implements URLStringReader.URLStringReaderListener {

    private static final String URL = "http://goo-inside.me/salt";

    private ProgressDialog mDialog;
    private EditText mPassword;
    private EditText mUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        PreferencesManager pManager = ManagerFactory.getPreferencesManager(this);

        boolean useDarkTheme = pManager.isDarkTheme();
        setTheme(useDarkTheme ? R.style.Theme_Dark : R.style.Theme_Light);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);

        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String username = mUsername.getText() == null ? "" : mUsername.getText().toString();
                String password = mPassword.getText() == null ? "" : mPassword.getText().toString();
                mDialog = new ProgressDialog(LoginActivity.this);
                mDialog.setIndeterminate(true);
                mDialog.setMessage(getResources().getString(R.string.logging_in));
                mDialog.setCancelable(true);
                mDialog.setCanceledOnTouchOutside(true);
                mDialog.show();
                try {
                    new URLStringReader(LoginActivity.this).execute(URL + "&username="
                            + URLEncoder.encode(username, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    // should never get here
                }
            }
        });

        String login = pManager.getLogin();

        final Button btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setEnabled(login != null && !"".equals(login));
        btnLogout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ManagerFactory.getPreferencesManager(LoginActivity.this).setLogin("");
                btnLogout.setEnabled(false);
                Constants.showToastOnUiThread(LoginActivity.this, R.string.logged_out);
            }
        });
    }

    @Override
    public void onReadEnd(String buffer) {
        mDialog.dismiss();
        mDialog = null;
        if (buffer != null && buffer.length() == 32) {
            ManagerFactory.getPreferencesManager(this).setLogin(buffer);
            Constants.showToastOnUiThread(this, R.string.logged_in);
            finish();
        } else if (buffer != null) {
            Constants.showToastOnUiThread(this, R.string.logged_invalid);
        } else {
            Constants.showToastOnUiThread(this, R.string.logged_down);
        }
    }

    @Override
    public void onReadError(Exception ex) {
    }

}
