//
//  ExternalStorage.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ExternalStorage extends Storage {

	private String classNameForLog;

	public ExternalStorage(Context ap, boolean refresh, StorageListenerInterface listener) {
		super(ap, refresh, listener);

		classNameForLog = this.getClass().getName() + "...";

		topPath = Environment.getExternalStorageDirectory().getPath();
		Log.i(packageName, classNameForLog + "getExternalStorageDirectory..." + topPath);

		selectDir();
	}

	@Override
	public boolean isStorageAvailable() {
		boolean result = false;

		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			result = true;
		}
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			result = true;
		}

		if (!result) {
			Toast.makeText(mContext, R.string.error_external_storage_not_ready, Toast.LENGTH_LONG).show();
		}

		return result;
	}
}
