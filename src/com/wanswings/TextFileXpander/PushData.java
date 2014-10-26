//
//  PushData.java
//  TextFileXpander
//
//  Created by wanswings on 2014/09/25.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class PushData {

	private Context mContext;
	private String packageName;
	private String classNameForLog;

	public PushData(Context ct) {
		mContext = ct;

		packageName = mContext.getPackageName();
		classNameForLog = this.getClass().getName() + "...";
	}

	public boolean itemClick(String str) {
		Log.i(packageName, classNameForLog + "itemClick..." + str);
		boolean isStay = false;

		Pattern pattern1 = Pattern.compile("^([a-z]+):\\s*(.+)");
		Matcher match1 = pattern1.matcher(str);
		if (match1.find()) {
			String matchCmd = match1.group(1);
			Log.i(packageName, classNameForLog + "matchCmd..." + matchCmd);
			String matchStr = match1.group(2);
			Log.i(packageName, classNameForLog + "matchStr..." + matchStr);

			String url = null;
			String action = null;
			boolean isSendClipboard = false;
			try {
				if (matchCmd.equals("currency")) {
					// currency
					Pattern pattern2 = Pattern.compile("^\\s*from:\\s*(.+)\\s+to:\\s*(\\S+)");
					Matcher match2 = pattern2.matcher(matchStr);
					if (match2.find()) {
						String matchfrom = match2.group(1);
						String matchto = match2.group(2);

						url = "http://www.google.com/finance/?q=";
						url += URLEncoder.encode(matchfrom, "utf-8");
						url += URLEncoder.encode(matchto, "utf-8");
						action = Intent.ACTION_VIEW;
					}
				}
				else if (matchCmd.equals("dict")) {
					// dict
					str = matchStr;
				}
				else if (matchCmd.equals("flight")) {
					// flight
					url = "http://flightaware.com/live/flight/" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("mailto")) {
					// mailto
					url = "mailto:" + matchStr;
					action = Intent.ACTION_SENDTO;
				}
				else if (matchCmd.equals("map")) {
					// map
					url = "http://maps.google.com/maps?q=" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("near")) {
					// near
					url = "http://foursquare.com/explore?near=" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("people")) {
					// people
					url = "content://contacts/people/";
					action = Intent.ACTION_VIEW;
					str = matchStr;
					isSendClipboard = true;
				}
				else if (matchCmd.equals("recipe")) {
					// recipe
					url = "http://www.epicurious.com/tools/searchresults?search=" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("route")) {
					// route
					Pattern pattern2 = Pattern.compile("^\\s*from:\\s*(.+)\\s+to:\\s*(.+)");
					Matcher match2 = pattern2.matcher(matchStr);
					if (match2.find()) {
						String matchfrom = match2.group(1);
						String matchto = match2.group(2);

						url = "http://maps.google.com/maps?saddr=";
						if (!matchfrom.equals("here")) {
							url += URLEncoder.encode(matchfrom, "utf-8");
						}
						url += "&daddr=" + URLEncoder.encode(matchto, "utf-8");
						action = Intent.ACTION_VIEW;
					}
				}
				else if (matchCmd.equals("tel")) {
					// tel
					url = "tel:" + matchStr;
					action = Intent.ACTION_DIAL;
				}
				else if (matchCmd.equals("twitter")) {
					// twitter
					url = "twitter://post?message=" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("url")) {
					// url
					url = matchStr;
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("weather")) {
					// weather
					url = "http://www.google.com/search?q=weather%20" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}
				else if (matchCmd.equals("youtube")) {
					// youtube
					url = "http://www.youtube.com/results?search_query=" + URLEncoder.encode(matchStr, "utf-8");
					action = Intent.ACTION_VIEW;
				}

				if (action != null) {
					Log.i(packageName, classNameForLog + action + "..." + url);
					isStay = true;
					Intent intent = new Intent();
					intent.setAction(action);
					intent.setData(Uri.parse(url));
					mContext.startActivity(intent);
					if (!isSendClipboard) {
						return isStay;
					}
				}
			}
			catch (Exception e) {
				Log.e(packageName, classNameForLog + "cannot launch..." + matchCmd);
			}
		}

		ClipData clip = ClipData.newPlainText("copied_text", str);
		ClipboardManager cm =(ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
		cm.setPrimaryClip(clip);
		Toast.makeText(mContext, R.string.toast_copied_clipboard, Toast.LENGTH_LONG).show();

		return isStay;
	}

	public void itemLongClick(String item) {
		Log.i(packageName, classNameForLog + "itemLongClick..." + item);

		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, item);
		mContext.startActivity(Intent.createChooser(intent, "Choose Share App"));
	}
}
