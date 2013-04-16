package com.kuxhausen.huemore.timing;

import com.kuxhausen.huemore.*;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class AlarmsListFragment extends ListFragment{

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View myView = inflater.inflate(R.layout.alarm_view, null);
		
		LinearLayout headingRow = (LinearLayout) myView
				.findViewById(R.id.showOnLandScape);
		if (headingRow.getVisibility() == View.GONE)
			setHasOptionsMenu(true);
		getActivity().setTitle(R.string.alarms);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initializeActionBar(true);
		}
		return myView;
	}
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void initializeActionBar(Boolean value) {
		try {
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(value);
		} catch (Error e) {
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.action_alarm, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			getActivity().onBackPressed();
			return true;
		
		case R.id.action_add_alarm:
			NewAlarmDialogFragment nadf = new NewAlarmDialogFragment();
			nadf.show(getFragmentManager(), "dialog");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}