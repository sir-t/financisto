package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import androidx.fragment.app.ListFragment;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.Report2DChartActivity;
import ru.orangesoftware.financisto.activity.ReportActivity;
import ru.orangesoftware.financisto.adapter.SummaryEntityListAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.graph.Report2DChart;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportType;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

public class ReportsListFragment extends ListFragment {

    public static final String EXTRA_REPORT_TYPE = "reportType";

    private ReportType[] reports;
    private Activity context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        context = getActivity();
        reports = getReportsList();

        setListAdapter(new SummaryEntityListAdapter(context, reports));

        return inflater.inflate(R.layout.reports_list, container, false);
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
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (reports[position].isConventionalBarReport()) {
            // Conventional Bars reports
            Intent intent = new Intent(context, ReportActivity.class);
            intent.putExtra(EXTRA_REPORT_TYPE, reports[position].name());
            startActivity(intent);
        } else {
            // 2D Chart reports
            Intent intent = new Intent(context, Report2DChartActivity.class);
            intent.putExtra(Report2DChart.REPORT_TYPE, reports[position].name());
            startActivity(intent);
        }
    }

    public static Report createReport(Context context, MyEntityManager em, Bundle extras) {
        String reportTypeName = extras.getString(EXTRA_REPORT_TYPE);
        ReportType reportType = ReportType.valueOf(reportTypeName);
        Currency c = em.getHomeCurrency();
        return reportType.createReport(context, c);
    }

    private ReportType[] getReportsList() {
        ArrayList<ReportType> reports = new ArrayList<>();

        reports.add(ReportType.BY_PERIOD);
        reports.add(ReportType.BY_CATEGORY);

        if (MyPreferences.isShowPayee(context)) {
            reports.add(ReportType.BY_PAYEE);
        }

        if (MyPreferences.isShowLocation(context)) {
            reports.add(ReportType.BY_LOCATION);
        }

        if (MyPreferences.isShowProject(context)) {
            reports.add(ReportType.BY_PROJECT);
        }

        reports.add(ReportType.BY_ACCOUNT_BY_PERIOD);
        reports.add(ReportType.BY_CATEGORY_BY_PERIOD);

        if (MyPreferences.isShowPayee(context)) {
            reports.add(ReportType.BY_PAYEE_BY_PERIOD);
        }

        if (MyPreferences.isShowLocation(context)) {
            reports.add(ReportType.BY_LOCATION_BY_PERIOD);
        }

        if (MyPreferences.isShowProject(context)) {
            reports.add(ReportType.BY_PROJECT_BY_PERIOD);
        }

        return reports.toArray(new ReportType[reports.size()]);
    }

}
