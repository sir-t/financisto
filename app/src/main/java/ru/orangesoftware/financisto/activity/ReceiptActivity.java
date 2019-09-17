package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.fragment.ReceiptFragment;

public class ReceiptActivity extends SingleFragmentActivity {

    private static final String EXTRA_CURRENCY_ID = "receipt.currency_id";
    private static final String EXTRA_TRANSACTION_ID = "receipt.transaction_id";
    private static final String EXTRA_QR_CODE = "receipt.qr_code";
    private static final String EXTRA_CHECK_STATUS = "receipt.check_status";
    private static final String EXTRA_REQUEST_STATUS = "receipt.request_status";
    private static final String EXTRA_RESPONSE_DATA = "receipt.response_data";

    @Override
    protected Fragment createFragment() {
        Intent intent = getIntent();
        if (intent != null && intent.getLongExtra(EXTRA_TRANSACTION_ID, -1) != -1) {
            return ReceiptFragment.newInstance(
                    intent.getLongExtra(EXTRA_CURRENCY_ID, 0),
                    intent.getLongExtra(EXTRA_TRANSACTION_ID, -1)
            );
        } else if (intent != null) {
            return ReceiptFragment.newInstance(
                    intent.getLongExtra(EXTRA_CURRENCY_ID, 0),
                    intent.getStringExtra(EXTRA_QR_CODE),
                    intent.getLongExtra(EXTRA_CHECK_STATUS, 0),
                    intent.getLongExtra(EXTRA_REQUEST_STATUS, 0),
                    intent.getStringExtra(EXTRA_RESPONSE_DATA)
            );
        }
        return null;
    }

    public static class Builder {

        final private Intent intent;

        public Builder(Context context, long transactionId) {
            intent = new Intent(context, ReceiptActivity.class);
            intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
        }

        public Builder(Context context, String qrCode, long checkStatus, long requestStatus, String responseData) {
            intent = new Intent(context, ReceiptActivity.class);
            intent.putExtra(EXTRA_QR_CODE, qrCode);
            intent.putExtra(EXTRA_CHECK_STATUS, checkStatus);
            intent.putExtra(EXTRA_REQUEST_STATUS, requestStatus);
            intent.putExtra(EXTRA_RESPONSE_DATA, responseData);
        }

        public Builder setCurrencyId(long currencyId) {
            intent.putExtra(EXTRA_CURRENCY_ID, currencyId);
            return this;
        }

        public Intent build() {
            return intent;
        }
    }

}
