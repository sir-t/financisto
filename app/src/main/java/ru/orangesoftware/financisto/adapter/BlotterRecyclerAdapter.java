/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.databinding.BlotterListItemBinding;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.model.CategoryEntity;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.recur.Recurrence;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.StringUtil;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.model.Category.isSplit;
import static ru.orangesoftware.financisto.utils.TransactionTitleUtils.generateTransactionTitle;

public class BlotterRecyclerAdapter extends BaseCursorRecyclerAdapter<BlotterRecyclerAdapter.BlotterListItemHolder> {

    private final Date dt = new Date();

    protected final StringBuilder sb = new StringBuilder();
    protected final Drawable icBlotterIncome;
    protected final Drawable icBlotterExpense;
    protected final Drawable icBlotterTransfer;
    protected final Drawable icBlotterSplit;
    protected final Utils u;
    protected final DatabaseAdapter db;

    private final int colors[];
    private final boolean showRunningBalance;
    private final Context context;

    private final Set<Long> checkedItems = new HashSet<>();

    public BlotterRecyclerAdapter(Context context, DatabaseAdapter db, Cursor c) {
        super(c);
        this.context = context;
        this.icBlotterIncome = context.getResources().getDrawable(R.drawable.ic_action_arrow_left_bottom);
        this.icBlotterExpense = context.getResources().getDrawable(R.drawable.ic_action_arrow_right_top);
        this.icBlotterTransfer = context.getResources().getDrawable(R.drawable.ic_action_arrow_top_down);
        this.icBlotterSplit = context.getResources().getDrawable(R.drawable.ic_action_share);
        this.u = new Utils(context);
        this.colors = initializeColors(context);
        this.showRunningBalance = MyPreferences.isShowRunningBalance(context);
        this.db = db;
    }

    @NonNull
    @Override
    public BlotterListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        BlotterListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.blotter_list_item, parent, false);
        return new BlotterListItemHolder(binding);
    }

    @Override
    public void onBindViewHolder(BlotterListItemHolder holder, Cursor cursor) {
        BlotterListItemBinding v = holder.mBinding;

        final long parent = cursor.getLong(BlotterColumns.parent_id.ordinal());
        final long id = parent > 0 ? parent : cursor.getLong(BlotterColumns._id.ordinal());
        if (isUnchecked(id)) {
            v.rowFG.setBackgroundResource(R.color.material_blue_gray);
        } else {
            v.rowFG.setBackgroundResource(R.color.holo_gray_dark);
        }
        bindView(v, context, cursor);
    }

    protected void bindView(final BlotterListItemBinding v, Context context, Cursor cursor) {
        long toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal());
        int isTemplate = cursor.getInt(BlotterColumns.is_template.ordinal());
        TextView noteView = isTemplate == 1 ? v.bottom : v.center;
        if (toAccountId > 0) {
            v.top.setText(R.string.transfer);

            String fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal());
            String toAccountTitle = cursor.getString(BlotterColumns.to_account_title.ordinal());
            u.setTransferTitleText(noteView, fromAccountTitle, toAccountTitle);

            long fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
            Currency fromCurrency = CurrencyCache.getCurrency(db, fromCurrencyId);
            long toCurrencyId = cursor.getLong(BlotterColumns.to_account_currency_id.ordinal());
            Currency toCurrency = CurrencyCache.getCurrency(db, toCurrencyId);

            long fromAmount = cursor.getLong(BlotterColumns.from_amount.ordinal());
            long toAmount = cursor.getLong(BlotterColumns.to_amount.ordinal());
            long fromBalance = cursor.getLong(BlotterColumns.from_account_balance.ordinal());
            long toBalance = cursor.getLong(BlotterColumns.to_account_balance.ordinal());
            u.setTransferAmountText(v.rightCenter, fromCurrency, fromAmount, toCurrency, toAmount);
            if (v.right != null) {
                u.setTransferBalanceText(v.right, fromCurrency, fromBalance, toCurrency, toBalance);
            }
            v.icon.setImageDrawable(icBlotterTransfer);
            v.icon.setColorFilter(u.transferColor);
            v.rightQrCode.setVisibility(View.GONE);
        } else {
            String fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal());
            v.top.setText(fromAccountTitle);
            setTransactionTitleText(cursor, noteView);
            sb.setLength(0);
            long fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
            Currency fromCurrency = CurrencyCache.getCurrency(db, fromCurrencyId);
            long amount = cursor.getLong(BlotterColumns.from_amount.ordinal());
            long originalCurrencyId = cursor.getLong(BlotterColumns.original_currency_id.ordinal());
            if (originalCurrencyId > 0) {
                Currency originalCurrency = CurrencyCache.getCurrency(db, originalCurrencyId);
                long originalAmount = cursor.getLong(BlotterColumns.original_from_amount.ordinal());
                u.setAmountText(sb, v.rightCenter, originalCurrency, originalAmount, fromCurrency, amount, true);
            } else {
                u.setAmountText(sb, v.rightCenter, fromCurrency, amount, true);
            }
            long categoryId = cursor.getLong(BlotterColumns.category_id.ordinal());
            if (isSplit(categoryId)) {
                v.icon.setImageDrawable(icBlotterSplit);
                v.icon.setColorFilter(u.splitColor);
            } else if (amount == 0) {
                int categoryType = cursor.getInt(BlotterColumns.category_type.ordinal());
                if (categoryType == CategoryEntity.TYPE_INCOME) {
                    v.icon.setImageDrawable(icBlotterIncome);
                    v.icon.setColorFilter(u.positiveColor);
                } else if (categoryType == CategoryEntity.TYPE_EXPENSE) {
                    v.icon.setImageDrawable(icBlotterExpense);
                    v.icon.setColorFilter(u.negativeColor);
                }
            } else {
                if (amount > 0) {
                    v.icon.setImageDrawable(icBlotterIncome);
                    v.icon.setColorFilter(u.positiveColor);
                } else if (amount < 0) {
                    v.icon.setImageDrawable(icBlotterExpense);
                    v.icon.setColorFilter(u.negativeColor);
                }
            }
            if (v.right != null) {
                long balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal());
                v.right.setText(Utils.amountToString(fromCurrency, balance, false));
            }
            if (cursor.getString(BlotterColumns.e_receipt_qr_code.ordinal()) != null) {
                v.rightQrCode.setVisibility(View.VISIBLE);
                String eReceiptData = cursor.getString(BlotterColumns.e_receipt_data.ordinal());
                if (eReceiptData != null && eReceiptData.startsWith("{")) {
                    v.rightQrCode.setColorFilter(Color.argb(255, 0, 255, 0));
                } else if (eReceiptData != null) {
                    v.rightQrCode.setColorFilter(Color.argb(255, 255, 0, 0));
                } else {
                    v.rightQrCode.setColorFilter(Color.argb(255, 255, 255, 255));
                }
            } else {
                v.rightQrCode.setVisibility(View.GONE);
            }
        }
        setIndicatorColor(v, cursor);
        if (isTemplate == 1) {
            String templateName = cursor.getString(BlotterColumns.template_name.ordinal());
            v.center.setText(templateName);
        } else {
            String recurrence = cursor.getString(BlotterColumns.recurrence.ordinal());
            if (isTemplate == 2 && recurrence != null) {
                Recurrence r = Recurrence.parse(recurrence);
                v.bottom.setText(r.toInfoString(context));
                v.bottom.setTextColor(v.top.getTextColors().getDefaultColor());
            } else {
                long date = cursor.getLong(BlotterColumns.datetime.ordinal());
                dt.setTime(date);
                v.bottom.setText(StringUtil.capitalize(DateUtils.formatDateTime(context, dt.getTime(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY)));

                if (isTemplate == 0 && date > System.currentTimeMillis()) {
                    u.setFutureTextColor(v.bottom);
                } else {
                    v.bottom.setTextColor(v.top.getTextColors().getDefaultColor());
                }
            }
        }
        removeRightViewIfNeeded(v);
    }

    private int[] initializeColors(Context context) {
        Resources r = context.getResources();
        TransactionStatus[] statuses = TransactionStatus.values();
        int count = statuses.length;
        int[] colors = new int[count];
        for (int i = 0; i < count; i++) {
            colors[i] = r.getColor(statuses[i].colorId);
        }
        return colors;
    }

    protected boolean isShowRunningBalance() {
        return showRunningBalance;
    }

    void removeRightViewIfNeeded(BlotterListItemBinding v) {
        if (v.right != null && !isShowRunningBalance()) {
            v.right.setVisibility(View.GONE);
        }
    }

    void setIndicatorColor(BlotterListItemBinding v, Cursor cursor) {
        TransactionStatus status = TransactionStatus.valueOf(cursor.getString(BlotterColumns.status.ordinal()));
        v.indicator.setBackgroundColor(colors[status.ordinal()]);
    }

    private void setTransactionTitleText(Cursor cursor, TextView noteView) {
        sb.setLength(0);
        String payee = cursor.getString(BlotterColumns.payee.ordinal());
        String note = cursor.getString(BlotterColumns.note.ordinal());
        long locationId = cursor.getLong(BlotterColumns.location_id.ordinal());
        String location = getLocationTitle(cursor, locationId);
        long categoryId = cursor.getLong(BlotterColumns.category_id.ordinal());
        String category = getCategoryTitle(cursor, categoryId);
        String text = generateTransactionTitle(sb, payee, note, location, categoryId, category);
        noteView.setText(text);
        noteView.setTextColor(Color.WHITE);
    }

    private String getCategoryTitle(Cursor cursor, long categoryId) {
        String category = "";
        if (categoryId != 0) {
            category = cursor.getString(BlotterColumns.category_title.ordinal());
        }
        return category;
    }

    private String getLocationTitle(Cursor cursor, long locationId) {
        String location = "";
        if (locationId > 0) {
            location = cursor.getString(BlotterColumns.location.ordinal());
        }
        return location;
    }

    private void updateCheckedState(long id, boolean checked) {
        if (checked) {
            checkedItems.add(id);
        } else {
            checkedItems.remove(id);
        }
    }

    public int getCheckedCount() {
        return checkedItems.size();
    }

    private boolean isUnchecked(long id) {
        return checkedItems.contains(id);
    }

    public void toggleSelection(long id, View layout) {
        boolean checked = !isUnchecked(id);
        updateCheckedState(id, checked);
        if (checked) {
            layout.setBackgroundResource(R.color.material_blue_gray);
        } else {
            layout.setBackgroundResource(R.color.holo_gray_dark);
        }
    }

    public void checkAll() {
        List<Long> allItems = new ArrayList<>();
        Cursor cursor = getCursor();
        boolean notEmpty = cursor.moveToFirst();
        if (notEmpty) {
            do {
                allItems.add(cursor.getLong(0));
            } while (cursor.moveToNext());
        }
        checkedItems.addAll(allItems);
        notifyDataSetChanged();
    }

    public void uncheckAll() {
        checkedItems.clear();
        notifyDataSetChanged();
    }

    public static class BlotterListItemHolder extends RecyclerView.ViewHolder {

        private final BlotterListItemBinding mBinding;

        public BlotterListItemHolder(BlotterListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

    }

    public long[] getAllCheckedIds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return checkedItems.stream().mapToLong(l -> l).toArray();
        } else {
            int checkedCount = getCheckedCount();
            long[] ids = new long[checkedCount];
            int k = 0;
            for (Long id : checkedItems) {
                ids[k++] = id;
            }
            return ids;
        }
    }

}
