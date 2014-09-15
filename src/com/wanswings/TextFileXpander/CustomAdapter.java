//
//  SystemBroadcastReceiver.java
//  TextFileXpander
//
//  Created by wanswings on 2014/09/09.
//  Copyright (c) 2014 wanswings. All rights reserved.
//
package com.wanswings.TextFileXpander;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CustomAdapter extends BaseAdapter {

	private int[] mTo;
	private String[] mFrom;
	private List<? extends Map<String, ?>> mData;
	private List<? extends Integer> mLayout;
	private LayoutInflater mInflater;

	public CustomAdapter(
				Context context,
				List<? extends Map<String, ?>> data,
				List<? extends Integer> layout,
				String[] from,
				int[] to) {
        mData = data;
        mLayout = layout;
        mFrom = from;
        mTo = to;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		if (mLayout.get(position) == R.layout.list_separator)
			return 0;
		else
			return 1;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = mInflater.inflate(mLayout.get(position), parent, false);
		} else {
			v = convertView;
		}
		bindView(v, mData.get(position), mFrom, mTo);
		return v;
	}

	private void bindView(View view, Map<String, ?> data, String[] from, int[] to) {
		int len = to.length;

		for (int i = 0; i < len; i++) {
			TextView v = (TextView)view.findViewById(to[i]);
			if (v != null) {
				v.setText((String)data.get(from[i]));
			}
		}
	}

	@Override
	public boolean isEnabled(int position) {
		if (mLayout.get(position) == R.layout.list_separator) {
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}
}
