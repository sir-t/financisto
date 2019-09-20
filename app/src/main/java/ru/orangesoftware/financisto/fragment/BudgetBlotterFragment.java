package ru.orangesoftware.financisto.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.Map;

import ru.orangesoftware.financisto.adapter.TransactionsRecyclerAdapter;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Total;

public class BudgetBlotterFragment extends BlotterFragment {

    private Map<Long, Category> categories;
    private Map<Long, Project> projects;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categories = MyEntity.asMap(db.getCategoriesList(true));
        projects = MyEntity.asMap(db.getActiveProjectsList(true));
        bFilter.setVisibility(View.GONE);
    }

    @Override
    protected Cursor createCursor() {
        long budgetId = blotterFilter.getBudgetId();
        return getBlotterForBudget(budgetId);
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null){
            setListAdapter(new TransactionsRecyclerAdapter(context, db, getCursor()));
        } else {
            ((TransactionsRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
        }
    }

    private Cursor getBlotterForBudget(long budgetId) {
        Budget b = db.load(Budget.class, budgetId);
        String where = Budget.createWhere(b, categories, projects);
        return db.getBlotterWithSplits(where);
    }

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new TotalCalculationTask(context, totalText) {
            @Override
            public Total getTotalInHomeCurrency() {
                long t0 = System.currentTimeMillis();
                try {
                    try {
                        long budgetId = blotterFilter.getBudgetId();
                        Budget b = db.load(Budget.class, budgetId);
                        Total total = new Total(b.getBudgetCurrency());
                        total.balance = db.fetchBudgetBalance(categories, projects, b);
                        return total;
                    } finally {
                        long t1 = System.currentTimeMillis();
                        Log.d("BUDGET TOTALS", (t1-t0)+"ms");
                    }
                } catch (Exception ex) {
                    Log.e("BudgetTotals", "Unexpected error", ex);
                    return Total.ZERO;
                }
            }

            @Override
            public Total[] getTotals() {
                return new Total[0];
            }
        };
    }

}
