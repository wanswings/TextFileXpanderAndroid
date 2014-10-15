//
//  StorageListenerInterface.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.util.EventListener;

public interface StorageListenerInterface extends EventListener {

	public void readyToReadPrivateFiles();
	public void readyToStartDropboxAuthActivity();
	public void readyToStartGoogleAuthActivity();
	public void cancelSelectDirDialog();

}
