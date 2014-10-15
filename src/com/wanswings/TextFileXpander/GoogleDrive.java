//
//  GoogleDrive.java
//  TextFileXpander
//
//  Created by wanswings on 2014/10/05.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class GoogleDrive extends Storage {

	private static final String MIME_FOLDER = "application/vnd.google-apps.folder";
	private static final String MIME_TEXT_PLAIN = "text/plain";

	private String classNameForLog;
	private AuthCommon common;
	private List<File> mFileList;

	public GoogleDrive(Context ct, boolean refresh, StorageListenerInterface listener) {
		super(ct, refresh, listener);

		classNameForLog = this.getClass().getName() + "...";
		common = (AuthCommon)ct.getApplicationContext();
		mFileList = new ArrayList<File>();
		storageListener.readyToStartGoogleAuthActivity();
	}

	@Override
	public boolean onNewIntent(Intent intent) {
		boolean result = true;

		if (!isStorageAvailable()) {
			// offline
			result = false;
		}
		else if (intent != null) {
			boolean fromGoogle = intent.getBooleanExtra(GoogleAuthActivity.EXTRA_FROM_GOOGLE, false);
			Log.i(packageName, classNameForLog + GoogleAuthActivity.EXTRA_FROM_GOOGLE + "..." + fromGoogle);
			if (fromGoogle) {
				result = intent.getBooleanExtra(GoogleAuthActivity.EXTRA_RESULT_GOOGLE, false);
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
		common.mDriveService = null;
		common.mGoogleCredential = null;

		super.close();
	}

	@Override
	protected List<String> getEntriesAPI(boolean isDir, String tPath) {
		List<String> result = new ArrayList<String>();

		String parent = null;
		for (File f : mFileList) {
			if (tPath.endsWith(f.getTitle())) {
				parent = "'" + f.getId() + "'";
				String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_GOOGLE);
				prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_GOOGLE, new String[]{keys[0], parent});
				break;
			}
		}
		if (parent == null) {
			parent = "'" + common.mDriveRootFolderId + "'";
			if (isRefresh) {
				String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_GOOGLE);
				if (keys != null) {
					parent = keys[1];
				}
			}
		}
		mFileList.clear();

		if (isStorageAvailable() && common.mGoogleCredential != null && common.mDriveService != null) {
			StringBuffer sb = new StringBuffer();
			sb.append(parent);
			sb.append(" in parents and trashed=false and mimeType = '");
			if (isDir) {
				sb.append(MIME_FOLDER);
			}
			else {
				sb.append(MIME_TEXT_PLAIN);
			}
			sb.append("'");

			try {
				Files.List request = common.mDriveService.files().list().setQ(sb.toString());
				do {
					FileList files = request.execute();
					mFileList.addAll(files.getItems());
					request.setPageToken(files.getNextPageToken());
				} while (request.getPageToken() != null && request.getPageToken().length() > 0);

				for (File f : mFileList) {
					if (isDir) {
						result.add(f.getTitle());
					}
					else {
						if (f.getTitle().toLowerCase(Locale.ENGLISH).endsWith(TEXT_FILE_EXTENSION)) {
							result.add(f.getTitle());
						}
					}
				}
			}
			catch (IOException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
		}
		return result;
	}

	@Override
	protected boolean getFileAPI(String fpath, String fname) {
		boolean result = false;

		File file = null;
		for (File f : mFileList) {
			if (fname.equals(f.getTitle())) {
				file = f;
				break;
			}
		}

		if (isStorageAvailable() && file != null &&
						file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
			InputStream is = null;
			FileOutputStream fos = null;
			try {
				HttpResponse resp = common.mDriveService.getRequestFactory()
								.buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
				is = resp.getContent();
				fos = mContext.openFileOutput(fname, Context.MODE_PRIVATE);
				byte[] readBytes = new byte[1024];
				int size = 0;
				while ((size = is.read(readBytes)) > 0) {
					fos.write(readBytes, 0, size);
				}
				result = true;
			}
			catch (IOException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
			finally {
				try {
					if (fos != null) fos.close();
					if (is != null) is.close();
				}
				catch (IOException e) {
					Log.e(packageName, classNameForLog + e.toString());
				}
			}
		}

		return result;
	}
}
