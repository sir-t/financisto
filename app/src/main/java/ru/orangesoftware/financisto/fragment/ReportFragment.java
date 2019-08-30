/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.FilterState;
import ru.orangesoftware.financisto.activity.RefreshSupportedActivity;
import ru.orangesoftware.financisto.activity.ReportFilterActivity;
import ru.orangesoftware.financisto.adapter.ReportAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.dialog.ReportTypeSelectionDialog;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.report.IncomeExpense;
import ru.orangesoftware.financisto.report.PeriodReport;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportData;
import ru.orangesoftware.financisto.report.ReportType;
import ru.orangesoftware.financisto.report.SubCategoryReport;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.Utils;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.report.ReportType.BY_CATEGORY;
import static ru.orangesoftware.financisto.report.ReportType.BY_LOCATION;
import static ru.orangesoftware.financisto.report.ReportType.BY_PAYEE;
import static ru.orangesoftware.financisto.report.ReportType.BY_PERIOD;
import static ru.orangesoftware.financisto.report.ReportType.BY_PROJECT;
import static ru.orangesoftware.financisto.report.ReportType.BY_SUB_CATEGORY;

public class ReportFragment extends ListFragment implements RefreshSupportedActivity {

    private static final String ARG_FILTER = "filter";
    private static final String ARG_REPORT_TYPE = "report_type";
    private static final String ARG_FILTER_INCOME_EXPENSE = "filter_income_expense";
    private static final String PREF_FILTER_INCOME_EXPENSE = "FILTER_INCOME_EXPENSE";

    private static final int FILTER_REQUEST = 1;
    private static final int CHANGE_REPORT_TYPE_REQUEST = 2000;
    public static final int REPORT_TYPE_PERIOD = 2001;
    public static final int REPORT_TYPE_CATEGORY = 2002;
    public static final int REPORT_TYPE_PAYEE = 2003;
    public static final int REPORT_TYPE_PROJECT = 2004;
    public static final int REPORT_TYPE_LOCATION = 2005;

    private DatabaseAdapter db;
    private ImageButton bFilter;
    private ImageButton bToggle;
    private Report currentReport;
    private ReportAsyncTask reportTask;

    private WhereFilter filter = WhereFilter.empty();
    private boolean needSaveFilter = false;

    private IncomeExpense incomeExpenseState = IncomeExpense.BOTH;
    private View view;
    private FragmentActivity context;
    private Button bTypeSwitcher;

    public static ReportFragment newInstance(Bundle filterBundle, String reportType, String incomeExpenseName) {
        Bundle args = new Bundle();
        args.putBundle(ARG_FILTER, filterBundle);
        args.putString(ARG_REPORT_TYPE, reportType);
        args.putString(ARG_FILTER_INCOME_EXPENSE, incomeExpenseName);

        ReportFragment fragment = new ReportFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();

        db = new DatabaseAdapter(context);
        db.open();
    }

    public Report createReport(ReportType type) {
        String reportTypeName = type.name();
        ReportType reportType = ReportType.valueOf(reportTypeName);
        Currency c = db.getHomeCurrency();
        return reportType.createReport(context, c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.report, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bFilter = view.findViewById(R.id.bFilter);

        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportFilterActivity.class);
            filter.toIntent(intent);
            startActivityForResult(intent, FILTER_REQUEST);
        });

        bToggle = view.findViewById(R.id.bToggle);
        bToggle.setOnClickListener(v -> toggleIncomeExpense());

        bTypeSwitcher = view.findViewById(R.id.bTypeSwitcher);
        bTypeSwitcher.setOnClickListener(a -> showSwitchTypeDialog());

        String reportTypeName = MyPreferences.getLastReportType(context, BY_PERIOD.name());
        Bundle bundle = getArguments();
        if (bundle != null) {
            Bundle filterBundle = bundle.getBundle(ARG_FILTER);
            if (filterBundle != null) {
                filter = WhereFilter.fromBundle(filterBundle);
                String incomeExpenseName = bundle.getString(ARG_FILTER_INCOME_EXPENSE, null);
                if (incomeExpenseName != null)
                    incomeExpenseState = IncomeExpense.valueOf(incomeExpenseName);
            }
            String reportType = bundle.getString(ARG_REPORT_TYPE);
            if (reportType != null)
                reportTypeName = reportType;
        }
        switchReport(ReportType.valueOf(reportTypeName), true);

        applyIncomeExpense();
        showOrRemoveTotals();
    }

    private void showSwitchTypeDialog() {
        ReportTypeSelectionDialog reportTypeSelectionDialog = new ReportTypeSelectionDialog(context);
        reportTypeSelectionDialog.setTargetFragment(this, CHANGE_REPORT_TYPE_REQUEST);
        reportTypeSelectionDialog.show(context.getSupportFragmentManager(), this.getClass().getName());
    }

    private void switchReport(ReportType type, boolean firstStart) {
        saveFilter(false);
        bTypeSwitcher.setText(type.getTitleId());
        currentReport = createReport(type);
        if (!firstStart)
            MyPreferences.setLastReportType(context, type.name());
        if ((type == BY_SUB_CATEGORY) || (firstStart && !filter.isEmpty()))
            applyFilter();
        else
            loadPrefsFilter();
        selectReport();
    }

    private void switchReport(ReportType type) {
        switchReport(type, false);
    }

    private SharedPreferences getPreferencesForReport() {
        return context.getSharedPreferences("ReportActivity_" + currentReport.reportType.name() + "_DEFAULT", 0);
    }

    private void toggleIncomeExpense() {
        IncomeExpense[] values = IncomeExpense.values();
        int nextIndex = incomeExpenseState.ordinal() + 1;
        incomeExpenseState = nextIndex < values.length ? values[nextIndex] : values[0];
        applyIncomeExpense();
        saveFilter(true);
        selectReport();
    }

    private void applyIncomeExpense() {
        String reportTitle = getString(currentReport.reportType.titleId);
        String incomeExpenseTitle = getString(incomeExpenseState.getTitleId());
        context.setTitle(reportTitle + " (" + incomeExpenseTitle + ")");
        bToggle.setImageDrawable(getResources().getDrawable(incomeExpenseState.getIconId()));
    }

    @Override
    public void onPause() {
        super.onPause();
        PinProtection.lock(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        PinProtection.unlock(context);
    }

    private void showOrRemoveTotals() {
        if (!currentReport.shouldDisplayTotal()) {
            view.findViewById(R.id.total).setVisibility(View.GONE);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (currentReport != null) {
            Intent intent = currentReport.createActivityIntent(context, db, WhereFilter.copyOf(filter), id);
            startActivity(intent);
        }
    }

    private void selectReport() {
        cancelCurrentReportTask();
        reportTask = new ReportAsyncTask(currentReport, incomeExpenseState);
        reportTask.execute();
    }

    private void cancelCurrentReportTask() {
        if (reportTask != null) {
            reportTask.cancel(true);
        }
    }


    @Override
    public void onDestroy() {
        cancelCurrentReportTask();
        db.close();
        super.onDestroy();
    }

    @Override
    public void recreateCursor() {
        selectReport();
    }

    @Override
    public void integrityCheck() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILTER_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                filter.clear();
                saveFilter(true);
                selectReport();
            } else if (resultCode == RESULT_OK) {
                filter = WhereFilter.fromIntent(data);
                saveFilter(true);
                selectReport();
            }
        }
        if (requestCode == CHANGE_REPORT_TYPE_REQUEST) {
            switch (resultCode) {
                case REPORT_TYPE_PERIOD:
                    switchReport(BY_PERIOD);
                    break;
                case REPORT_TYPE_CATEGORY:
                    switchReport(BY_CATEGORY);
                    break;
                case REPORT_TYPE_PAYEE:
                    switchReport(BY_PAYEE);
                    break;
                case REPORT_TYPE_PROJECT:
                    switchReport(BY_PROJECT);
                    break;
                case REPORT_TYPE_LOCATION:
                    switchReport(BY_LOCATION);
                    break;
            }
        }
    }

    private void applyFilter() {
        bFilter.setEnabled(!(currentReport instanceof PeriodReport));
        FilterState.updateFilterColor(context, filter, bFilter);
        bTypeSwitcher.setVisibility(currentReport instanceof SubCategoryReport ? View.GONE : View.VISIBLE);
    }

    private void saveFilter(boolean needApplyFilter) {
        if (needSaveFilter) {
            SharedPreferences preferences = getPreferencesForReport();
            filter.toSharedPreferences(preferences);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_FILTER_INCOME_EXPENSE, incomeExpenseState.name());
            editor.apply();
        }
        if (needApplyFilter)
            applyFilter();
    }

    private void loadPrefsFilter() {
        SharedPreferences preferences = getPreferencesForReport();
        filter = WhereFilter.fromSharedPreferences(preferences);
        incomeExpenseState = IncomeExpense.valueOf(preferences.getString(PREF_FILTER_INCOME_EXPENSE, IncomeExpense.BOTH.name()));
        needSaveFilter = true;
        applyFilter();
    }

    private void displayTotal(Total total) {
        if (currentReport.shouldDisplayTotal()) {
            TextView totalText = view.findViewById(R.id.total);
            Utils u = new Utils(context);
            u.setTotal(totalText, total);
        }
    }

    private class ReportAsyncTask extends AsyncTask<Void, Void, ReportData> {

        private final Report report;
        private final IncomeExpense incomeExpense;

        private ReportAsyncTask(Report report, IncomeExpense incomeExpense) {
            this.report = report;
            this.incomeExpense = incomeExpense;
        }

        @Override
        protected void onPreExecute() {
            context.setProgressBarIndeterminateVisibility(true);
            ((TextView) view.findViewById(android.R.id.empty)).setText(R.string.calculating);
        }

        @Override
        protected ReportData doInBackground(Void... voids) {
            report.setIncomeExpense(incomeExpense);
            return report.getReport(db, WhereFilter.copyOf(filter));
        }

        @Override
        protected void onPostExecute(ReportData data) {
            context.setProgressBarIndeterminateVisibility(false);
            displayTotal(data.total);
            ((TextView) view.findViewById(android.R.id.empty)).setText(R.string.empty_report);
            ReportAdapter adapter = new ReportAdapter(context, data.units);
            setListAdapter(adapter);
        }

    }

}
