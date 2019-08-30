package ru.orangesoftware.financisto.dialog;

import android.content.Context;

import java.util.LinkedHashMap;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_CATEGORY;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_LOCATION;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_PAYEE;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_PERIOD;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_PROJECT;

public class ReportTypeSelectionDialog extends AbstractDialogFragment {

    public ReportTypeSelectionDialog(Context context) {
        resourceResponseMap = new LinkedHashMap<>(4);
        resourceResponseMap.put(R.string.period, REPORT_TYPE_PERIOD);
        resourceResponseMap.put(R.string.category, REPORT_TYPE_CATEGORY);
        if (MyPreferences.isShowPayee(context))
            resourceResponseMap.put(R.string.payee, REPORT_TYPE_PAYEE);
        if (MyPreferences.isShowLocation(context))
            resourceResponseMap.put(R.string.location, REPORT_TYPE_LOCATION);
        if (MyPreferences.isShowProject(context))
            resourceResponseMap.put(R.string.project, REPORT_TYPE_PROJECT);
    }
}
