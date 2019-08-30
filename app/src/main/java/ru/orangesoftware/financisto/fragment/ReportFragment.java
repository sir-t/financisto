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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.math.BigDecimal;

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
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.report.IncomeExpense;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportData;
import ru.orangesoftware.financisto.report.ReportType;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.Utils;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.report.ReportType.BY_CATEGORY;
import static ru.orangesoftware.financisto.report.ReportType.BY_LOCATION;
import static ru.orangesoftware.financisto.report.ReportType.BY_PAYEE;
import static ru.orangesoftware.financisto.report.ReportType.BY_PERIOD;
import static ru.orangesoftware.financisto.report.ReportType.BY_PROJECT;

public class ReportFragment extends ListFragment implements RefreshSupportedActivity {

    public static final String FILTER_INCOME_EXPENSE = "FILTER_INCOME_EXPENSE";
    public static final int CHANGE_REPORT_TYPE_REQUEST = 2000;
    public static final int REPORT_TYPE_PERIOD = 2001;
    public static final int REPORT_TYPE_CATEGORY = 2002;
    public static final int REPORT_TYPE_PAYEE = 2003;
    public static final int REPORT_TYPE_PROJECT = 2004;
    public static final int REPORT_TYPE_LOCATION = 2005;
    protected static final int FILTER_REQUEST = 1;
    private DatabaseAdapter db;
    private ImageButton bFilter;
    private ImageButton bToggle;
    private Report currentReport;
    private ReportAsyncTask reportTask;

    private WhereFilter filter = WhereFilter.empty();
    private boolean saveFilter = false;

    private IncomeExpense incomeExpenseState = IncomeExpense.BOTH;
    private View view;
    private FragmentActivity context;
    private Button bTypeSwitcher;

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

        switchReport(BY_PERIOD);

        FilterState.updateFilterColor(context, filter, bFilter);
        applyIncomeExpense();
        showOrRemoveTotals();
    }

    private void showSwitchTypeDialog() {
        ReportTypeSelectionDialog reportTypeSelectionDialog = new ReportTypeSelectionDialog(context);
        reportTypeSelectionDialog.setTargetFragment(this, CHANGE_REPORT_TYPE_REQUEST);
        reportTypeSelectionDialog.show(context.getSupportFragmentManager(), this.getClass().getName());
    }

    private void switchReport(ReportType type) {
        bTypeSwitcher.setText(type.getTitleId());
        currentReport = createReport(type);
        filter = new WhereFilter("Empty");
        FilterState.updateFilterColor(context, filter, bFilter);
        selectReport();
    }

    private SharedPreferences getPreferencesForReport() {
        return context.getSharedPreferences("ReportActivity_" + currentReport.reportType.name() + "_DEFAULT", 0);
    }

    private void toggleIncomeExpense() {
        IncomeExpense[] values = IncomeExpense.values();
        int nextIndex = incomeExpenseState.ordinal() + 1;
        incomeExpenseState = nextIndex < values.length ? values[nextIndex] : values[0];
        applyIncomeExpense();
        FilterState.updateFilterColor(context, filter, bFilter);
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
                FilterState.updateFilterColor(context, filter, bFilter);
                selectReport();
            } else if (resultCode == RESULT_OK) {
                filter = WhereFilter.fromIntent(data);
                FilterState.updateFilterColor(context, filter, bFilter);
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

    private class PieChartGeneratorTask extends AsyncTask<Void, Void, Intent> {

        @Override
        protected void onPreExecute() {
            context.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Intent doInBackground(Void... voids) {
            return createPieChart();
        }

        private Intent createPieChart() {
            DefaultRenderer renderer = new DefaultRenderer();
            renderer.setLabelsTextSize(getResources().getDimension(R.dimen.report_labels_text_size));
            renderer.setLegendTextSize(getResources().getDimension(R.dimen.report_legend_text_size));
            renderer.setMargins(new int[]{0, 0, 0, 0});
            ReportData report = currentReport.getReportForChart(db, WhereFilter.copyOf(filter));
            CategorySeries series = new CategorySeries("AAA");
            long total = Math.abs(report.total.amount) + Math.abs(report.total.balance);
            int[] colors = generateColors(2 * report.units.size());
            int i = 0;
            for (GraphUnit unit : report.units) {
                addSeries(series, renderer, unit.name, unit.getIncomeExpense().income, total, colors[i++]);
                addSeries(series, renderer, unit.name, unit.getIncomeExpense().expense, total, colors[i++]);
            }
            renderer.setZoomButtonsVisible(true);
            renderer.setZoomEnabled(true);
            renderer.setChartTitleTextSize(20);
            return ChartFactory.getPieChartIntent(context, series, renderer, getString(R.string.report));
        }

        public int[] generateColors(int n) {
            int[] colors = new int[n];
            for (int i = 0; i < n; i++) {
                colors[i] = Color.HSVToColor(new float[]{360 * (float) i / (float) n, .75f, .85f});
            }
            return colors;
        }

        private void addSeries(CategorySeries series, DefaultRenderer renderer, String name, BigDecimal expense, long total, int color) {
            long amount = expense.longValue();
            if (amount != 0 && total != 0) {
                long percentage = 100 * Math.abs(amount) / total;
                series.add((amount > 0 ? "+" : "-") + name + "(" + percentage + "%)", percentage);
                SimpleSeriesRenderer r = new SimpleSeriesRenderer();
                r.setColor(color);
                renderer.addSeriesRenderer(r);
            }
        }

        @Override
        protected void onPostExecute(Intent intent) {
            context.setProgressBarIndeterminateVisibility(false);
            startActivity(intent);
        }

    }

}
