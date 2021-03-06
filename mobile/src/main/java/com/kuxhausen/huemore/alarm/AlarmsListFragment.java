package com.kuxhausen.huemore.alarm;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.kuxhausen.huemore.NavigationDrawerActivity;
import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.persistence.Definitions.AlarmColumns;
import com.kuxhausen.huemore.persistence.Definitions.InternalArguments;

public class AlarmsListFragment extends ListFragment implements
                                                     LoaderManager.LoaderCallbacks<Cursor> {

  private static final int ALARMS_LOADER = 0;
  private NavigationDrawerActivity mParent;
  private AlarmRowAdapter dataSource;
  private int mLongSelectedPos = -1; // updated on long click

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mParent = (NavigationDrawerActivity) this.getActivity();

    getLoaderManager().initLoader(ALARMS_LOADER, null, this);

    dataSource = new AlarmRowAdapter(getActivity(), R.layout.alarm_row, null, 0);

    setListAdapter(dataSource);

    View myView = inflater.inflate(R.layout.alarms_list_fragment, null);

    setHasOptionsMenu(true);

    getActivity().setTitle(R.string.alarms);

    mParent.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    return myView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.action_alarm, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.action_add_alarm:
        NewAlarmDialogFragment nadf = new NewAlarmDialogFragment();
        nadf.show(getFragmentManager(), InternalArguments.FRAG_MANAGER_DIALOG_TAG);
        nadf.onLoadLoaderManager(null);
        return true;
      case R.id.action_help:
        mParent.showHelp(this.getResources().getString(R.string.help_title_alarms));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    mLongSelectedPos = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;

    android.view.MenuInflater inflater = this.getActivity().getMenuInflater();
    inflater.inflate(R.menu.context_alarm, menu);
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    AlarmData longSelectedRow = dataSource.getRow(mLongSelectedPos);

    switch (item.getItemId()) {
      case R.id.contextalarmmenu_edit:
        NewAlarmDialogFragment nadf = new NewAlarmDialogFragment();
        nadf.show(getFragmentManager(), InternalArguments.FRAG_MANAGER_DIALOG_TAG);
        nadf.onLoadLoaderManager(longSelectedRow);
        return true;
      case R.id.contextalarmmenu_delete:
        AlarmReceiver.unregisterAlarm(mParent, longSelectedRow);
        AlarmLogic.deleteAlarmFromDB(mParent, longSelectedRow);
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  /**
   * Callback that's invoked when the system has initialized the Loader and is ready to start the
   * query. This usually happens when initLoader() is called. The loaderID argument contains the ID
   * value passed to the initLoader() call.
   */
  @Override
  public Loader<Cursor> onCreateLoader(int loaderID, Bundle arg1) {
    /*
     * Takes action based on the ID of the Loader that's being created
     */
    switch (loaderID) {
      case ALARMS_LOADER:
        // Returns a new CursorLoader
        return new CursorLoader(getActivity(), // Parent activity context
                                AlarmColumns.ALARMS_URI, // Table
                                AlarmData.QUERY_COLUMNS, // Projection to return
                                null, // No selection clause
                                null, // No selection arguments
                                null // Default sort order
        );
      default:
        // An invalid id was passed in
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
    /*
     * Moves the query results into the adapter, causing the ListView fronting this adapter to
     * re-display
     */
    dataSource.changeCursor(cursor);
    registerForContextMenu(getListView());
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    /*
     * Clears out the adapter's reference to the Cursor. This prevents memory leaks.
     */
    // unregisterForContextMenu(getListView());
    dataSource.changeCursor(null);
  }
}
