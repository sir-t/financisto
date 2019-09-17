package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ReceiptActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
import ru.orangesoftware.financisto.databinding.ReceiptItemBinding;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.CategoryEntity;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.StringUtil;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;
import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.*;
import static ru.orangesoftware.financisto.model.Category.isSplit;
import static ru.orangesoftware.financisto.utils.TransactionTitleUtils.generateTransactionTitle;

public class ReceiptListFragment extends AbstractRecycleFragment implements ItemClick, ItemSwipeable {

	public ReceiptListFragment() {
		super(R.layout.receipts_list);
	}

	@Override
	protected Cursor createCursor() {
		return db.getBlotterWithOnlyReceipts();
	}

	@Override
	protected void updateAdapter() {
		if (getListAdapter() == null) {
			setListAdapter(new ReceiptRecyclerAdapter(db, context, getCursor()));
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

	private class ReceiptRecyclerItemHolder extends RecyclerView.ViewHolder {

		private final ReceiptItemBinding mBinding;

		ReceiptRecyclerItemHolder(ReceiptItemBinding binding) {
			super(binding.getRoot());
			mBinding = binding;
			mBinding.getRoot().findViewById(R.id.edit_task).setVisibility(View.GONE);
		}

		void bind(Transaction t, Cursor cursor, Context context) {
			final Utils u = new Utils(context);
			final Drawable icBlotterIncome = context.getResources().getDrawable(R.drawable.ic_action_arrow_left_bottom);
			final Drawable icBlotterExpense = context.getResources().getDrawable(R.drawable.ic_action_arrow_right_top);
			final Drawable icBlotterTransfer = context.getResources().getDrawable(R.drawable.ic_action_arrow_top_down);
			final Drawable icBlotterSplit = context.getResources().getDrawable(R.drawable.ic_action_share);

			String payee = cursor.getString(BlotterColumns.payee.ordinal());
			String note = t.note;
			String location = "";
			if (t.locationId > 0) {
				location = cursor.getString(BlotterColumns.location.ordinal());
			}
			String toAccount = cursor.getString(BlotterColumns.to_account_title.ordinal());
			long fromAmount = t.fromAmount;
			if (t.toAccountId > 0) {
				mBinding.top.setText(R.string.transfer);
				if (fromAmount > 0) {
					note = toAccount+" \u00BB";
				} else {
					note = "\u00AB "+toAccount;
				}
				mBinding.icon.setImageDrawable(icBlotterTransfer);
				mBinding.icon.setColorFilter(u.transferColor);
				mBinding.rightQrCode.setVisibility(View.GONE);
			} else {
				String title = cursor.getString(BlotterColumns.from_account_title.ordinal());
				mBinding.top.setText(title);
				mBinding.center.setTextColor(Color.WHITE);
				if (t.receipt.qr_code != null) {
					mBinding.rightQrCode.setVisibility(View.VISIBLE);
					String eReceiptData = t.receipt.response_data;
					if (eReceiptData != null && eReceiptData.startsWith("{")) {
						mBinding.rightQrCode.setColorFilter(Color.argb(255, 0, 255, 0));
					} else if (eReceiptData != null) {
						mBinding.rightQrCode.setColorFilter(Color.argb(255, 255, 0, 0));
					} else {
						mBinding.rightQrCode.setColorFilter(Color.argb(255, 255, 255, 255));
					}
				} else {
					mBinding.rightQrCode.setVisibility(View.GONE);
				}
			}

			long categoryId = t.categoryId;
			String category = "";
			if (categoryId != 0) {
				category = cursor.getString(BlotterColumns.category_title.ordinal());
			}
			StringBuilder sb = new StringBuilder();
			String text = generateTransactionTitle(sb, payee, note, location, categoryId, category);
			mBinding.center.setText(text);
			sb.setLength(0);

			long currencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
			Currency c = CurrencyCache.getCurrency(db, currencyId);
			long originalCurrencyId = t.originalCurrencyId;
			if (originalCurrencyId > 0) {
				Currency originalCurrency = CurrencyCache.getCurrency(db, originalCurrencyId);
				long originalAmount = t.originalFromAmount;
				u.setAmountText(sb, mBinding.rightCenter, originalCurrency, originalAmount, c, fromAmount, true);
			} else {
				u.setAmountText(mBinding.rightCenter, c, fromAmount, true);
			}


			if (isSplit(t.categoryId)) {
				mBinding.icon.setImageDrawable(icBlotterSplit);
				mBinding.icon.setColorFilter(u.splitColor);
			} else {
				int categoryType = cursor.getInt(BlotterColumns.category_type.ordinal());
				if ((fromAmount > 0) || (fromAmount == 0 && categoryType == CategoryEntity.TYPE_INCOME)) {
					mBinding.icon.setImageDrawable(icBlotterIncome);
					mBinding.icon.setColorFilter(u.positiveColor);
				} else if ((fromAmount < 0) || (fromAmount == 0 && categoryType == CategoryEntity.TYPE_EXPENSE)) {
					mBinding.icon.setImageDrawable(icBlotterExpense);
					mBinding.icon.setColorFilter(u.negativeColor);
				}
			}

			long date = t.dateTime;
			mBinding.bottom.setText(StringUtil.capitalize(DateUtils.formatDateTime(context, date,
					DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_ABBREV_MONTH|DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_ABBREV_WEEKDAY)));
			if (date > System.currentTimeMillis()) {
				u.setFutureTextColor(mBinding.bottom);
			} else {
				mBinding.bottom.setTextColor(mBinding.top.getTextColors().getDefaultColor());
			}

			long balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal());
			mBinding.right.setText(Utils.amountToString(c, balance, false));

			if (!MyPreferences.isShowRunningBalance(context)) {
				mBinding.right.setVisibility(View.GONE);
			}

			Resources r = context.getResources();
			TransactionStatus status = TransactionStatus.valueOf(cursor.getString(BlotterColumns.status.ordinal()));
			mBinding.indicator.setBackgroundColor(r.getColor(status.colorId));
		}

	}

	public class ReceiptRecyclerAdapter extends BaseCursorRecyclerAdapter<ReceiptRecyclerItemHolder> {

		private final DatabaseAdapter db;
		private final Context context;

		ReceiptRecyclerAdapter(DatabaseAdapter db, Context context, Cursor c) {
			super(c);
			this.db = db;
			this.context = context;
		}

		@NonNull
		@Override
		public ReceiptRecyclerItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			ReceiptItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.receipt_item, parent, false);
			return new ReceiptRecyclerItemHolder(binding);
		}

		@Override
		public void onBindViewHolder(ReceiptRecyclerItemHolder holder, Cursor cursor) {
			Transaction t = Transaction.fromBlotterCursor(cursor);
			holder.bind(t, cursor, context);
		}
	}

}
