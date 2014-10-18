//
//  TextViewActivity.java
//  TextFileXpander
//
//  Created by wanswings on 2014/10/15.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class TextViewActivity extends Activity {

	private static final String DEFAULT_FONTSIZE = "1.0";
	private static final String[] FONT_SIZES = {"0.6", "0.8", "1.0", "1.2", "1.4", "1.6", "1.8", "2.0"};

	private String fontSize;
	private int selectedFontSizeIdx;
	private String ynHideMarker;
	private String packageName;
	private String classNameForLog;
	private PrivateSharedPrefs prefs;
	private String localFileName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		packageName = this.getPackageName();
		classNameForLog = this.getClass().getName() + "...";
		prefs = new PrivateSharedPrefs(this, PrivateSharedPrefs.SAVE_PREFS_NAME_TEXTVIEW);
		Log.i(packageName, classNameForLog + "onCreate start");

		loadKeys();

		setContentView(R.layout.activity_text_view);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		boolean fromMain = intent.getBooleanExtra(MainActivity.EXTRA_FROM_MAIN, false);
		if (fromMain) {
			localFileName = intent.getStringExtra(MainActivity.EXTRA_PARAM_MAIN);
			setTitle(localFileName);
			Log.i(packageName, classNameForLog + "EXTRA_PARAM_MAIN..." + localFileName);
			viewTextData();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(packageName, classNameForLog + "onResume start");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.text_view, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem item = (MenuItem)menu.findItem(R.id.action_hide_marker);
		if (ynHideMarker.equals(getString(android.R.string.yes))) {
			item.setTitle(R.string.action_show_marker);
		}
		else {
			item.setTitle(R.string.action_hide_marker);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(packageName, classNameForLog + "onOptionsItemSelected start");

		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		}
		else if (id == R.id.action_font_size) {
			selectFontSize();
			return true;
		}
		else if (id == R.id.action_hide_marker) {
			if (ynHideMarker.equals(getString(android.R.string.yes))) {
				ynHideMarker = getString(android.R.string.no);
			}
			else {
				ynHideMarker = getString(android.R.string.yes);
			}
			saveKeys(null, ynHideMarker);
			viewTextData();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadKeys() {
		String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_TEXTVIEW);
		if (keys != null) {
			fontSize = keys[0];
			ynHideMarker = keys[1];
		}
		else {
			fontSize = DEFAULT_FONTSIZE;
			ynHideMarker = getString(android.R.string.no);
			saveKeys(fontSize, ynHideMarker);
		}
	}

	private void saveKeys(String fSize, String hideMarker) {
		prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_TEXTVIEW, new String[]{fSize, hideMarker});
	}

	private void selectFontSize() {
		selectedFontSizeIdx = -1;
		int selected = -1;
		for (int i = 0; i < FONT_SIZES.length; i++) {
			if (fontSize.equals(FONT_SIZES[i])) {
				selectedFontSizeIdx = selected = i;
				break;
			}
		}
		new AlertDialog.Builder(this)
		.setTitle(R.string.dialog_title_select_view_type)
		.setSingleChoiceItems(FONT_SIZES, selected, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int idx) {
				selectedFontSizeIdx = idx;
			}
		})
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (selectedFontSizeIdx >= 0) {
					fontSize = FONT_SIZES[selectedFontSizeIdx];
					saveKeys(fontSize, null);
					viewTextData();
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.show();
	}

	private void viewTextData() {
		Pattern pattern = Pattern.compile("^marker:(strong:|weak:)?\\s*(.+)");

		TextView view = (TextView)findViewById(R.id.textView);
		view.setText("");

		InputStream is = null;
		BufferedReader br = null;
		String line = null;
		try {
			is = openFileInput(localFileName);
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			while ((line = br.readLine()) != null) {
				if (line.length() == 0) {					
					view.append("\n");
				}
				else {
					SpannableStringBuilder ssb = null;
					Matcher match = pattern.matcher(line);
					if (match.find()) {
						String matchCmd = match.group(1);
						String matchStr = match.group(2);
						int fg;
						if (matchCmd == null) {
							fg = Color.BLUE;
						}
						else if (matchCmd.equals("strong:")) {
							fg = Color.RED;
						}
						else if (matchCmd.equals("weak:")) {
							fg = Color.LTGRAY;
						}
						else {
							fg = Color.BLUE;
						}
						ssb = new SpannableStringBuilder(matchStr);
						int len = ssb.length();
						if (ynHideMarker.equals(getString(android.R.string.yes))) {
							BackgroundColorSpan bg = new BackgroundColorSpan(Color.BLACK);								
							ssb.setSpan(bg, 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
							fg = Color.BLACK;
						}
						ssb.setSpan(new ForegroundColorSpan(fg), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
					}
					if (ssb == null) {
						ssb = new SpannableStringBuilder(line);
					}
					if (!fontSize.equals(DEFAULT_FONTSIZE)) {
						RelativeSizeSpan fs = new RelativeSizeSpan(Float.parseFloat(fontSize));
						ssb.setSpan(fs, 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );								
					}
					view.append(ssb);
					view.append("\n");
				}
			}
		}
		catch (IOException e) {
			Log.e(packageName, classNameForLog + e.toString());
		}
		finally {
			try {
				if (br != null) br.close();
				if (is != null) is.close();
			}
			catch (IOException e) {
				Log.e(packageName, classNameForLog + e.toString());
			}
		}
	}

}
