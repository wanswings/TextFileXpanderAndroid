//
//  MainActivity.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class SubActivity extends Activity {

	protected static final String EXTRA_FROM_SUB = "fromSub";
	protected static final String EXTRA_RESULT_SUB = "resultSub";

	private String packageName;
	protected StorageListenerInterface storageListener;
	private String classNameForLog;

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
		Log.i(packageName, classNameForLog + "onCreate start");

		setContentView(R.layout.activity_sub);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		boolean fromMain = intent.getBooleanExtra(MainActivity.EXTRA_FROM_MAIN, false);
		if (fromMain) {
			String localFileName = intent.getStringExtra(MainActivity.EXTRA_PARAM_MAIN);
			setTitle(localFileName);
			Log.i(packageName, classNameForLog + "EXTRA_PARAM_MAIN..." + localFileName);
			refreshLocalData(localFileName);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(packageName, classNameForLog + "onResume start");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(packageName, classNameForLog + "onOptionsItemSelected start");

		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void refreshLocalData(String fname) {
		List<Map<String, String>> itemsMapList = new ArrayList<Map<String, String>>();
		List<Integer> itemsLayoutList = new ArrayList<Integer>();
		Pattern pattern = Pattern.compile("^(-{2}-+)\\s*(.*)");

		boolean existSubData = false;
		InputStream is = null;
		BufferedReader br = null;
		try {
			is = openFileInput(fname);
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine())!= null) {
				if (line.length() > 0) {
					existSubData = true;
					Map<String, String> itemMap = new HashMap<String, String>();
					Matcher match = pattern.matcher(line);
					if (match.find()) {
						itemMap.put("child", match.group(2));
						itemsLayoutList.add(R.layout.list_separator);
					}
					else {
						itemMap.put("child", line);
						itemsLayoutList.add(android.R.layout.simple_list_item_1);
					}
					itemsMapList.add(itemMap);
				}
			}
		}
		catch(IOException e) {
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

		if (!existSubData) {
			return;
		}

		CustomAdapter adapter = new CustomAdapter(
				this,
				itemsMapList,
				itemsLayoutList,
				new String []{"child"},
				new int []{android.R.id.text1}
		) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final View itemRenderer = super.getView(position, convertView, parent);
				final TextView tview = (TextView)itemRenderer.findViewById(android.R.id.text1);
				tview.setTextColor(0xff0000a0);
				tview.setEllipsize(TruncateAt.END);
				tview.setHorizontallyScrolling(true);

				return itemRenderer;
			}
		};

		ListView listView = (ListView)findViewById(R.id.listViewSub);
		listView.setAdapter(adapter);
		listView.setDivider(new ColorDrawable(0xffc0c0c0));
		listView.setDividerHeight(4);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView)parent;
				@SuppressWarnings("unchecked")
				Map<String, String> itemMap = (Map<String, String>)listView.getItemAtPosition(position);
				PushData push = new PushData(SubActivity.this);
				boolean isStay = push.itemClick(itemMap.get("child"));
				if (!isStay) {
					backToMain(false);
				}
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView)parent;
				@SuppressWarnings("unchecked")
				Map<String, String> itemMap = (Map<String, String>)listView.getItemAtPosition(position);
				PushData push = new PushData(SubActivity.this);
				push.itemLongClick(itemMap.get("child"));
				return true;
			}
		});
	}

	private void backToMain(boolean isStay) {
		Intent intent = new Intent(SubActivity.this, MainActivity.class);
		intent.putExtra(EXTRA_FROM_SUB, true);
		intent.putExtra(EXTRA_RESULT_SUB, isStay);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.setAction(Intent.ACTION_VIEW);
		startActivity(intent);
		finish();
	}
}
