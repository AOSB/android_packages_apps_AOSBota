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

package com.beerbong.gooupdater;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;

import com.beerbong.gooupdater.manager.ManagerFactory;
import com.beerbong.gooupdater.updater.GappsUpdater;
import com.beerbong.gooupdater.updater.RomUpdater;

public class Service extends android.app.Service {

    private boolean mFirstRun = true;
    private int mFirstRunCount = 0;

    private class ServiceRunnable extends Thread {

        boolean running = true;

        @Override
        public void run() {
            while (running) {
                long sleepTime = ManagerFactory.getPreferencesManager(Service.this)
                        .getTimeNotifications();
                if (running) {
                    if (ManagerFactory.getPreferencesManager(Service.this).isAcceptNotifications()) {
                        if (isNetworkAvailable()) {
                            if (sleepTime > 0) {
                                mRomUpdater.check();
                                mGappsUpdater.check();
                            }
                            mFirstRun = false;
                        }
                        try {
                            sleep(mFirstRun ? 1000 * 30 : sleepTime > 0 ? sleepTime : 1000 * 60 * 60);
                        } catch (Exception ex) {
                        }
                        if (mFirstRun) {
                            mFirstRunCount++;
                            if (mFirstRunCount >= 3) {
                                mFirstRun = false;
                            }
                        }
                    }
                }
            }
        }
    };

    public class LocalBinder extends Binder {

        Service getService() {
            return Service.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private RomUpdater mRomUpdater;
    private GappsUpdater mGappsUpdater;
    private ServiceRunnable mServiceThread;

    @Override
    public void onCreate() {
        super.onCreate();

        mRomUpdater = new RomUpdater(this, null, true);
        mGappsUpdater = new GappsUpdater(this, null, true);
        mServiceThread = new ServiceRunnable();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            mServiceThread.start();
        } catch (Exception ex) {
            // already running?
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronized (mServiceThread) {
            mServiceThread.running = false;
            mServiceThread.notify();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
