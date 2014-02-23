package com.kuxhausen.huemore.editmood;

import java.util.Calendar;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.InternalArguments;
import com.kuxhausen.huemore.timing.Conversions;


import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;

public class RelativeStartTimeslot implements TimeslotStartTime, OnClickListener{

	final static int MAX_MOOD_EVENT_TIME = 24*60*60*10-1;
	int moodEventTime;
	private EditMoodStateGridFragment frag;
	private Button t;
	
	public RelativeStartTimeslot(EditMoodStateGridFragment frag, int id, int position){
		this.frag = frag;
		LayoutInflater inflater = frag.getActivity().getLayoutInflater();
		t = (Button)inflater.inflate(R.layout.timeslot_date, null);
		t.setOnClickListener(this);
		
		moodEventTime = 0;
		
		setStartTime(36000+frag.computeMinimumValue(position));
	}

	
	public String getTime() {
		if(frag==null || frag.getActivity()==null)
			return "";
		Calendar c = Conversions.calendarMillisFromMoodDailyTime(moodEventTime);
		return DateFormat.getTimeFormat(frag.getActivity()).format(c.getTime());
	}
	
	@Override
	public View getView() {
		t.setText(getTime());
		
		return t;
	}

	@Override
	public void setStartTime(int offsetWithinDayInDeciSeconds) {		
		moodEventTime = Math.min(MAX_MOOD_EVENT_TIME,offsetWithinDayInDeciSeconds);
	}

	@Override
	public int getStartTime() {		
		return moodEventTime;
	}
	
	@Override
	public void onClick(View v) {
		TimePickerFragment etdf = new TimePickerFragment();
		etdf.t = this;
		//etdf.setTimeOfDayResultListener(this);
		etdf.show(frag.getFragmentManager(),InternalArguments.FRAG_MANAGER_DIALOG_TAG);
	}

	public static class TimePickerFragment extends SherlockDialogFragment implements TimePickerDialog.OnTimeSetListener {

		RelativeStartTimeslot t; 
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			// Use the current time as the default values for the picker
			Calendar c = Conversions.calendarMillisFromMoodDailyTime(t.moodEventTime);
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);
			
			// Create a new instance of TimePickerDialog and return it
			return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
		}
		
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.MILLISECOND, 0);
			c.set(Calendar.MINUTE, minute);
			c.set(Calendar.HOUR_OF_DAY, hourOfDay);
			
			
			Calendar previousTimeslotCal = Conversions.calendarMillisFromMoodDailyTime(t.frag.computeMinimumValue(t.frag.dailyTimeslotDuration.indexOf(t)));
		//	if(previousTimeslotCal.before(c)){
				t.setStartTime(Conversions.moodDailyTimeFromCalendarMillis(c));
		//	}
		//	else{
		//		t.setDuration(36000+Conversions.moodDailyTimeFromCalendarMillis(previousTimeslotCal.getTimeInMillis()));
		//	}
			t.frag.redrawGrid();
		}
	}
}