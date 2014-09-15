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
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {
	private List<? extends Map<String, ?>> mGroupData;
	private List<? extends Integer> mGroupLayout;
	private String[] mGroupFrom;
	private int[] mGroupTo;

	private List<? extends List<? extends Map<String, ?>>> mChildData;
	private List<? extends List<? extends Integer>> mChildLayout;
	private String[] mChildFrom;
	private int[] mChildTo;

	private LayoutInflater mInflater;

	public CustomExpandableListAdapter(
				Context context,
				List<? extends Map<String, ?>> groupData,
				List<? extends Integer> groupLayout,
				String[] groupFrom,
				int[] groupTo,
				List<? extends List<? extends Map<String, ?>>> childData,
				List<? extends List<? extends Integer>> childLayout,
				String[] childFrom,
				int[] childTo
				) {
		mGroupData = groupData;
		mGroupLayout = groupLayout;
		mGroupFrom = groupFrom;
		mGroupTo = groupTo;

		mChildData = childData;
		mChildLayout = childLayout;
		mChildFrom = childFrom;
		mChildTo = childTo;

		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mChildData.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public int getChildType(int groupPosition, int childPosition) {
		if (mChildLayout.get(groupPosition).get(childPosition) == R.layout.list_separator)
			return 0;
		else
			return 1;
	}

	@Override
	public int getChildTypeCount() {
		return 2;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = mInflater.inflate(mChildLayout.get(groupPosition).get(childPosition), parent, false);
		} else {
			v = convertView;
		}
		bindView(v, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
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
	public int getChildrenCount(int groupPosition) {
		return mChildData.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mGroupData.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mGroupData.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public int getGroupType(int groupPosition) {
		if (mGroupLayout.get(groupPosition) == R.layout.list_separator)
			return 0;
		else
			return 1;
	}

	@Override
	public int getGroupTypeCount() {
		return 2;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = mInflater.inflate(mGroupLayout.get(groupPosition), parent, false);
		} else {
			v = convertView;
		}
		bindView(v, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
		return v;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		if (mChildLayout.get(groupPosition).get(childPosition) == R.layout.list_separator) {
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
