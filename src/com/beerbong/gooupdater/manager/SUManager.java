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

package com.beerbong.gooupdater.manager;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import android.content.Context;

public class SUManager extends Manager {

    protected SUManager(Context context) {
        super(context);
    }

    public String execute(String command) {
        String result = null;

        try {
            Process p = Runtime.getRuntime().exec("su");
            DataInputStream in = new DataInputStream(p.getInputStream());
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes(command + "\n");
            out.flush();
            out.close();
            StringBuffer buffer = new StringBuffer();

            while (true) {
                String line = in.readLine();
                result = null;
                if (line == null) {
                    p.waitFor();
                    result = buffer.toString().trim();
                    break;
                }

                buffer.append(line + "\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }
}