package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ReceiptActivity;
import ru.orangesoftware.financisto.adapter.BlotterRecyclerAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.databinding.BlotterListItemBinding;
import ru.orangesoftware.financisto.databinding.ReceiptsListBinding;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;

import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;

public class ReceiptListFragment extends BlotterFragment implements ItemClick, ItemSwipeable {

	public ReceiptListFragment() {
		super(R.layout.receipts_list);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// Replace super class

		ReceiptsListBinding binding = (ReceiptsListBinding) getBinding();

		blotterFilter = new WhereFilter("receipts");

		bSearch = binding.bSearch;
		bSearch.setOnClickListener(method -> {
			EditText searchText = binding.searchText;
			FrameLayout searchLayout = binding.searchTextFrame;
			InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

			searchText.setOnFocusChangeListener((v, b) -> {
				if (!v.hasFocus()) {
					imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
				}
			});

			binding.searchTextClear.setOnClickListener(v -> searchText.setText(""));

			if (searchLayout.getVisibility() == View.VISIBLE) {
				imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
				searchLayout.setVisibility(View.GONE);
				return;
			}

			searchLayout.setVisibility(View.VISIBLE);
			searchText.requestFocusFromTouch();
			imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);

			searchText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				}

				@Override
				public void afterTextChanged(Editable editable) {
					ImageButton clearButton = view.findViewById(R.id.search_text_clear);
					String text = editable.toString();
					blotterFilter.remove(BlotterFilter.RECEIPT_DATA);

					if (!text.isEmpty()) {
						blotterFilter.contains(BlotterFilter.RECEIPT_DATA, text);
						clearButton.setVisibility(View.VISIBLE);
					} else {
						clearButton.setVisibility(View.GONE);
					}

					recreateCursor();
				}
			});

			if (blotterFilter.get(BlotterFilter.RECEIPT_DATA) != null) {
				String searchFilterText = blotterFilter.get(BlotterFilter.RECEIPT_DATA).getStringValue();
				if (!searchFilterText.isEmpty()) {
					searchFilterText = searchFilterText.substring(1, searchFilterText.length() - 1);
					searchText.setText(searchFilterText);
				}
			}
		});
	}

	@Override
	protected Cursor createCursor() {
		return db.getBlotterWithOnlyReceipts(blotterFilter);
	}

	@Override
	protected void updateAdapter() {
		if (getListAdapter() == null) {
			setListAdapter(new ReceiptRecyclerAdapter(context, db, getCursor()));
		} else {
			((ReceiptRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			recreateCursor();
		}
	}

	@Override
	public void onItemClick(View view, int position) {
		if (getCursor().moveToPosition(position)) {
			Transaction t = Transaction.fromBlotterCursor(getCursor());
			if (t.receipt.response_data != null && t.receipt.response_data.startsWith("{")) {
				ReceiptActivity.Builder builder = new ReceiptActivity.Builder(context, t.id);
				Account a = db.getAccount(t.fromAccountId);
				if (a != null) {
					builder.setCurrencyId(a.currency.id);
				}
				startActivity(builder.build());
			}
		}
	}

	@Override
	public Integer[] getSwipeOptions() {
		return new Integer[]{R.id.delete_task};
	}

	@Override
	public void onSwipeClick(int viewID, int position) {
		long id = getListAdapter().getItemId(position);
		switch (viewID) {
			case R.id.delete_task:
				new AlertDialog.Builder(context)
						.setMessage(R.string.delete_receipt_confirm)
						.setPositiveButton(R.string.yes, (arg0, arg1) -> {
							db.deleteElectronicReceiptForTransaction(id);
							recreateCursor();
						})
						.setNegativeButton(R.string.no, null)
						.show();
				break;
		}
	}

	@Override
	protected void calculateTotals() {
		// do nothing
	}

	public class ReceiptRecyclerAdapter extends BlotterRecyclerAdapter {

		private final DatabaseAdapter db;
		private final Context context;

		ReceiptRecyclerAdapter(Context context, DatabaseAdapter db, Cursor c) {
			super(context, db, c);
			this.db = db;
			this.context = context;
		}

		@NonNull
		@Override
		public BlotterListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(context);
			BlotterListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.blotter_list_item, parent, false);
			binding.getRoot().findViewById(R.id.edit_task).setVisibility(View.GONE);
			return new BlotterListItemHolder(binding);
		}

	}

}
