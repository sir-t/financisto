package ru.orangesoftware.financisto.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import ru.orangesoftware.financisto.adapter.TransactionsRecyclerAdapter;
import ru.orangesoftware.financisto.blotter.BlotterTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;

public class SplitsBlotterFragment extends BlotterFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bFilter.setVisibility(View.GONE);
    }

    @Override
    protected Cursor createCursor() {
        return db.getBlotterForAccountWithSplits(blotterFilter);
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null){
            setListAdapter(new TransactionsRecyclerAdapter(context, db, getCursor()));
        } else {
            ((TransactionsRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
        }
    }

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new BlotterTotalCalculationTask(getContext(), db, blotterFilter, totalText);
    }
}
