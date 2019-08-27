package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.DateFilterActivity;
import ru.orangesoftware.financisto.activity.FilterState;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.FuturePlanner;
import ru.orangesoftware.financisto.utils.TransactionList;
import ru.orangesoftware.financisto.utils.Utils;

import static android.content.Context.MODE_PRIVATE;

public class PlannerFragment extends AbstractListFragment {

    private static final int SHOW_FILTER_REQUEST = 1;

    private TextView totalText;
    private TextView filterText;

    private PlannerTask task;

    private WhereFilter filter = WhereFilter.empty();

    public PlannerFragment() {
        super(R.layout.planner);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        totalText = view.findViewById(R.id.total);
        filterText = view.findViewById(R.id.period);
        ImageButton bFilter = view.findViewById(R.id.bFilter);
        bFilter.setOnClickListener(view -> showFilter());

        SharedPreferences preferences = context.getPreferences(MODE_PRIVATE);
        filter = WhereFilter.fromSharedPreferences(preferences);
        applyDateTimeCriteria(filter.isEmpty() ? null : filter.getDateTime());

        FilterState.updateFilterColor(context, filter, bFilter);

        return view;
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected void updateAdapter() {
        retrieveData();
    }

    @Override
    protected void deleteItem(View v, int position, long id) {

    }

    @Override
    protected void editItem(View v, int position, long id) {

    }

    @Override
    protected void viewItem(View v, int position, long id) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
            applyDateTimeCriteria(c);
            saveFilter();
            retrieveData();
        }
    }

    private void applyDateTimeCriteria(DateTimeCriteria criteria) {
        if (criteria == null) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            criteria = new DateTimeCriteria(PeriodType.THIS_MONTH);
        }
        long now = System.currentTimeMillis();
        if (now > criteria.getLongValue1()) {
            Period period = criteria.getPeriod();
            period.start = now;
            criteria = new DateTimeCriteria(period);
        }
        filter.put(criteria);
    }

    private void showFilter() {
        Intent intent = new Intent(context, DateFilterActivity.class);
        intent.putExtra(DateFilterActivity.EXTRA_FILTER_DONT_SHOW_NO_FILTER, true);
        intent.putExtra(DateFilterActivity.EXTRA_FILTER_SHOW_PLANNER, true);
        filter.toIntent(intent);
        startActivityForResult(intent, SHOW_FILTER_REQUEST);
    }

    private void saveFilter() {
        SharedPreferences preferences = context.getPreferences(MODE_PRIVATE);
        filter.toSharedPreferences(preferences);
        SharedPreferences.Editor editor = preferences.edit();
        editor.apply();
    }

    private void retrieveData() {
        if (task != null) {
            task.cancel(true);
        }
        task = new PlannerTask(filter);
        task.execute();
    }

    private void updateFilterText(WhereFilter filter) {
        Criteria c = filter.get(DatabaseHelper.ReportColumns.DATETIME);
        if (c != null) {
            filterText.setText(DateUtils.formatDateRange(context, c.getLongValue1(), c.getLongValue2(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
        } else {
            filterText.setText(R.string.no_filter);
        }
    }

    private void setTotals(Total[] totals) {
        Utils u = new Utils(context);
        u.setTotal(totalText, totals[0]);
    }

    private class PlannerTask extends AsyncTask<Void, Void, TransactionList> {

        private final WhereFilter filter;

        private PlannerTask(WhereFilter filter) {
            this.filter = WhereFilter.copyOf(filter);
        }

        @Override
        protected TransactionList doInBackground(Void... voids) {
            FuturePlanner planner = new FuturePlanner(db, filter, new Date());
            return planner.getPlannedTransactionsWithTotals();
        }

        @Override
        protected void onPostExecute(TransactionList data) {
            ScheduledListAdapter adapter = new ScheduledListAdapter(context, data.transactions);
            setListAdapter(adapter);
            setTotals(data.totals);
            updateFilterText(filter);
        }

    }
}
