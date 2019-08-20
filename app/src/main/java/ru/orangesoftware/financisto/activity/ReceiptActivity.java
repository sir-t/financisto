package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;

import org.json.JSONObject;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

public class ReceiptActivity extends AbstractListActivity {

    public static final String CURRENCY_ID = "CURRENCY_ID";
    public static final String RECEIPT_DATA = "RECEIPT_DATA";

    private long currencyId;
    private String receiptData;
    private JSONObject receiptJSON;

    private Button bBack;
    private Button bDelete;

    public ReceiptActivity() {
        super(R.layout.receipt_items_list);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        bBack = findViewById(R.id.bBack);
        bBack.setOnClickListener(view -> {

        });

        bDelete = findViewById(R.id.bDelete);
        bDelete.setOnClickListener(view -> {

        });

        Intent intent = getIntent();
        if (intent != null) {
            this.currencyId = intent.getLongExtra(CURRENCY_ID, 0);
            this.receiptData = intent.getStringExtra(RECEIPT_DATA);
            Log.i("Financisto", "data: " + this.receiptData + ", currency: " + this.currencyId);
            try {
                receiptJSON = new JSONObject(this.receiptData);
            } catch (Exception ex) {
                Log.e("Financisto", "jsonObject", ex);
            }
            try {
                receiptJSON = receiptJSON.getJSONObject("document").getJSONObject("receipt");
            } catch (Exception ex) {
                Log.e("Financisto", "jsonObject", ex);
            }
        }
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new ReceiptAdapter(this, this.receiptJSON);
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
    }

    private class ReceiptAdapter extends BaseAdapter {

        private final JSONObject jsonObject;
        private final Utils u;

        private ReceiptAdapter(Context context, JSONObject jsonObject) {
            this.jsonObject = jsonObject;
            this.u = new Utils(context);
        }

        @Override
        public int getCount() {
            try {
                return jsonObject.getJSONArray("items").length();
            } catch (Exception ex) {
                return 0;
            }
        }

        @Override
        public JSONObject getItem(int i) {
            try {
                return jsonObject.getJSONArray("items").getJSONObject(i);
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BlotterListAdapter.BlotterViewHolder v;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.blotter_list_item, parent, false);
                v = new BlotterListAdapter.BlotterViewHolder(convertView);
                convertView.setTag(v);
            } else {
                v = (BlotterListAdapter.BlotterViewHolder)convertView.getTag();
            }
            JSONObject c = getItem(position);
            try {
                Currency ccache = CurrencyCache.getCurrency(db, currencyId);
                v.topView.setText(c.getString("name"));
                v.centerView.setText(Utils.amountToString(ccache, c.getLong("sum"), false));
                v.bottomView.setVisibility(View.GONE);
                v.rightCenterView.setText(c.getString("quantity"));
                v.rightView.setText(Utils.amountToString(ccache, c.getLong("price"), false));
                v.indicator.setVisibility(View.GONE);
                v.iconView.setVisibility(View.GONE);
                v.eReceiptView.setVisibility(View.GONE);
            } catch (Exception ex) {
                Log.e("Financisto", "getView", ex);
            }
            return convertView;
        }

    }

}
