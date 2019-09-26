package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment;
import ru.orangesoftware.financisto.fragment.ReceiptFragment;
import ru.orangesoftware.financisto.model.ElectronicReceipt;

public class ReceiptActivity extends SingleFragmentActivity {

    private static final String EXTRA_CURRENCY_ID = "receipt.currency_id";
    private static final String EXTRA_TRANSACTION_ID = "receipt.transaction_id";
    private static final String EXTRA_QR_CODE = "receipt.qr_code";
    private static final String EXTRA_CHECK_STATUS = "receipt.check_status";
    private static final String EXTRA_REQUEST_STATUS = "receipt.request_status";
    private static final String EXTRA_RESPONSE_DATA = "receipt.response_data";
    private static final String EXTRA_SELECTION_MODE = "receipt.selection_mode";

    @Override
    protected Fragment createFragment() {
        Intent intent = getIntent();

        if (intent != null) {
            ReceiptFragment.Builder b = new ReceiptFragment.Builder()
                    .setCurrencyId(intent.getLongExtra(EXTRA_CURRENCY_ID, 0));

            boolean selectionMode = intent.getBooleanExtra(EXTRA_SELECTION_MODE, false);
            if (selectionMode) {
                b.setSelectionMode(AbstractRecycleFragment.SelectionMode.ALWAYS_ON.name());
            }

            long transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1);
            if (transactionId != -1) {
                b.setTransactionId(transactionId);
            } else {
                b.setReceiptData(
                        intent.getStringExtra(EXTRA_QR_CODE),
                        intent.getLongExtra(EXTRA_CHECK_STATUS, 0),
                        intent.getLongExtra(EXTRA_REQUEST_STATUS, 0),
                        intent.getStringExtra(EXTRA_RESPONSE_DATA)
                );
            }

            return b.newInstance();
        }
        return null;
    }

    public static class Builder {

        final private Intent intent;

        public Builder(Context context, long transactionId) {
            intent = new Intent(context, ReceiptActivity.class);
            intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        }

        public Builder(Context context, ElectronicReceipt receipt) {
            intent = new Intent(context, ReceiptActivity.class);
            intent.putExtra(EXTRA_QR_CODE, receipt.qr_code);
            intent.putExtra(EXTRA_CHECK_STATUS, receipt.check_status);
            intent.putExtra(EXTRA_REQUEST_STATUS, receipt.request_status);
            intent.putExtra(EXTRA_RESPONSE_DATA, receipt.response_data);
        }

        public Builder setCurrencyId(long currencyId) {
            intent.putExtra(EXTRA_CURRENCY_ID, currencyId);
            return this;
        }

        public Builder setSelectionMode(boolean selectionMode) {
            intent.putExtra(EXTRA_SELECTION_MODE, selectionMode);
            return this;
        }

        public Intent build() {
            return intent;
        }
    }

}
