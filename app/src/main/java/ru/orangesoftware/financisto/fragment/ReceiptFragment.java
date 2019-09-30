package ru.orangesoftware.financisto.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.databinding.ReceiptItemBinding;
import ru.orangesoftware.financisto.databinding.ReceiptItemsListBinding;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSelection;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.ElectronicReceipt;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

import static android.app.Activity.RESULT_OK;

public class ReceiptFragment extends AbstractRecycleFragment implements ItemSelection {

    public static final String RESULT_AMOUNT = "RESULT_AMOUNT";

    private static final String ARG_CURRENCY_ID = "CURRENCY_ID";
    private static final String ARG_TRANSACTION_ID = "TRANSACTION_ID";
    private static final String ARG_QR_CODE = "QR_CODE";
    private static final String ARG_CHECK_STATUS = "CHECK_STATUS";
    private static final String ARG_REQUEST_STATUS = "REQUEST_STATUS";
    private static final String ARG_RECEIPT_DATA = "RECEIPT_DATA";
    private static final String ARG_SELECTION_MODE = "SELECTION_MODE";

    private long currencyId;
    private long transactionId;
    private ElectronicReceipt eReceipt;

    private String qrCodeStatus;
    private long checkStatus;
    private long requestStatus;
    private String receiptData;
    private JSONObject receiptJSON;

    private View selectionBottomBar;
    private TextView selectionCountText;
    private long selectedAmount;

    public static class Builder {
        final private Bundle args;

        public Builder() {
            args = new Bundle();
        }

        public Builder setCurrencyId(long currencyId) {
            args.putLong(ARG_CURRENCY_ID, currencyId);
            return this;
        }

        public Builder setTransactionId(long transactionId) {
            args.putLong(ARG_TRANSACTION_ID, transactionId);;
            return this;
        }

        public Builder setReceiptData(String qrCode, long checkStatus, long requestStatus, String receiptData) {
            args.putString(ARG_QR_CODE, qrCode);
            args.putLong(ARG_CHECK_STATUS, checkStatus);
            args.putLong(ARG_REQUEST_STATUS, requestStatus);
            args.putString(ARG_RECEIPT_DATA, receiptData);
            return this;
        }

        public Builder setSelectionMode(String selectionMode) {
            args.putString(ARG_SELECTION_MODE, selectionMode);
            return this;
        }

        public ReceiptFragment newInstance() {
            ReceiptFragment fragment = new ReceiptFragment();
            fragment.setArguments(args);
            return fragment;
        }
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

            selectionCountText = binding.selectionCount;
            selectionBottomBar = binding.selectionBottomBar;

            updateSelectionMode(SelectionMode.valueOf(bundle.getString(ARG_SELECTION_MODE, "OFF")));
            if (selectionMode != SelectionMode.OFF) {
                binding.topBar.setVisibility(View.GONE);
                selectionBottomBar.setVisibility(View.VISIBLE);
                binding.bCheckAll.setOnClickListener(arg0 -> checkAll());
                binding.bUncheckAll.setOnClickListener(arg0 -> uncheckAll());
                binding.bOK.setOnClickListener(arg0 -> {
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_AMOUNT, -selectedAmount);
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                });
            }
        }

        return v;
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null && this.receiptJSON != null) {
            setListAdapter(new ReceiptAdapter(this.receiptJSON));
            if (selectionMode != SelectionMode.OFF && getListAdapter() instanceof AdapterSelection) {
                checkAll();
            }
        }
    }

    @Override
    public void toggleSelectionMode(boolean hide) {

    }

    @Override
    public void updateCount(int count) {
        long amount = 0;
        for (ReceiptItem item : ((ReceiptAdapter) getListAdapter()).getCheckedItems()) {
            amount += item.getSum();
        }
        selectedAmount = amount;
        Currency ccache = CurrencyCache.getCurrency(db, currencyId);
        selectionCountText.setText(count + " -> " + Utils.amountToString(ccache, amount));
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

        void bind(ReceiptItem item, boolean checked) {
            mItem = item;

            mBinding.rowFG.setBackgroundResource(checked ? R.color.material_blue_gray : R.color.holo_gray_dark);
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

    private class ReceiptAdapter extends RecyclerView.Adapter<ReceiptHolder> implements AdapterSelection {

        private final JSONObject mJsonObject;

        private final JSONArray mItems;
        private final Set<Integer> checkedItems = new HashSet<>();

        private ReceiptAdapter(JSONObject jsonObject) {
            mJsonObject = jsonObject;
            JSONArray items = null;
            try {
                items = mJsonObject.getJSONArray("items");
            } catch (Exception ex) { }
            mItems = items;
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
                ReceiptItem item = new ReceiptItem(mItems.getJSONObject(position));
                holder.bind(item, checkedItems.contains(position));
            } catch (Exception ex) { }
        }

        @Override
        public int getItemCount() {
            return mItems.length();
        }

        @Override
        public void checkItem(int position) {
            boolean checked = !checkedItems.contains(position);
            if (checked) {
                checkedItems.add(position);
            } else {
                checkedItems.remove(position);
            }
            notifyItemChanged(position);
        }

        @Override
        public void checkAll() {
            checkedItems.clear();
            for (int i = 0; i < mItems.length(); i++)
                checkedItems.add(i);
            notifyDataSetChanged();
        }

        @Override
        public void uncheckAll() {
            checkedItems.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCheckedCount() {
            return checkedItems.size();
        }

        public List<ReceiptItem> getCheckedItems() {
            List<ReceiptItem> list = new LinkedList<>();
            try {
                for (int pos : checkedItems) {
                    list.add(new ReceiptItem(mItems.getJSONObject(pos)));
                }
            } catch (Exception ex) { }
            return list;
        }
    }

}
