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

public class TrafficRecord {
	public long tx=0;
	public long rx=0;
	public String tag=null;
    long totTx=0;
    long totRx=0;
	long wifiTx=0;
	long wifiRx=0;
	long dataTx=0;
	long dataRx=0;
	
	TrafficRecord() {
		tx= TrafficStats.getTotalTxBytes();
		rx= TrafficStats.getTotalRxBytes();

        totTx= TrafficStats.getTotalTxBytes();
        totRx= TrafficStats.getTotalRxBytes();
        dataTx= TrafficStats.getMobileTxBytes();
        dataRx= TrafficStats.getMobileTxBytes();
        wifiTx= totTx-dataTx;
        wifiRx= totRx-dataRx;
	}
	
	TrafficRecord(int uid, String tag) {
		tx= TrafficStats.getUidTxBytes(uid);
		rx= TrafficStats.getUidRxBytes(uid);
		this.tag=tag;

        totTx= TrafficStats.getTotalTxBytes();
        totRx= TrafficStats.getTotalRxBytes();
		dataTx= TrafficStats.getMobileTxBytes();
		dataRx= TrafficStats.getMobileTxBytes();
		wifiTx= totTx-dataTx;
		wifiRx= totRx-dataRx;
	}
}