//
//  GoogleAuthActivity.java
//  TextFileXpander
//
//  Created by wanswings on 2014/10/05.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.IOException;
import java.util.Arrays;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class GoogleAuthActivity extends Activity {

	protected static final String EXTRA_FROM_GOOGLE = "fromGoogle";
	protected static final String EXTRA_RESULT_GOOGLE = "resultGoogle";
	protected static final int REQUEST_AUTHORIZATION = 1;
	protected static final int REQUEST_ACCOUNT_PICKER = 2;

	private String packageName;
	private String classNameForLog;
	private PrivateSharedPrefs prefs;
	private AuthCommon common;
	private ProgressDialog mProgDialog;

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
		common = (AuthCommon)getApplication();
		Log.i(packageName, classNameForLog + "onCreate start");

		setContentView(R.layout.activity_google);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(packageName, classNameForLog + "onStart start");

		if (common.mGoogleCredential != null) {
			return;
		}

		boolean isError = true;

		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null || !info.isConnected()) {
			// offline
			Toast.makeText(GoogleAuthActivity.this, R.string.error_internet_not_available, Toast.LENGTH_LONG).show();
		}
		else {
			if (checkGooglePlayServicesAvailable()) {
				common.mGoogleCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE_READONLY));

				String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_GOOGLE);
				if (keys == null) {
					startActivityForResult(common.mGoogleCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
				}
				else {
					common.mGoogleCredential.setSelectedAccountName(keys[0]);
					common.mDriveService = new Drive.Builder(
												AndroidHttp.newCompatibleTransport(),
												new GsonFactory(),
												common.mGoogleCredential
											).setApplicationName(getPackageName()).build();
					mProgDialog = ProgressDialog.show(this, "", getString(R.string.dialog_authorization));
					getRootFolder();
				}
				isError = false;
			}
			else {
				// Google Play Services not available
				Toast.makeText(GoogleAuthActivity.this, R.string.error_gps_not_available, Toast.LENGTH_LONG).show();
			}
		}
		if (isError) {
			common.mDriveService = null;
			common.mGoogleCredential = null;
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra(EXTRA_FROM_GOOGLE, true);
			intent.putExtra(EXTRA_RESULT_GOOGLE, false);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.i(packageName, classNameForLog + "onActivityResult..." + requestCode);
		switch (requestCode) {
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				getRootFolder();
			}
			else {
				startActivityForResult(common.mGoogleCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
			}
			break;
		case REQUEST_ACCOUNT_PICKER:
			boolean isError = true;

			if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
				String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					common.mGoogleCredential.setSelectedAccountName(accountName);

					String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_GOOGLE);
					if (keys == null || !keys[0].equals(accountName)) {
						prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_GOOGLE, new String[]{accountName, ""});
					}

					Log.i(packageName, classNameForLog + "REQUEST_ACCOUNT_PICKER..." + accountName);

					common.mDriveService = new Drive.Builder(
												AndroidHttp.newCompatibleTransport(),
												new GsonFactory(),
												common.mGoogleCredential
											).setApplicationName(getPackageName()).build();
					if (mProgDialog != null) {
						mProgDialog.dismiss();
						mProgDialog = null;
					}
					mProgDialog = ProgressDialog.show(this, "", getString(R.string.dialog_authorization));
					getRootFolder();
					isError = false;
				}
			}

			if (isError) {
				if (mProgDialog != null) {
					mProgDialog.dismiss();
					mProgDialog = null;
				}
				common.mDriveService = null;
				common.mGoogleCredential = null;
				Intent intent = new Intent(this, MainActivity.class);
				intent.putExtra(EXTRA_FROM_GOOGLE, true);
				intent.putExtra(EXTRA_RESULT_GOOGLE, false);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.setAction(Intent.ACTION_VIEW);
				startActivity(intent);
				finish();
			}
			break;
		}
	}

	private void getRootFolder() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					About about = common.mDriveService.about().get().execute();
					common.mDriveRootFolderId = about.getRootFolderId();

					if (mProgDialog != null) {
						mProgDialog.dismiss();
						mProgDialog = null;
					}
					Intent intent = new Intent(GoogleAuthActivity.this, MainActivity.class);
					intent.putExtra(EXTRA_FROM_GOOGLE, true);
					intent.putExtra(EXTRA_RESULT_GOOGLE, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.setAction(Intent.ACTION_VIEW);
					startActivity(intent);
					finish();
				}
				catch (UserRecoverableAuthIOException e) {
					startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
				}
				catch (GoogleAuthIOException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		})).start();
	}

	// Check that Google Play Services APK is installed and up to date.
	private boolean checkGooglePlayServicesAvailable() {
		final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
			return false;
		}
		return true;
	}
}
