package ru.orangesoftware.financisto.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;

public class SplitsBlotterFragment extends BlotterFragment {
    @Override
    protected void initUI(Bundle savedInstanceState) {
        super.initUI(savedInstanceState);
        bFilter.setVisibility(View.GONE);
    }

    @Override
    protected Cursor createCursor() {
        return db.getBlotterForAccountWithSplits(blotterFilter);
    }

    @Override
    protected void updateAdapter() {
        if(adapter == null){
            adapter = new TransactionsListAdapter(getContext(), db, cursor);
            setListAdapter(adapter);
        }else{
            ((TransactionsListAdapter) adapter).changeCursor(cursor);
            ((TransactionsListAdapter) adapter).notifyDataSetChanged();
        }
    }

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new BlotterTotalCalculationTask(getContext(), db, blotterFilter, totalText);
    }
}
