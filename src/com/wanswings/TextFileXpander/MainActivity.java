//
//  MainActivity.java
//  TextFileXpander
//
//  Created by wanswings on 2014/08/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements StorageListenerInterface {

	protected static final String EXTRA_FROM_MAIN = "fromMain";
	protected static final String EXTRA_PARAM_MAIN = "paramMain";

	private static final String SAMPLE_FILE = "sample.txt";
	private static final String VIEW_TYPE_STD = "Standard View";
	private static final String VIEW_TYPE_EXP = "Expandable View";
	private static final String VIEW_TYPE_TEXT = "Memorizable View";
	private static final String[] VIEW_TYPES = {VIEW_TYPE_STD, VIEW_TYPE_EXP, VIEW_TYPE_TEXT};
	private static final String DEVICE_STORAGE = "EXT Storage";
	private static final String[] STORAGE_NAMES = {"Dropbox", "Google Drive", DEVICE_STORAGE};
	private static final String[] STORAGE_CLASSES = {Dropbox.class.getName(), GoogleDrive.class.getName(), ExternalStorage.class.getName()};

	private Object storage = null;
	private String currentStorage;
	private int selectedStorageIdx;
	private String currentViewType;
	private int selectedViewTypeIdx;
	private String ynNotification;
	private String packageName;
	private String classNameForLog;
	private PrivateSharedPrefs prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		packageName = this.getPackageName();
		classNameForLog = this.getClass().getName() + "...";
		prefs = new PrivateSharedPrefs(this, PrivateSharedPrefs.SAVE_PREFS_NAME_MAIN);
		Log.i(packageName, classNameForLog + "onCreate start");

		String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_FIRSTTIME);
		if (keys == null) {
			// first time
			copySampleFile();
			prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_FIRSTTIME, new String[]{"1"});
		}

		loadKeys();
		if (!currentStorage.equals("")) {
			setTitle(getString(R.string.title_activity_main) + " [" + currentStorage + "]");
		}

		String action = getIntent().getAction();
		if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.i(packageName, classNameForLog + "ACTION_BOOT_COMPLETED");
			if (ynNotification.equals(getString(android.R.string.yes))) {
				addNotification();
			}
			finish();
		}
		else {
			if (currentViewType.equals(VIEW_TYPE_EXP)) {
				setContentView(R.layout.activity_main_expandable);
			}
			else {
				setContentView(R.layout.activity_main);			
			}
	
			if (fileList().length > 0) {
				refreshLocalData();
			}
			else {
				selectStorage();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(packageName, classNameForLog + "onResume start");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(packageName, classNameForLog + "onNewIntent start");

		boolean fromSub = intent.getBooleanExtra(SubActivity.EXTRA_FROM_SUB, false);
		if (fromSub) {
			boolean isStay = intent.getBooleanExtra(SubActivity.EXTRA_RESULT_SUB, false);
			if (!isStay) {
				finish();
			}
		}
		else {
			if (storage != null) {
				boolean needLogout = false;
				String mName = "onNewIntent";
				try {
					Method method = storage.getClass().getMethod(mName, Intent.class);
					boolean result = (Boolean)method.invoke(storage, intent);
					if (!result) {
						// NG
						needLogout = true;
					}
				}
				catch (Exception e) {
					Log.e(packageName, classNameForLog + mName + "..." + e.toString());
					needLogout = true;
				}
				if (needLogout) {
					closeStorage();
					loadKeys();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem item = (MenuItem)menu.findItem(R.id.action_add_notification);
		if (ynNotification.equals(getString(android.R.string.yes))) {
			item.setTitle(R.string.action_remove_notification);
		}
		else {
			item.setTitle(R.string.action_add_notification);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(packageName, classNameForLog + "onOptionsItemSelected start");

		int id = item.getItemId();
		if (id == R.id.action_load_data) {
			selectStorage();
			return true;
		}
		else if (id == R.id.action_refresh) {
			refresh();
			return true;
		}
		else if (id == R.id.action_add_notification) {
			if (ynNotification.equals(getString(android.R.string.yes))) {
				removeNotification();
				ynNotification = getString(android.R.string.no);
			}
			else {
				addNotification();
				ynNotification = getString(android.R.string.yes);
			}
			saveKeys(null, ynNotification, null);
			return true;
		}
		else if (id == R.id.action_view_type) {
			selectViewType();
			return true;
		}
		else if (id == R.id.action_reset) {
			reset();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void readyToReadPrivateFiles() {
		Log.i(packageName, classNameForLog + "readyToReadPrivateFiles start");
		closeStorage();
		saveKeys(null, null, currentStorage);
		if (!currentStorage.equals("")) {
			setTitle(getString(R.string.title_activity_main) + " [" + currentStorage + "]");
		}
		refreshLocalData();
	}

	@Override
	public void readyToStartDropboxAuthActivity() {
		Log.i(packageName, classNameForLog + "readyToStartDropboxAuthActivity start");
		Intent intent = new Intent(this, DropboxAuthActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		startActivity(intent);
	}

	@Override
	public void readyToStartGoogleAuthActivity() {
		Log.i(packageName, classNameForLog + "readyToStartGoogleAuthActivity start");
		Intent intent = new Intent(this, GoogleAuthActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		startActivity(intent);
	}

	@Override
	public void cancelSelectDirDialog() {
		Log.i(packageName, classNameForLog + "cancelSelectDirDialog start");
		closeStorage();
		loadKeys();
	}

	private boolean copySampleFile() {
		boolean result = false;

		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = getAssets().open(SAMPLE_FILE);
			fos = openFileOutput(SAMPLE_FILE, Context.MODE_PRIVATE);
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				fos.write(buf, 0, len);
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

		return result;
	}

	private void loadKeys() {
		String[] keys = prefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_MAIN);
		if (keys != null) {
			currentViewType = keys[0];
			ynNotification = keys[1];
			currentStorage = keys[2];
		}
		else {
			currentViewType = VIEW_TYPE_STD;
			ynNotification = getString(android.R.string.no);
			currentStorage = "";
			saveKeys(currentViewType, ynNotification, currentStorage);
		}
	}

	private void saveKeys(String viewType, String notification, String storage) {
		prefs.storeKeys(PrivateSharedPrefs.SAVE_KEYS_MAIN, new String[]{viewType, notification, storage});
	}

	private void addNotification() {
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		Notification notification = new Notification.Builder(this)
					.setContentIntent(contentIntent)
					.setContentTitle(getString(R.string.app_name))
					.setContentText(getString(R.string.app_summary))
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setLargeIcon(largeIcon)
					.build();
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notificationManager.notify(R.string.app_name, notification);
	}

	private void removeNotification() {
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(R.string.app_name);
	}

	private void selectViewType() {
		selectedViewTypeIdx = -1;
		int selected = -1;
		for (int i = 0; i < VIEW_TYPES.length; i++) {
			if (currentViewType.equals(VIEW_TYPES[i])) {
				selectedViewTypeIdx = selected = i;
				break;
			}
		}
		new AlertDialog.Builder(this)
		.setTitle(R.string.dialog_title_select_view_type)
		.setSingleChoiceItems(VIEW_TYPES, selected, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int idx) {
				selectedViewTypeIdx = idx;
			}
		})
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (selectedViewTypeIdx >= 0) {
					currentViewType = VIEW_TYPES[selectedViewTypeIdx];
					saveKeys(currentViewType, null, null);
					Toast.makeText(MainActivity.this, R.string.toast_restart, Toast.LENGTH_LONG).show();
					Intent intent = getIntent();
					finish();
					startActivity(intent);
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.show();
	}

	private void reset() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.dialog_title_reset)
		.setMessage(R.string.dialog_message_reset)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Log.i(packageName, classNameForLog + "reset start");
				// delete private files
				String[] fileList = fileList();
				for (String fname: fileList) {
					deleteFile(fname);
				}
				prefs.clearAllKeys();
				removeNotification();
				Toast.makeText(MainActivity.this, R.string.toast_restart, Toast.LENGTH_LONG).show();
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.show();
	}

	private void closeStorage() {
		Log.i(packageName, classNameForLog + "closeStorage start");

		if (storage != null) {
			String mName = "close";
			try {
				Method method = storage.getClass().getMethod(mName);
				method.invoke(storage);
			}
			catch (Exception e) {
				Log.e(packageName, classNameForLog + mName + "..." + e.toString());
			}
			storage = null;
		}
	}

	private void refresh() {
		selectedStorageIdx = -1;
		for (int i = 0; i < STORAGE_NAMES.length; i++) {
			if (currentStorage.equals(STORAGE_NAMES[i])) {
				selectedStorageIdx = i;
				break;
			}
		}
		if (selectedStorageIdx == -1) {
			return;
		}

		try {
			Class<?> storageClass = Class.forName(STORAGE_CLASSES[selectedStorageIdx]);
			// isRefresh = true
			storage = storageClass.getConstructor(Context.class, boolean.class, StorageListenerInterface.class)
									.newInstance(MainActivity.this, true, MainActivity.this);
		}
		catch (Exception e) {
			Log.e(packageName, classNameForLog + currentStorage + " newInstance..." + e.toString());
			storage = null;
		}
	}

	private void selectStorage() {
		selectedStorageIdx = -1;
		int selected = -1;
		for (int i = 0; i < STORAGE_NAMES.length; i++) {
			if (currentStorage.equals(STORAGE_NAMES[i])) {
				selectedStorageIdx = selected = i;
				break;
			}
		}
		new AlertDialog.Builder(this)
		.setTitle(R.string.dialog_title_select_storage)
		.setSingleChoiceItems(STORAGE_NAMES, selected, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int idx) {
				selectedStorageIdx = idx;
			}
		})
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (selectedStorageIdx >= 0) {
					currentStorage = STORAGE_NAMES[selectedStorageIdx];
					try {
						Class<?> storageClass = Class.forName(STORAGE_CLASSES[selectedStorageIdx]);
						// isRefresh = false
						storage = storageClass.getConstructor(Context.class, boolean.class, StorageListenerInterface.class)
												.newInstance(MainActivity.this, false, MainActivity.this);
					}
					catch (Exception e) {
						Log.e(packageName, classNameForLog + currentStorage + " newInstance..." + e.toString());
						storage = null;
					}
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.show();
	}

	private void refreshLocalData() {
		if (currentViewType.equals(VIEW_TYPE_EXP)) {
			refreshLocalData4ExpandableListView();
		}
		else {
			refreshLocalData4ListView();
		}
	}

	private void refreshLocalData4ExpandableListView() {
		List<Map<String, String>> groupMapList = new ArrayList<Map<String, String>>();
		List<Integer> groupLayoutList = new ArrayList<Integer>();
		List<List<Map<String, String>>> itemsMapListList = new ArrayList<List<Map<String, String>>>();
		List<List<Integer>> itemsLayoutListList = new ArrayList<List<Integer>>();
		Pattern pattern1 = Pattern.compile("^(-{2}-+)\\s*(.*)");
		Pattern pattern2 = Pattern.compile("^marker:(strong:|weak:)?\\s*(.+)");

		String[] fileList = fileList();
		Arrays.sort(fileList);
		for (String fname: fileList) {
			Map<String, String> groupMap = new HashMap<String, String>();
			groupMap.put("group", fname);
			groupMapList.add(groupMap);
			groupLayoutList.add(R.layout.expandable_list);

			List<Map<String, String>> itemsMapList = new ArrayList<Map<String, String>>();
			List<Integer> itemsLayoutList = new ArrayList<Integer>();
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
								line = match2.group(2);
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
							itemMap.put("child", line);
							itemsLayoutList.add(R.layout.expandable_list);
						}
						itemsMapList.add(itemMap);
					}
				}
				itemsMapListList.add(itemsMapList);
				itemsLayoutListList.add(itemsLayoutList);
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
		}

		CustomExpandableListAdapter adapter = new CustomExpandableListAdapter(
				this,
				groupMapList,
				groupLayoutList,
				new String []{"group"},
				new int []{android.R.id.text1},
				itemsMapListList,
				itemsLayoutListList,
				new String []{"child"},
				new int []{android.R.id.text1}
		) {
			@Override
			public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
				final View itemRenderer = super.getGroupView(groupPosition, isExpanded, convertView, parent);
				final TextView tview = (TextView)itemRenderer.findViewById(android.R.id.text1);
				tview.setTextColor(Color.BLACK);
				tview.setEllipsize(TruncateAt.END);
				tview.setHorizontallyScrolling(true);

				return itemRenderer;
			}
			@Override
			public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
				final View itemRenderer = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
				final TextView tview = (TextView)itemRenderer.findViewById(android.R.id.text1);
				tview.setEllipsize(TruncateAt.END);
				tview.setHorizontallyScrolling(true);

				@SuppressWarnings("unchecked")
				Map<String, String> itemMap = (Map<String, String>)getChild(groupPosition, childPosition);
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
				else {
					fg = Color.BLACK;
				}
				tview.setTextColor(fg);

				return itemRenderer;
			}
		};

		ExpandableListView listView = (ExpandableListView)findViewById(R.id.expandableListViewMain);
		listView.setAdapter(adapter);
		listView.setDivider(new ColorDrawable(0xff808080));
		listView.setChildDivider(new ColorDrawable(0xffc0c0c0));
		listView.setDividerHeight(4);
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				ExpandableListView listView = (ExpandableListView)parent;
				long exPosition = listView.getExpandableListPosition(position);
				int groupPosition = ExpandableListView.getPackedPositionGroup(exPosition);
				if (ExpandableListView.getPackedPositionType(exPosition) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
					// group
					@SuppressWarnings("unchecked")
					Map<String, String> groupMap = (Map<String, String>)listView.getItemAtPosition(groupPosition);
					groupItemLongClick(groupMap.get("group"));
				}
				else {
					// child
					int childPosition = ExpandableListView.getPackedPositionChild(exPosition);
					ExpandableListAdapter adapter = listView.getExpandableListAdapter();
					@SuppressWarnings("unchecked")
					Map<String, String> item = (Map<String, String>)adapter.getChild(groupPosition, childPosition);
					PushData push = new PushData(MainActivity.this);
					push.itemLongClick(item.get("child"));
				}
				return true;
			}
		});
		listView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
				ExpandableListAdapter adapter = parent.getExpandableListAdapter();
				@SuppressWarnings("unchecked")
				Map<String, String> item = (Map<String, String>)adapter.getChild(groupPosition, childPosition);
				PushData push = new PushData(MainActivity.this);
				boolean isStay = push.itemClick(item.get("child"));
				if (!isStay) {
					finish();
				}
				return false;
			}
		});
	}

	private void refreshLocalData4ListView() {
		List<Map<String, String>> groupMapList = new ArrayList<Map<String, String>>();

		String[] fileList = fileList();
		Arrays.sort(fileList);
		for (String fname: fileList) {
			Map<String, String> groupMap = new HashMap<String, String>();
			groupMap.put("group", fname);
			groupMap.put("next", getString(R.string.mark_next));
			groupMapList.add(groupMap);
		}

		SimpleAdapter adapter = new SimpleAdapter(
				this,
				groupMapList,
				R.layout.standard_list,
				new String []{"group", "next"},
				new int []{android.R.id.text1, android.R.id.text2}
		) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final View itemRenderer = super.getView(position, convertView, parent);
				final TextView tview = (TextView)itemRenderer.findViewById(android.R.id.text1);
				tview.setTextColor(Color.BLACK);
				tview.setEllipsize(TruncateAt.END);
				tview.setHorizontallyScrolling(true);

				return itemRenderer;
			}
		};

		ListView listView = (ListView)findViewById(R.id.listViewMain);
		listView.setAdapter(adapter);
		listView.setDivider(new ColorDrawable(0xff808080));
		listView.setDividerHeight(4);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView)parent;
				@SuppressWarnings("unchecked")
				Map<String, String> groupMap = (Map<String, String>)listView.getItemAtPosition(position);

				Intent intent = null;
				if (currentViewType.equals(VIEW_TYPE_TEXT)) {
					intent = new Intent(MainActivity.this, TextViewActivity.class);
				}
				else {
					intent = new Intent(MainActivity.this, SubActivity.class);
				}
				intent.putExtra(EXTRA_FROM_MAIN, true);
				intent.putExtra(EXTRA_PARAM_MAIN, groupMap.get("group"));
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.setAction(Intent.ACTION_VIEW);
				startActivity(intent);
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView)parent;
				@SuppressWarnings("unchecked")
				Map<String, String> groupMap = (Map<String, String>)listView.getItemAtPosition(position);
				groupItemLongClick(groupMap.get("group"));
				return true;
			}
		});
	}

	private void groupItemLongClick(String fname) {
		if (!currentStorage.equals(DEVICE_STORAGE)) {
			launchTextAppForCloud(fname);
		}
		else {
			PrivateSharedPrefs sPrefs = new PrivateSharedPrefs(MainActivity.this, PrivateSharedPrefs.SAVE_PREFS_NAME_STORAGE);
			String[] keys = sPrefs.getKeys(PrivateSharedPrefs.SAVE_KEYS_STORAGE);
			if (keys != null) {
				launchTextApp(keys[0] + File.separator + fname);
			}
		}
	}

	private void launchTextAppForCloud(String fname) {
		boolean result = false;
		File dstFile = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			dstFile = File.createTempFile(fname.substring(0, fname.lastIndexOf(".")), Storage.TEXT_FILE_EXTENSION);
			dstFile.deleteOnExit();
			fis = openFileInput(fname);
			fos = new FileOutputStream(dstFile);
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

		if (result) {
			dstFile.setReadable(true, false);
			launchTextApp(dstFile.getAbsolutePath());
		}
	}

	private void launchTextApp(String fullPath) {
		Log.i(packageName, classNameForLog + "launchTextApp..." + fullPath);

		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + fullPath), "text/plain");
		startActivity(intent);
	}
}
