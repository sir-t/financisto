package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ReceiptActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
import ru.orangesoftware.financisto.databinding.ReceiptItemBinding;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.StringUtil;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;
import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import static ru.orangesoftware.financisto.utils.TransactionTitleUtils.generateTransactionTitle;

public class ReceiptListFragment extends AbstractRecycleFragment implements ItemClick {

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
			if (t.eReceiptData != null && t.eReceiptData.startsWith("{")) {
				ReceiptActivity.Builder builder = new ReceiptActivity.Builder(context, t.eReceiptData);
				builder.setCurrencyId(t.originalCurrencyId);
//				if (isDifferentCurrency() || selectedAccount != null)
//					builder.setCurrencyId(isDifferentCurrency() ? selectedOriginCurrencyId : selectedAccount.currency.id);
				startActivity(builder.build());
			}
		}
	}

	private class ReceiptRecyclerItemHolder extends RecyclerView.ViewHolder {

		private final ReceiptItemBinding mBinding;

		ReceiptRecyclerItemHolder(ReceiptItemBinding binding) {
			super(binding.getRoot());
			mBinding = binding;
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
				mBinding.eReceipt.setVisibility(View.GONE);
			} else {
				String title = cursor.getString(BlotterColumns.from_account_title.ordinal());
				mBinding.top.setText(title);
				mBinding.center.setTextColor(Color.WHITE);
				if (t.eReceiptQRCode != null) {
					String eReceiptData = t.eReceiptData;
					if (eReceiptData != null && eReceiptData.startsWith("{"))
						mBinding.eReceipt.setText("qr ok");
					else if (eReceiptData != null) {
						mBinding.eReceipt.setText("qr err " + eReceiptData);
					} else {
						mBinding.eReceipt.setText("qr");
					}
					mBinding.eReceipt.setVisibility(View.VISIBLE);
				} else
					mBinding.eReceipt.setVisibility(View.GONE);
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

			if (fromAmount > 0) {
				mBinding.icon.setImageDrawable(icBlotterIncome);
				mBinding.icon.setColorFilter(u.positiveColor);
			} else if (fromAmount < 0) {
				mBinding.icon.setImageDrawable(icBlotterExpense);
				mBinding.icon.setColorFilter(u.negativeColor);
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

			if (mBinding.right != null && !MyPreferences.isShowRunningBalance(context)) {
				mBinding.right.setVisibility(View.GONE);
			}

//			TransactionStatus status = TransactionStatus.valueOf(cursor.getString(BlotterColumns.status.ordinal()));
//			mBinding.indicator.setBackgroundColor(colors[status.ordinal()]);
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
