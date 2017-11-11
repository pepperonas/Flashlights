/*
 * Copyright (c) 2017 Martin Pfeffer
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

package io.celox.app.flashlights;

import android.app.Application;

import com.pepperonas.aespreferences.AesPrefs;
import com.pepperonas.andbasx.AndBasx;
import com.pepperonas.jbasx.log.Log;

import java.util.Date;

import io.celox.app.flashlights.utils.AesConst;
import io.celox.app.flashlights.utils.Utils;

/**
 * @author Martin Pfeffer <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class App extends Application {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        AesPrefs.init(this, AesConst.AES_PREFS_FILE_NAME,
                "XgLfNEIn@E2uVUg69E*wQ!sgsBLO&S$UYEZ", AesPrefs.LogMode.ALL);

        Date d = new Date();
        d.setTime(System.currentTimeMillis() - 100000);

        AndBasx.init(this);
        AesPrefs.initInstallationDate();
        AesPrefs.initOrIncrementLaunchCounter();

        AesPrefs.putLong(AesConst.APP_STARTED, System.currentTimeMillis());

        Log.i(TAG, "onCreate: App started... " + Utils.getReadableTimeStamp(AesPrefs.getLong(AesConst.APP_STARTED, System.currentTimeMillis())));
    }
}
