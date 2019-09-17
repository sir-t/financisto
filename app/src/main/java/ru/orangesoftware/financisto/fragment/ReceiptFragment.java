package ru.orangesoftware.financisto.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.databinding.ReceiptItemBinding;
import ru.orangesoftware.financisto.databinding.ReceiptItemsListBinding;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.ElectronicReceipt;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

public class ReceiptFragment extends AbstractRecycleFragment {

    private static final String ARG_CURRENCY_ID = "CURRENCY_ID";
    private static final String ARG_TRANSACTION_ID = "TRANSACTION_ID";
    private static final String ARG_QR_CODE = "QR_CODE";
    private static final String ARG_CHECK_STATUS = "CHECK_STATUS";
    private static final String ARG_REQUEST_STATUS = "REQUEST_STATUS";
    private static final String ARG_RECEIPT_DATA = "RECEIPT_DATA";

    private long currencyId;
    private long transactionId;
    private ElectronicReceipt eReceipt;

    private String qrCodeStatus;
    private long checkStatus;
    private long requestStatus;
    private String receiptData;
    private JSONObject receiptJSON;

    public static ReceiptFragment newInstance(long currencyId, long transactionId) {
        Bundle args = new Bundle();
        args.putLong(ARG_CURRENCY_ID, currencyId);
        args.putLong(ARG_TRANSACTION_ID, transactionId);

        ReceiptFragment fragment = new ReceiptFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ReceiptFragment newInstance(long currencyId, String qrCode, long checkStatus, long requestStatus, String receiptData) {
        Bundle args = new Bundle();
        args.putLong(ARG_CURRENCY_ID, currencyId);
        args.putString(ARG_QR_CODE, qrCode);
        args.putLong(ARG_CHECK_STATUS, checkStatus);
        args.putLong(ARG_REQUEST_STATUS, requestStatus);
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
        View v = super.onCreateView(inflater, container, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            currencyId = bundle.getLong(ARG_CURRENCY_ID, 0);
            transactionId = bundle.getLong(ARG_TRANSACTION_ID, -1);
            if (transactionId != -1) {
                eReceipt = db.getElectronicReceiptForTransaction(transactionId);
                qrCodeStatus = eReceipt.qr_code != null ? "OK" : "null";
                checkStatus = eReceipt.check_status;
                requestStatus = eReceipt.request_status;
                receiptData = eReceipt.response_data;
            } else {
                qrCodeStatus = bundle.getString(ARG_QR_CODE, null) != null ? "OK" : "null";
                checkStatus = bundle.getLong(ARG_CHECK_STATUS);
                requestStatus = bundle.getLong(ARG_REQUEST_STATUS);
                receiptData = bundle.getString(ARG_RECEIPT_DATA);
            }

            try {
                receiptJSON = new JSONObject(receiptData);
                receiptJSON = receiptJSON.getJSONObject("document").getJSONObject("receipt");
            } catch (Exception ex) {
                Log.e("Financisto", "jsonObject", ex);
            }

            ReceiptItemsListBinding binding = (ReceiptItemsListBinding) getBinding();
            binding.bQrCode.setText("QRCode: " + qrCodeStatus);
            binding.bCheckStatus.setText("Check: " + checkStatus);
            binding.bRequestStatus.setText("Request: " + requestStatus);
        }

        return v;
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null) {
            setListAdapter(new ReceiptAdapter(this.receiptJSON));
        }
    }

    private class ReceiptItem {

        private String mName;
        private long mSum;
        private String mQuantity;
        private long mPrice;

        ReceiptItem(JSONObject jsonObject) {
            try {
                mName = jsonObject.getString("name");
                mSum = jsonObject.getLong("sum");
                mQuantity = jsonObject.getString("quantity");
                mPrice = jsonObject.getLong("price");
            } catch (Exception ex) {
                Log.e("Financisto", "getView", ex);
            }
        }

        public String getName() {
            return mName;
        }

        public long getSum() {
            return mSum;
        }

        public String getQuantity() {
            return mQuantity;
        }

        public long getPrice() {
            return mPrice;
        }
    }

    private class ReceiptHolder extends RecyclerView.ViewHolder {

        private ReceiptItem mItem;
        private final ReceiptItemBinding mBinding;

        private final Currency ccache;

        ReceiptHolder(ReceiptItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            ccache = CurrencyCache.getCurrency(db, currencyId);
        }

        void bind(ReceiptItem item) {
            mItem = item;

            mBinding.top.setText(mItem.getName());
            mBinding.center.setText(Utils.amountToString(ccache, mItem.getSum(),false));
            mBinding.bottom.setVisibility(View.GONE);
            mBinding.rightCenter.setText(mItem.getQuantity());
            mBinding.right.setText(Utils.amountToString(ccache, mItem.getPrice(),false));
            mBinding.indicator.setVisibility(View.GONE);
            mBinding.icon.setVisibility(View.GONE);
            mBinding.rightQrCode.setVisibility(View.GONE);
        }

    }

    private class ReceiptAdapter extends RecyclerView.Adapter<ReceiptHolder> {

        private final JSONObject mJsonObject;

        private ReceiptAdapter(JSONObject jsonObject) {
            mJsonObject = jsonObject;
        }

        @NonNull
        @Override
        public ReceiptHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            ReceiptItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.receipt_item, parent, false);
            return new ReceiptHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ReceiptHolder holder, int position) {
            try {
                ReceiptItem item = new ReceiptItem(mJsonObject.getJSONArray("items").getJSONObject(position));
                holder.bind(item);
            } catch (Exception ex) { }
        }

        @Override
        public int getItemCount() {
            try {
                return mJsonObject.getJSONArray("items").length();
            } catch (Exception ex) {
                return 0;
            }
        }

    }

}
