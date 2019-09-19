package ru.orangesoftware.financisto.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.BudgetActivity;
import ru.orangesoftware.financisto.activity.BudgetBlotterActivity;
import ru.orangesoftware.financisto.activity.BudgetListTotalsDetailsActivity;
import ru.orangesoftware.financisto.activity.DateFilterActivity;
import ru.orangesoftware.financisto.activity.FilterState;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.databinding.BudgetListBinding;
import ru.orangesoftware.financisto.databinding.BudgetListItemBinding;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.db.BudgetsTotalCalculator;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.Utils;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;

public class BudgetListFragment extends AbstractRecycleFragment implements ItemClick, ItemSwipeable {

    private static final int NEW_BUDGET_REQUEST = 1;
    private static final int EDIT_BUDGET_REQUEST = 2;
    private static final int VIEW_BUDGET_REQUEST = 3;
    private static final int FILTER_BUDGET_REQUEST = 4;

    private ImageButton bFilter;

    private WhereFilter filter = WhereFilter.empty();

    public BudgetListFragment() {
        super(R.layout.budget_list);
    }

    private ArrayList<Budget> budgets = new ArrayList<>();
    private Handler handler;
    private BudgetTotalsCalculationTask totalCalculationTask;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        budgets = db.getAllBudgets(filter);
        handler = new Handler();

        TextView totalText = view.findViewById(R.id.total);
        totalText.setOnClickListener(v -> showTotals());

        BudgetListBinding binding = (BudgetListBinding) getBinding();
        binding.bAdd.setOnClickListener(v -> {
            Intent intent = new Intent(context, BudgetActivity.class);
            startActivityForResult(intent, NEW_BUDGET_REQUEST);
        });
        bFilter = binding.bFilter;
        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(context, DateFilterActivity.class);
            filter.toIntent(intent);
            startActivityForResult(intent, FILTER_BUDGET_REQUEST);
        });

        if (filter.isEmpty()) {
            filter.put(new DateTimeCriteria(PeriodType.THIS_MONTH));
        }

        applyFilter();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILTER_BUDGET_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                filter.clear();
            } else if (resultCode == RESULT_OK) {
                String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
                PeriodType p = PeriodType.valueOf(periodType);
                if (PeriodType.CUSTOM == p) {
                    long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
                    long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
                    filter.put(new DateTimeCriteria(periodFrom, periodTo));
                } else {
                    filter.put(new DateTimeCriteria(p));
                }
            }
        }
        updateAdapter();
    }

    @Override
    public void updateAdapter() {
        budgets = db.getAllBudgets(filter);
        calculateTotals();

        if (getListAdapter() == null) {
            setListAdapter(new BudgetRecyclerAdapter(budgets));
        } else {
            ((BudgetRecyclerAdapter) getListAdapter()).setBudgets(budgets);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        long id = getListAdapter().getItemId(position);
        Budget b = db.load(Budget.class, id);
        Intent intent = new Intent(context, BudgetBlotterActivity.class);
        Criteria.eq(BlotterFilter.BUDGET_ID, String.valueOf(id)).toIntent(b.title, intent);
        startActivityForResult(intent, VIEW_BUDGET_REQUEST);
    }

    @Override
    public Integer[] getSwipeOptions() {
        return new Integer[]{R.id.delete_task, R.id.edit_task};
    }

    @Override
    public void onSwipeClick(int viewID, int position) {
        long id = getListAdapter().getItemId(position);
        switch (viewID) {
            case R.id.delete_task:
                deleteItem(id);
                break;
            case R.id.edit_task:
                editItem(id);
                break;
        }
    }

    private void showTotals() {
        Intent intent = new Intent(context, BudgetListTotalsDetailsActivity.class);
        filter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    private void applyFilter() {
        FilterState.updateFilterColor(context, filter, bFilter);
    }

    private void calculateTotals() {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        TextView totalText = getView().findViewById(R.id.total);
        totalCalculationTask = new BudgetTotalsCalculationTask(totalText);
        totalCalculationTask.execute((Void[]) null);
    }

    private void deleteItem(final long id) {
        final Budget b = db.load(Budget.class, id);
        if (b.parentBudgetId > 0) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.delete_budget_recurring_select)
                    .setPositiveButton(R.string.delete_budget_one_entry, (arg0, arg1) -> {
                        db.deleteBudgetOneEntry(id);
                        updateAdapter();
                    })
                    .setNeutralButton(R.string.delete_budget_all_entries, (arg0, arg1) -> {
                        db.deleteBudget(b.parentBudgetId);
                        updateAdapter();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            RecurUtils.Recur recur = RecurUtils.createFromExtraString(b.recur);
            new AlertDialog.Builder(context)
                    .setMessage(recur.interval == RecurUtils.RecurInterval.NO_RECUR ? R.string.delete_budget_confirm : R.string.delete_budget_recurring_confirm)
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                        db.deleteBudget(id);
                        updateAdapter();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    public void editItem(final long id) {
        Budget b = db.load(Budget.class, id);
        RecurUtils.Recur recur = b.getRecur();
        if (recur.interval != RecurUtils.RecurInterval.NO_RECUR) {
            Toast t = Toast.makeText(context, R.string.edit_recurring_budget, Toast.LENGTH_LONG);
            t.show();
        }
        Intent intent = new Intent(context, BudgetActivity.class);
        intent.putExtra(BudgetActivity.BUDGET_ID_EXTRA, b.parentBudgetId > 0 ? b.parentBudgetId : id);
        startActivityForResult(intent, EDIT_BUDGET_REQUEST);
    }

    private class BudgetItemHolder extends RecyclerView.ViewHolder {

        private final BudgetListItemBinding mBinding;

        public BudgetItemHolder(BudgetListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(Budget b) {
            mBinding.bottom.setText("*/*");
            mBinding.center.setText(b.title);

            Currency c = b.getBudgetCurrency();
            long amount = b.amount;
            mBinding.rightCenter.setText(Utils.amountToString(c, Math.abs(amount)));

            StringBuilder sb = new StringBuilder();

            sb.setLength(0);
            RecurUtils.Recur r = b.getRecur();
            if (r.interval != RecurUtils.RecurInterval.NO_RECUR) {
                sb.append("#").append(b.recurNum+1).append(" ");
            }
            sb.append(DateUtils.formatDateRange(context, b.startDate, b.endDate,
                    DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_ABBREV_MONTH));
            mBinding.top.setText(sb.toString());

            if (b.updated) {
                Utils u = new Utils(context);

                long spent = b.spent;
                long balance = amount+spent;
                u.setAmountText(mBinding.right1, c, balance, false);
                u.setAmountText(mBinding.right2, c, spent, false);

                sb.setLength(0);
                String categoriesText = b.categoriesText;
                if (Utils.isEmpty(categoriesText)) {
                    sb.append("*");
                } else {
                    sb.append( categoriesText);
                }
                if (b.includeSubcategories) {
                    sb.append("/*");
                }
                if (!Utils.isEmpty(b.projectsText)) {
                    sb.append(" [").append(b.projectsText).append("]");
                }
                mBinding.bottom.setText(sb.toString());
                if (b.amount > 0) {
                    mBinding.progress.setMax((int)Math.abs(b.amount));
                    mBinding.progress.setProgress((int)(balance-1));
                } else {
                    mBinding.progress.setMax((int)Math.abs(b.amount));
                    mBinding.progress.setProgress((int)(spent-1));
                }
            } else {
                mBinding.right1.setText(R.string.calculating);
                mBinding.right2.setText(R.string.calculating);
                mBinding.progress.setMax(1);
                mBinding.progress.setSecondaryProgress(0);
                mBinding.progress.setProgress(0);
            }
            mBinding.progress.setVisibility(View.VISIBLE);
        }

    }

    public class BudgetRecyclerAdapter extends RecyclerView.Adapter<BudgetItemHolder> {

        private List<Budget> budgets;

        public BudgetRecyclerAdapter(List<Budget> budgets) {
            this.budgets = budgets;
        }

        @NonNull
        @Override
        public BudgetItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            BudgetListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.budget_list_item, parent, false);
            return new BudgetItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull BudgetItemHolder holder, int position) {
            Budget b = budgets.get(position);
            holder.bind(b);
        }

        @Override
        public int getItemCount() {
            return budgets.size();
        }

        @Override
        public long getItemId(int i) {
            return budgets.get(i).id;
        }

        public void setBudgets(List<Budget> budgets) {
            this.budgets = budgets;
            notifyDataSetChanged();
        }

    }

    public class BudgetTotalsCalculationTask extends AsyncTask<Void, Total, Total> {

        private volatile boolean isRunning = true;

        private final TextView totalText;

        public BudgetTotalsCalculationTask(TextView totalText) {
            this.totalText = totalText;
        }

        @Override
        protected Total doInBackground(Void... params) {
            try {
                BudgetsTotalCalculator c = new BudgetsTotalCalculator(db, budgets);
                c.updateBudgets(handler);
                return c.calculateTotalInHomeCurrency();
            } catch (Exception ex) {
                Log.e("BudgetTotals", "Unexpected error", ex);
                return Total.ZERO;
            }

        }

        @Override
        protected void onPostExecute(Total result) {
            if (isRunning) {
                Utils u = new Utils(context);
                u.setTotal(totalText, result);
                ((BudgetRecyclerAdapter) getListAdapter()).notifyDataSetChanged();
            }
        }

        public void stop() {
            isRunning = false;
        }

    }


}
