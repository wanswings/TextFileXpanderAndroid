//
//  Storage.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrivateSharedPrefs {

	protected static final String SAVE_PREFS_NAME_MAIN = "MAIN";
	protected static final String[] SAVE_KEYS_FIRSTTIME = {"FIRSTTIME"};
	protected static final String[] SAVE_KEYS_MAIN = {"VIEWTYPE", "NOTIFICATION", "CURRENTSTORAGE"};
	protected static final String SAVE_PREFS_NAME_STORAGE = "STORAGE";
	protected static final String[] SAVE_KEYS_STORAGE = {"CURRENTPATH"};
	protected static final String[] SAVE_KEYS_DROPBOX = {"DROPBOXKEY", "DROPBOXSECRET"};

	private Context mContext;
	private String prefsName;

	PrivateSharedPrefs(Context ct, String name) {
		mContext = ct;
		prefsName = name;
	}

	public void clearAllKeys() {
		SharedPreferences prefs1 = mContext.getSharedPreferences(SAVE_PREFS_NAME_MAIN, Context.MODE_PRIVATE);
		Editor edit1 = prefs1.edit();
		edit1.clear();
		edit1.commit();
		SharedPreferences prefs2 = mContext.getSharedPreferences(SAVE_PREFS_NAME_STORAGE, Context.MODE_PRIVATE);
		Editor edit2 = prefs2.edit();
		edit2.clear();
		edit2.commit();
	}

	public String[] getKeys(String[] keys) {
		SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		String[] result = new String[keys.length];

		for (int i = 0; i < keys.length; i++) {
			String value = prefs.getString(keys[i], null);
			if (value == null) {
				result = null;
				break;
			}
			result[i] = value;
		}
		return result;
	}

	public void storeKeys(String[] keys, String[] values) {
		SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		Editor edit = prefs.edit();
		for (int i = 0; i < keys.length; i++) {
			if (values[i] != null) {
				edit.putString(keys[i], values[i]);
			}
		}
		edit.commit();
	}

	public void clearKeys() {
		SharedPreferences prefs = mContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}
}
