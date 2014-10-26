//
//  SubActivity.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
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
	private CustomAdapter adapter;

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
		Pattern pattern1 = Pattern.compile("^(-{2}-+)\\s*(.*)");
		Pattern pattern2 = Pattern.compile("^([a-z]+):(.+)");

		int idxSub = 0;
		InputStream is = null;
		BufferedReader br = null;
		try {
			is = openFileInput(fname);
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			while ((line = br.readLine())!= null) {
				if (line.length() > 0) {
					Map<String, String> itemMap = new HashMap<String, String>();
					Matcher match1 = pattern1.matcher(line);
					if (match1.find()) {
						itemMap.put("marker", "");
						itemMap.put("child", match1.group(2));
						itemsLayoutList.add(R.layout.list_separator);
					}
					else {
						Matcher match2 = pattern2.matcher(line);
						if (match2.find()) {
							String matchCmd = match2.group(1);
							String matchStr = match2.group(2);

							if (matchCmd.equals("currency")) {
								// currency
								getCurrencyStart(matchStr, idxSub);
								itemMap.put("marker", "");
							}
							else if (matchCmd.equals("marker")) {
								// marker
								line = getMarkerColor(itemMap, matchStr, line);
							}
							else {
								itemMap.put("marker", "");
							}
						}
						else {
							itemMap.put("marker", "");
						}
						itemMap.put("child", line);
						itemsLayoutList.add(android.R.layout.simple_list_item_1);
					}
					itemsMapList.add(itemMap);
					idxSub++;
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

		if (idxSub == 0) {
			return;
		}

		adapter = new CustomAdapter(
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
				tview.setEllipsize(TruncateAt.END);
				tview.setHorizontallyScrolling(true);

				@SuppressWarnings("unchecked")
				Map<String, String> itemMap = (Map<String, String>)getItem(position);
				String marker = itemMap.get("marker");
				int fg;
				if (marker.equals("strong:")) {
					fg = Color.RED;
				}
				else if (marker.equals("weak:")) {
					fg = Color.LTGRAY;
				}
				else if (marker.equals("normal:")) {
					fg = Color.BLUE;
				}
				else if (marker.equals("currency:")) {
					fg = 0xffa52a2a;
				}
				else {
					fg = Color.BLACK;
				}
				tview.setTextColor(fg);

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

	private String getMarkerColor(Map<String, String> itemMap, String param, String line) {
		Pattern pattern = Pattern.compile("^\\s*(strong:|weak:)?\\s*(.+)");
		Matcher match = pattern.matcher(param);
		if (match.find()) {
			String matchCmd = match.group(1);
			line = match.group(2);
			if (matchCmd == null) {
				itemMap.put("marker", "normal:");
			}
			else {
				itemMap.put("marker", matchCmd);									
			}
		}
		else {
			itemMap.put("marker", "");
		}

		return line;
	}

	private void getCurrencyStart(String param, int idxSub) {
		Pattern pattern = Pattern.compile("^\\s*from:\\s*(.+)\\s+to:\\s*(.+)");
		Matcher match = pattern.matcher(param);
		if (!match.find()) {
			return;
		}

		String matchfrom = match.group(1);
		String matchto = match.group(2);

		try {
			String wk = "http://www.google.com/finance/converter?a=1&from=";
			wk += URLEncoder.encode(matchfrom, "utf-8");
			wk += "&to=" + URLEncoder.encode(matchto, "utf-8");
			HttpGetTask task = new HttpGetTask();
			task.execute(new String[]{wk, String.valueOf(idxSub)});
		}
		catch (Exception e) {
		}
	}

	class HttpGetTask extends AsyncTask<String, Void, Map<String, String>> {
		@Override
		protected Map<String, String> doInBackground(String... params) {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(params[0]);
			try{
				HttpResponse response = client.execute(get);
				StatusLine statusLine = response.getStatusLine();
				if(statusLine.getStatusCode() == HttpURLConnection.HTTP_OK){
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					response.getEntity().writeTo(outputStream);
					Map<String, String> resultMap = new HashMap<String, String>();
					resultMap.put("getdata", outputStream.toString());
					resultMap.put("idxSub", params[1]);
					return resultMap;
				}
			}
			catch (Exception e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(Map<String, String> result) {
			Pattern pattern = Pattern.compile("<span class=bld>([0-9\\.]+).+</span>");
			Matcher match = pattern.matcher(result.get("getdata"));
			if (match.find()) {
				String matchValue = match.group(1);

				int idxSub = Integer.parseInt(result.get("idxSub"));
				@SuppressWarnings("unchecked")
				Map<String, String> itemMap = (Map<String, String>)adapter.getItem(idxSub);
				String line = itemMap.get("child");
				line += "   [" + matchValue + "]";
				itemMap.put("child", line);
				itemMap.put("marker", "currency:");
				Log.i(packageName, classNameForLog + "onPostExecute..." + line);

				adapter.notifyDataSetChanged();
			}
		}
	}
}
