//
//  DropboxAuthActivity.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class DropboxAuthActivity extends Activity {

	protected static final String EXTRA_FROM_DROPBOX = "fromDropbox";
	protected static final String EXTRA_RESULT_DROPBOX = "resultDropbox";

	private DropboxAPI<AndroidAuthSession> mAPI;
	private String packageName;
	private String classNameForLog;
	private PrivateSharedPrefs prefs;

	@Override
	protected void onDestroy() {
		Log.i(packageName, classNameForLog + "onDestroy start");
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		packageName = this.getPackageName();
		classNameForLog = this.getClass().getName() + "...";
		prefs = new PrivateSharedPrefs(this, PrivateSharedPrefs.SAVE_PREFS_NAME_STORAGE);
		Log.i(packageName, classNameForLog + "onCreate start");

		setContentView(R.layout.activity_dropbox);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(packageName, classNameForLog + "onResume start");

		if (isFinishing()) {
			return;
		}
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null || !info.isConnected()) {
			// offline
			Toast.makeText(DropboxAuthActivity.this, R.string.error_internet_not_available, Toast.LENGTH_LONG).show();
			mAPI = null;
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			finish();
		}
		else if (mAPI == null) {
			AppKeyPair appKeyPair = new AppKeyPair(getString(R.string.dropbox_app_key),
													getString(R.string.dropbox_app_secret));
			String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_DROPBOX);
			if (keys == null) {
				AndroidAuthSession session  = new AndroidAuthSession(appKeyPair);
				mAPI = new DropboxAPI<AndroidAuthSession>(session);
			}
			else {
				AndroidAuthSession session = new AndroidAuthSession(appKeyPair, new AccessTokenPair(keys[0], keys[1]));
				mAPI = new DropboxAPI<AndroidAuthSession>(session);
			}
			if (!mAPI.getSession().isLinked()) {
				mAPI.getSession().startAuthentication(DropboxAuthActivity.this);
			}
			else {
				mAPI = null;
				Intent intent = new Intent(DropboxAuthActivity.this, MainActivity.class);
				intent.putExtra(EXTRA_FROM_DROPBOX, true);
				intent.putExtra(EXTRA_RESULT_DROPBOX, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.setAction(Intent.ACTION_VIEW);
				startActivity(intent);
				finish();
			}
		}
		else {
			boolean result = true;
			AndroidAuthSession session = mAPI.getSession();
			if (session.authenticationSuccessful()) {
				Log.i(packageName, classNameForLog + "authenticationSuccessful");
				try {
					// Required to complete authentication, sets the access token on the session
					session.finishAuthentication();
					TokenPair tokens = session.getAccessTokenPair();
	
					String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_DROPBOX);
					if (keys == null || !keys[0].equals(tokens.key) || !keys[1].equals(tokens.secret)) {
						prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_DROPBOX, new String[]{tokens.key, tokens.secret});
					}
				}
				catch (IllegalStateException e) {
					Log.e(getPackageName(), classNameForLog + e.toString());
					result = false;
				}
			}
			else {
				result = false;
			}

			mAPI = null;
			Intent intent = new Intent(DropboxAuthActivity.this, MainActivity.class);
			intent.putExtra(EXTRA_FROM_DROPBOX, true);
			intent.putExtra(EXTRA_RESULT_DROPBOX, result);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			finish();
		}
	}
}
