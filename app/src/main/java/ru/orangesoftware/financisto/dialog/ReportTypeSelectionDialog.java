package ru.orangesoftware.financisto.dialog;

import java.util.LinkedHashMap;

import ru.orangesoftware.financisto.R;

import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_CATEGORY;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_PAYEE;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_PERIOD;
import static ru.orangesoftware.financisto.fragment.ReportFragment.REPORT_TYPE_PROJECT;

public class ReportTypeSelectionDialog extends AbstractDialogFragment {

    public ReportTypeSelectionDialog() {
        resourceResponseMap = new LinkedHashMap<>(4);
        resourceResponseMap.put(R.string.period, REPORT_TYPE_PERIOD);
        resourceResponseMap.put(R.string.category, REPORT_TYPE_CATEGORY);
        resourceResponseMap.put(R.string.payee, REPORT_TYPE_PAYEE);
        resourceResponseMap.put(R.string.project, REPORT_TYPE_PROJECT);
    }
}
