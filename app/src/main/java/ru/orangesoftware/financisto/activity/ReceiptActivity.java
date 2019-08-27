package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.fragment.ReceiptFragment;

public class ReceiptActivity extends SingleFragmentActivity {

    private static final String EXTRA_CURRENCY_ID = "receipt.currency_id";
    private static final String EXTRA_RECEIPT_DATA = "receipt.receipt_data";

    @Override
    protected Fragment createFragment() {
        Intent intent = getIntent();
        if (intent != null) {
            return ReceiptFragment.newInstance(
                    intent.getLongExtra(EXTRA_CURRENCY_ID, 0),
                    intent.getStringExtra(EXTRA_RECEIPT_DATA)
            );
        }
        return null;
    }

    public static class Builder {

        private Intent intent;

        Builder(Context context, String receiptData) {
            intent = new Intent(context, ReceiptActivity.class);
            intent.putExtra(EXTRA_RECEIPT_DATA, receiptData);
        }

        Builder setCurrencyId(long currencyId) {
            intent.putExtra(EXTRA_CURRENCY_ID, currencyId);
            return this;
        }

        public Intent build() {
            return intent;
        }
    }

}
