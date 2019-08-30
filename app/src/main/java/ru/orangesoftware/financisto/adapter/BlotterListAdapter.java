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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.orangesoftware.financisto.R;
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

public class BlotterListAdapter extends ResourceCursorAdapter {

    private final Date dt = new Date();

    protected final StringBuilder sb = new StringBuilder();
    protected final Drawable icBlotterIncome;
    protected final Drawable icBlotterExpense;
    protected final Drawable icBlotterTransfer;
    protected final Drawable icBlotterSplit;
    protected final Utils u;
    protected final DatabaseAdapter db;

    private final int colors[];

    private final Set<Long> checkedItems = new HashSet<>();

    private boolean showRunningBalance;

    public BlotterListAdapter(Context context, DatabaseAdapter db, Cursor c) {
        this(context, db, R.layout.blotter_list_item, c, false);
    }

    public BlotterListAdapter(Context context, DatabaseAdapter db, int layoutId, Cursor c) {
        this(context, db, layoutId, c, false);
    }

    public BlotterListAdapter(Context context, DatabaseAdapter db, int layoutId, Cursor c, boolean autoRequery) {
        super(context, layoutId, c, autoRequery);
        this.icBlotterIncome = context.getResources().getDrawable(R.drawable.ic_action_arrow_left_bottom);
        this.icBlotterExpense = context.getResources().getDrawable(R.drawable.ic_action_arrow_right_top);
        this.icBlotterTransfer = context.getResources().getDrawable(R.drawable.ic_action_arrow_top_down);
        this.icBlotterSplit = context.getResources().getDrawable(R.drawable.ic_action_share);
        this.u = new Utils(context);
        this.colors = initializeColors(context);
        this.showRunningBalance = MyPreferences.isShowRunningBalance(context);
        this.db = db;
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

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);
        createHolder(view);
        return view;
    }

    private void createHolder(View view) {
        BlotterViewHolder h = new BlotterViewHolder(view);
        view.setTag(h);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final BlotterViewHolder v = (BlotterViewHolder) view.getTag();
        final long parent = cursor.getLong(BlotterColumns.parent_id.ordinal());
        final long id = parent > 0 ? parent : cursor.getLong(BlotterColumns._id.ordinal());
        if (isUnchecked(id)) {
            v.layout.setBackgroundResource(R.color.material_blue_gray);
        } else {
            v.layout.setBackgroundResource(R.color.holo_gray_dark);
        }
        bindView(v, context, cursor);
    }

    protected void bindView(final BlotterViewHolder v, Context context, Cursor cursor) {
        long toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal());
        int isTemplate = cursor.getInt(BlotterColumns.is_template.ordinal());
        TextView noteView = isTemplate == 1 ? v.bottomView : v.centerView;
        if (toAccountId > 0) {
            v.topView.setText(R.string.transfer);

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
            u.setTransferAmountText(v.rightCenterView, fromCurrency, fromAmount, toCurrency, toAmount);
            if (v.rightView != null) {
                u.setTransferBalanceText(v.rightView, fromCurrency, fromBalance, toCurrency, toBalance);
            }
            v.iconView.setImageDrawable(icBlotterTransfer);
            v.iconView.setColorFilter(u.transferColor);
            v.eReceiptView.setVisibility(View.GONE);
        } else {
            String fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal());
            v.topView.setText(fromAccountTitle);
            setTransactionTitleText(cursor, noteView);
            sb.setLength(0);
            long fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
            Currency fromCurrency = CurrencyCache.getCurrency(db, fromCurrencyId);
            long amount = cursor.getLong(BlotterColumns.from_amount.ordinal());
            long originalCurrencyId = cursor.getLong(BlotterColumns.original_currency_id.ordinal());
            if (originalCurrencyId > 0) {
                Currency originalCurrency = CurrencyCache.getCurrency(db, originalCurrencyId);
                long originalAmount = cursor.getLong(BlotterColumns.original_from_amount.ordinal());
                u.setAmountText(sb, v.rightCenterView, originalCurrency, originalAmount, fromCurrency, amount, true);
            } else {
                u.setAmountText(sb, v.rightCenterView, fromCurrency, amount, true);
            }
            long categoryId = cursor.getLong(BlotterColumns.category_id.ordinal());
            if (isSplit(categoryId)) {
                v.iconView.setImageDrawable(icBlotterSplit);
                v.iconView.setColorFilter(u.splitColor);
            } else if (amount == 0) {
                int categoryType = cursor.getInt(BlotterColumns.category_type.ordinal());
                if (categoryType == CategoryEntity.TYPE_INCOME) {
                    v.iconView.setImageDrawable(icBlotterIncome);
                    v.iconView.setColorFilter(u.positiveColor);
                } else if (categoryType == CategoryEntity.TYPE_EXPENSE) {
                    v.iconView.setImageDrawable(icBlotterExpense);
                    v.iconView.setColorFilter(u.negativeColor);
                }
            } else {
                if (amount > 0) {
                    v.iconView.setImageDrawable(icBlotterIncome);
                    v.iconView.setColorFilter(u.positiveColor);
                } else if (amount < 0) {
                    v.iconView.setImageDrawable(icBlotterExpense);
                    v.iconView.setColorFilter(u.negativeColor);
                }
            }
            if (v.rightView != null) {
                long balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal());
                v.rightView.setText(Utils.amountToString(fromCurrency, balance, false));
            }
            if (cursor.getString(BlotterColumns.e_receipt_qr_code.ordinal()) != null) {
                String eReceiptData = cursor.getString(BlotterColumns.e_receipt_data.ordinal());
                if (eReceiptData != null && eReceiptData.startsWith("{"))
                    v.eReceiptView.setText("qr ok");
                else if (eReceiptData != null) {
                    v.eReceiptView.setText("qr err " + eReceiptData);
                } else {
                    v.eReceiptView.setText("qr");
                }
                v.eReceiptView.setVisibility(View.VISIBLE);
            } else
                v.eReceiptView.setVisibility(View.GONE);
        }
        setIndicatorColor(v, cursor);
        if (isTemplate == 1) {
            String templateName = cursor.getString(BlotterColumns.template_name.ordinal());
            v.centerView.setText(templateName);
        } else {
            String recurrence = cursor.getString(BlotterColumns.recurrence.ordinal());
            if (isTemplate == 2 && recurrence != null) {
                Recurrence r = Recurrence.parse(recurrence);
                v.bottomView.setText(r.toInfoString(context));
                v.bottomView.setTextColor(v.topView.getTextColors().getDefaultColor());
            } else {
                long date = cursor.getLong(BlotterColumns.datetime.ordinal());
                dt.setTime(date);
                v.bottomView.setText(StringUtil.capitalize(DateUtils.formatDateTime(context, dt.getTime(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY)));

                if (isTemplate == 0 && date > System.currentTimeMillis()) {
                    u.setFutureTextColor(v.bottomView);
                } else {
                    v.bottomView.setTextColor(v.topView.getTextColors().getDefaultColor());
                }
            }
        }
        removeRightViewIfNeeded(v);
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

    void removeRightViewIfNeeded(BlotterViewHolder v) {
        if (v.rightView != null && !isShowRunningBalance()) {
            v.rightView.setVisibility(View.GONE);
        }
    }

    void setIndicatorColor(BlotterViewHolder v, Cursor cursor) {
        TransactionStatus status = TransactionStatus.valueOf(cursor.getString(BlotterColumns.status.ordinal()));
        v.indicator.setBackgroundColor(colors[status.ordinal()]);
    }

    private boolean isUnchecked(long id) {
        return checkedItems.contains(id);
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
        notifyDataSetInvalidated();
    }

    public void uncheckAll() {
        checkedItems.clear();
        notifyDataSetInvalidated();
    }

    public static class BlotterViewHolder {

        public final RelativeLayout layout;
        public final TextView indicator;
        public final TextView topView;
        public final TextView centerView;
        public final TextView bottomView;
        public final TextView rightCenterView;
        public final TextView rightView;
        public final ImageView iconView;
        public final CheckBox checkBox;
        public final TextView eReceiptView;

        public BlotterViewHolder(View view) {
            layout = view.findViewById(R.id.layout);
            indicator = view.findViewById(R.id.indicator);
            topView = view.findViewById(R.id.top);
            centerView = view.findViewById(R.id.center);
            bottomView = view.findViewById(R.id.bottom);
            rightCenterView = view.findViewById(R.id.right_center);
            rightView = view.findViewById(R.id.right);
            iconView = view.findViewById(R.id.right_top);
            checkBox = view.findViewById(R.id.cb);
            eReceiptView = view.findViewById(R.id.e_receipt);
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
