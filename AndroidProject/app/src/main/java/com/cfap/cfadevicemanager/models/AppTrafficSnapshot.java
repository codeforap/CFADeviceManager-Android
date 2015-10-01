/***
	Copyright (c) 2008-2011 CommonsWare, LLC
	Licensed under the Apache License, Version 2.0 (the "License"); you may not
	use this file except in compliance with the License. You may obtain a copy
	of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
	by applicable law or agreed to in writing, software distributed under the
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
	OF ANY KIND, either express or implied. See the License for the specific
	language governing permissions and limitations under the License.
	
	From _Tuning Android Applications_
		http://commonsware.com/AndTuning
*/

package com.cfap.cfadevicemanager.models;

import android.net.TrafficStats;

public class AppTrafficSnapshot {

	private String name;
	private int iconDrawable;
	private long appDataStamp;
	private int uid;

	AppTrafficSnapshot(int uid, String name, int iconDrawable) {
		this.uid = uid;
		this.name = name;
		this.iconDrawable = iconDrawable;
		appDataStamp = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getIconDrawable() {
		return iconDrawable;
	}

	public void setIconDrawable(int iconDrawable) {
		this.iconDrawable = iconDrawable;
	}

	public long getAppDataStamp() {
		return appDataStamp;
	}

	public void setAppDataStamp(long appDataStamp) {
		this.appDataStamp = appDataStamp;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}
}