package com.kuxhausen.huemore.editmood;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;

import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.persistence.Definitions.InternalArguments;
import com.kuxhausen.huemore.persistence.Utils;

import java.util.Calendar;

public class TimeOfDayTimeslot implements OnClickListener {

  final static int MAX_MOOD_EVENT_TIME = 24 * 60 * 60 * 10 - 1;
  int moodEventTime;
  private EditMoodStateGridFragment frag;
  private Button t;
  private int mPosition;

  public TimeOfDayTimeslot(EditMoodStateGridFragment frag, int pos) {
    this.frag = frag;
    LayoutInflater inflater = frag.getActivity().getLayoutInflater();
    t = (Button) inflater.inflate(R.layout.timeslot_date, null);
    t.setOnClickListener(this);

    moodEventTime = 0;
    mPosition = pos;
    setStartTime(0);
  }

  public void validate() {
    setStartTime(moodEventTime);
  }

  public String getTime() {
    if (frag == null || frag.getActivity() == null) {
      return "";
    }
    Calendar c = Utils.calendarMillisFromMoodDailyTime(moodEventTime);
    return DateFormat.getTimeFormat(frag.getActivity()).format(c.getTime());
  }

  public View getView(int position) {
    mPosition = position;
    validate();
    t.setText(getTime());
    return t;
  }

  public void setStartTime(int offsetWithinDayInDeciSeconds) {
    moodEventTime =
        Math.max(frag.computeMinimumValue(mPosition),
                 Math.min(MAX_MOOD_EVENT_TIME, offsetWithinDayInDeciSeconds));
    t.setText(getTime());
  }

  public int getStartTime() {
    return moodEventTime;
  }

  @Override
  public void onClick(View v) {
    TimePickerFragment etdf = new TimePickerFragment();
    etdf.t = this;
    etdf.show(frag.getFragmentManager(), InternalArguments.FRAG_MANAGER_DIALOG_TAG);
  }

  public static class TimePickerFragment extends DialogFragment implements
                                                                TimePickerDialog.OnTimeSetListener {

    TimeOfDayTimeslot t;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      // Use the current time as the default values for the picker
      Calendar c = Utils.calendarMillisFromMoodDailyTime(t.moodEventTime);
      int hour = c.get(Calendar.HOUR_OF_DAY);
      int minute = c.get(Calendar.MINUTE);

      // Create a new instance of TimePickerDialog and return it
      return new TimePickerDialog(getActivity(), this, hour, minute,
                                  DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
      Calendar c = Calendar.getInstance();
      c.set(Calendar.MILLISECOND, 0);
      c.set(Calendar.MINUTE, minute);
      c.set(Calendar.HOUR_OF_DAY, hourOfDay);

      Calendar previousTimeslotCal =
          Utils.calendarMillisFromMoodDailyTime(t.frag.computeMinimumValue(t.mPosition));

      if (previousTimeslotCal.before(c)) {
        t.setStartTime(Utils.moodDailyTimeFromCalendarMillis(c));
      } else {
        t.setStartTime(t.frag.computeMinimumValue(t.mPosition));
      }
      t.frag.validate();
    }
  }
}
