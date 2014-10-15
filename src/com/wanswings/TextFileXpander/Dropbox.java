//
//  Dropbox.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;

public class Dropbox extends Storage {

	private String classNameForLog;
	private AuthCommon common;

	public Dropbox(Context ct, boolean refresh, StorageListenerInterface listener) {
		super(ct, refresh, listener);

		classNameForLog = this.getClass().getName() + "...";
		common = (AuthCommon)ct.getApplicationContext();
		storageListener.readyToStartDropboxAuthActivity();
	}

	@Override
	public boolean onNewIntent(Intent intent) {
		boolean result = true;

		if (!isStorageAvailable()) {
			// offline
			result = false;
		}
		else if (intent != null) {
			boolean fromDropbox = intent.getBooleanExtra(DropboxAuthActivity.EXTRA_FROM_DROPBOX, false);
			Log.i(packageName, classNameForLog + DropboxAuthActivity.EXTRA_FROM_DROPBOX + "..." + fromDropbox);
			if (fromDropbox) {
				result = intent.getBooleanExtra(DropboxAuthActivity.EXTRA_RESULT_DROPBOX, false);
				if (result) {
					selectDir();
				}
			}
		}
		return result;
	}

	@Override
	public boolean isStorageAvailable() {
		boolean result = false;

		ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null && info.isConnected()) {
			// online
			result = true;
		}
		else {
			Toast.makeText(mContext, R.string.error_internet_not_available, Toast.LENGTH_LONG).show();
		}

		return result;
	}

	@Override
	public void close() {
		if (common.mDropboxAPI != null && common.mDropboxAPI.getSession().isLinked()) {
			common.mDropboxAPI.getSession().unlink();
		}
		common.mDropboxAPI = null;

		super.close();
	}

	@Override
	protected List<String> getEntriesAPI(boolean isDir, String tPath) {
		List<String> result = new ArrayList<String>();

		if (isStorageAvailable() && common.mDropboxAPI != null && common.mDropboxAPI.getSession().isLinked()) {
			try {
				Entry rootEntry = common.mDropboxAPI.metadata(tPath, 500, null, true, null);
				List<Entry> entries = rootEntry.contents;
				for (final Entry entry : entries) {
					if (isDir && entry.isDir) {
						result.add(entry.fileName());
					}
					else if (!isDir && !entry.isDir) {
						if (entry.fileName().toLowerCase(Locale.ENGLISH).endsWith(TEXT_FILE_EXTENSION)) {
							result.add(entry.fileName());
						}
					}
				}
			}
			catch (DropboxException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
		}
		return result;
	}

	@Override
	protected boolean getFileAPI(String fpath, String fname) {
		boolean result = false;

		if (isStorageAvailable() && common.mDropboxAPI != null && common.mDropboxAPI.getSession().isLinked()) {
			FileOutputStream fos = null;
			try {
				fos = mContext.openFileOutput(fname, Context.MODE_PRIVATE);
				common.mDropboxAPI.getFile(fpath + File.separator + fname, null, fos, null);
				result = true;
			}
			catch (DropboxException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
			catch (IOException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
			finally {
				try {
					if (fos != null) fos.close();
				}
				catch (IOException e) {
					Log.e(packageName, classNameForLog + e.toString());
				}
			}
		}
		return result;
	}
}
