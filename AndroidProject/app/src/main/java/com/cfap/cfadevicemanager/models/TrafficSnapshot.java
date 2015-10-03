/***
 * Copyright (c) 2008-2011 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 * <p>
 * From _Tuning Android Applications_
 * http://commonsware.com/AndTuning
 */

package com.cfap.cfadevicemanager.models;

import android.content.Context;
import android.content.pm.ApplicationInfo;
<<<<<<< HEAD
import org.joda.time.DateTime;
=======

import org.joda.time.DateTime;

>>>>>>> 250970c2e8e3780a150eb8aca12cfa0e24e91c94
import java.util.HashMap;
import java.util.Map;

public class TrafficSnapshot {

    private Map<Integer, AppTrafficSnapshot> apps;
    private DateTime timeStamp;

    private TrafficSnapshot(Context context) {
        apps = new HashMap<>();
        for (ApplicationInfo app :
                context.getPackageManager().getInstalledApplications(0)) {
            apps.put(app.uid, new AppTrafficSnapshot(app.uid,app.packageName,app.icon));
        }

        timeStamp = DateTime.now();
    }

    public static TrafficSnapshot getCurrentTrafficSnapshot(Context context) {
        return new TrafficSnapshot(context);
    }

    public String getDay() {
        return timeStamp.toString("dd-MMM-yyyy");
    }

    public Map<Integer, AppTrafficSnapshot> getApps() {
        return apps;
    }

    public AppTrafficSnapshot getSnapshot(int uid) {
        return apps.get(uid);
    }
}