package ru.orangesoftware.financisto.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import org.json.JSONObject;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

public class ReceiptFragment extends AbstractListFragment {

    private static final String ARG_CURRENCY_ID = "CURRENCY_ID";
    private static final String ARG_RECEIPT_DATA = "RECEIPT_DATA";

    private long currencyId;
    private String receiptData;
    private JSONObject receiptJSON;

    private Button bBack;
    private Button bDelete;

    public static ReceiptFragment newInstance(long currencyId, String receiptData) {
        Bundle args = new Bundle();
        args.putLong(ARG_CURRENCY_ID, currencyId);
        args.putString(ARG_RECEIPT_DATA, receiptData);

        ReceiptFragment fragment = new ReceiptFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private ReceiptFragment() {
        super(R.layout.receipt_items_list);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        bBack = view.findViewById(R.id.bBack);
        bDelete = view.findViewById(R.id.bDelete);

        Bundle bundle = getArguments();
        if (bundle != null) {
            currencyId = bundle.getLong(ARG_CURRENCY_ID, 0);
            receiptData = bundle.getString(ARG_RECEIPT_DATA);
            try {
                receiptJSON = new JSONObject(this.receiptData);
                receiptJSON = receiptJSON.getJSONObject("document").getJSONObject("receipt");
            } catch (Exception ex) {
                Log.e("Financisto", "jsonObject", ex);
            }
        }

        return view;
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected void updateAdapter() {
        if (adapter == null) {
            adapter = new ReceiptAdapter(context, this.receiptJSON);
            setListAdapter(adapter);
        }
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

        private ReceiptAdapter(Context context, JSONObject jsonObject) {
            this.jsonObject = jsonObject;
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
