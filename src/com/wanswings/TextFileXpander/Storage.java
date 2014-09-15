//
//  Storage.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class Storage {

	protected static final String TEXT_FILE_EXTENSION = ".txt";

	protected Context mContext;
	protected boolean isRefresh;
	protected String packageName;
	protected StorageListenerInterface storageListener;
	protected PrivateSharedPrefs prefs;
	protected String topPath;
	private String currentPath;
	private String classNameForLog;
	private int selectedPathIdx;
	private ProgressDialog mProgDialog;

	public Storage(Context ct, boolean refresh, StorageListenerInterface listener) {
		mContext = ct;
		isRefresh = refresh;
		storageListener = listener;

		packageName = mContext.getPackageName();
		classNameForLog = this.getClass().getName() + "...";
		topPath = "";	// Root
		prefs = new PrivateSharedPrefs(ct, PrivateSharedPrefs.SAVE_PREFS_NAME_STORAGE);
		String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_STORAGE);
		if (keys != null) {
			currentPath = keys[0];
		}
		else {
			currentPath = "";
		}
	}

	public boolean onNewIntent(Intent intent) {
		return true;
	}

	public boolean isStorageAvailable() {
		return true;
	}

	public void close() {
	}

	protected void selectDir() {
		if (!isStorageAvailable()) {
			return;
		}
		if (isRefresh) {
			getFiles();
			return;
		}

		GetEntries task = new GetEntries(true) {
			@Override
			protected void onPostExecute(List<String> result) {
				super.onPostExecute(result);

				if (result.size() > 0) {
					final String[] dirs = (String[])result.toArray(new String[0]);
					Arrays.sort(dirs);
					selectedPathIdx = -1;
					int selected = -1;
					for (int i = 0; i < dirs.length; i++) {
						if (currentPath.equals(topPath + File.separator + dirs[i])) {
							selectedPathIdx = selected = i;
							break;
						}
					}
					new AlertDialog.Builder(mContext)
					.setTitle(R.string.dialog_title_select_dir)
					.setSingleChoiceItems(dirs, selected, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int idx) {
							selectedPathIdx = idx;
						}
					})
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (selectedPathIdx >= 0) {
								currentPath = topPath + File.separator + dirs[selectedPathIdx];
								prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_STORAGE, new String[]{currentPath});
								getFiles();
							}
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							storageListener.cancelSelectDirDialog();
						}
					})
					.show();
				}
				else {
					// no directory
					if (topPath.equals("")) {
						// root
						currentPath = File.separator;
					}
					else {
						currentPath = topPath;
					}
					prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_STORAGE, new String[]{currentPath});
					getFiles();
				}
			}
		};
		task.execute(topPath + File.separator);
	}

	protected void getFiles() {
		if (!isStorageAvailable() || currentPath.equals("")) {
			return;
		}

		GetEntries task = new GetEntries(false) {
			@Override
			protected void onPostExecute(List<String> result) {
				super.onPostExecute(result);

				deleteLocalFiles();
				GetFiles task2 = new GetFiles(currentPath, result) {
					@Override
					protected void onPostExecute(Boolean result2) {
						super.onPostExecute(result2);
						storageListener.readyToReadPrivateFiles();
					}
				};
				task2.execute();
			}
		};
		task.execute(currentPath);
	}

	// for local storage
	protected List<String> getEntriesAPI(boolean isDir, String tPath) {
		List<String> result = new ArrayList<String>();
		File dir = new File(tPath);
		File[] entries = dir.listFiles();
		if (entries != null) {
			for (final File entry : entries) {
				if (isDir && entry.isDirectory()) {
					result.add(entry.getName());
				}
				else if (!isDir && entry.isFile()) {
					if (entry.getName().toLowerCase(Locale.ENGLISH).endsWith(TEXT_FILE_EXTENSION)) {
						result.add(entry.getName());
					}
				}
			}
		}
		return result;
	}

	// for local storage
	protected boolean getFileAPI(String fpath, String fname) {
		boolean result = false;

		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(fpath + File.separator + fname);
			fos = mContext.openFileOutput(fname, Context.MODE_PRIVATE);
			byte[] readBytes = new byte[fis.available()];
			int size = 0;
			while ((size = fis.read(readBytes)) > 0) {
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
				if (fis != null) fis.close();
			}
			catch (IOException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
		}
		return result;
	}

	private void deleteLocalFiles() {
		String[] fileList = mContext.fileList();
		for (String fname: fileList) {
			mContext.deleteFile(fname);
		}
	}

	private class GetEntries extends AsyncTask<String, Void, List<String>> {

		private boolean isDir;

		GetEntries(boolean isDir) {
			super();
			this.isDir = isDir;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialog = ProgressDialog.show(mContext, "", mContext.getString(R.string.dialog_loading_entries));
		}

		@Override
		protected List<String> doInBackground(String... params) {
			String tPath = params[0];
			return getEntriesAPI(isDir, tPath);
		}

		@Override
		protected void onPostExecute(List<String> result) {
			mProgDialog.dismiss();
		}
	}

	private class GetFiles extends AsyncTask<Void, Void, Boolean> {

		private String fpath;
		private List<String> entries;

		GetFiles(String fpath, List<String> entries) {
			super();
			this.fpath = fpath;
			this.entries = entries;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialog = ProgressDialog.show(mContext, "", mContext.getString(R.string.dialog_loading_files));
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean result = false;
			for (final String fname : entries) {
				result = getFileAPI(fpath, fname);
				if (!result) {
					break;
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mProgDialog.dismiss();
		}
	}
}
