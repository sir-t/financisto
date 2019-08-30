package ru.orangesoftware.financisto.dialog;

import java.util.LinkedHashMap;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.fragment.BlotterFragment;

public class MassOperationsDialog extends AbstractDialogFragment {

    public MassOperationsDialog() {
        resourceResponseMap = new LinkedHashMap<>(4);
        resourceResponseMap.put(R.string.delete, BlotterFragment.MASS_OPERATION_DELETE);
        resourceResponseMap.put(R.string.clear, BlotterFragment.MASS_OPERATION_CLEAR);
        resourceResponseMap.put(R.string.transaction_status_pending, BlotterFragment.MASS_OPERATION_PENDING);
        resourceResponseMap.put(R.string.reconcile, BlotterFragment.MASS_OPERATION_RECONCILE);
    }
}
