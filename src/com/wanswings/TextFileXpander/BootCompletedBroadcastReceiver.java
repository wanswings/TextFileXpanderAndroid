//
//  SystemBroadcastReceiver.java
//  TextFileXpander
//
//  Created by wanswings on 2014/09/09.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
				intent.setClass(context, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}
		}
	}

}
