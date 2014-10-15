//
//  AuthCommon.java
//  TextFileXpander
//
//  Created by wanswings on 2014/10/05.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.Drive;

import android.app.Application;

public class AuthCommon extends Application {

	protected DropboxAPI<AndroidAuthSession> mDropboxAPI;

	protected GoogleAccountCredential mGoogleCredential;
	protected Drive mDriveService;
	protected String mDriveRootFolderId;

}
