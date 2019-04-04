package ru.orangesoftware.financisto.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.IntegrityCheckTask;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.service.RecurrenceScheduler;
import ru.orangesoftware.financisto.utils.IntegrityCheckInstalledOnSdCard;

import static android.app.Activity.RESULT_OK;

public class ScheduledListFragment extends BlotterFragment {

    private RecurrenceScheduler scheduler;

    @Override
    protected void calculateTotals() {
        // do nothing
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected void updateAdapter() {
        ArrayList<TransactionInfo> transactions = scheduler.getSortedSchedules(System.currentTimeMillis());
        if(adapter == null){
            adapter = new ScheduledListAdapter(context, transactions);
            setListAdapter(adapter);
        } else {
            ((ScheduledListAdapter)adapter).setTransactions(transactions);
            ((ScheduledListAdapter)adapter).notifyDataSetChanged();
        }
    }

    @Override
    public void recreateCursor() {
        long now = System.currentTimeMillis();
        ArrayList<TransactionInfo> transactions = scheduler.scheduleAll(context, now);
        updateAdapter(transactions);
    }

    private void updateAdapter(ArrayList<TransactionInfo> transactions) {
        ((ScheduledListAdapter) adapter).setTransactions(transactions);
    }

    @Override
    protected void initUI(Bundle savedInstanceState) {
        super.initUI(savedInstanceState);
        scheduler = new RecurrenceScheduler(db);
        // remove filter button and totals
        bFilter.setVisibility(View.GONE);
        view.findViewById(R.id.total).setVisibility(View.GONE);
        internalOnCreateTemplates();
    }

    private void internalOnCreateTemplates() {
        // change empty list message
        ((TextView) view.findViewById(android.R.id.empty)).setText(R.string.no_scheduled_transactions);
        // fix filter
        blotterFilter = new WhereFilter("schedules");
        blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            recreateCursor();
        }
    }

    @Override
    public void afterDeletingTransaction(long id) {
        super.afterDeletingTransaction(id);
        scheduler.cancelPendingIntentForSchedule(context, id);
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(context).execute(new IntegrityCheckInstalledOnSdCard(context));
    }
}
