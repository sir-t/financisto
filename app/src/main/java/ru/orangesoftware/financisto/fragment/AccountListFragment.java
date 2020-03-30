package ru.orangesoftware.financisto.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountActivity;
import ru.orangesoftware.financisto.activity.AccountListTotalsDetailsActivity;
import ru.orangesoftware.financisto.activity.BlotterFilterActivity;
import ru.orangesoftware.financisto.activity.GenericBlotterActivity;
import ru.orangesoftware.financisto.activity.IntegrityCheckTask;
import ru.orangesoftware.financisto.activity.PurgeAccountActivity;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
import ru.orangesoftware.financisto.adapter.BottomListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.databinding.AccountListBinding;
import ru.orangesoftware.financisto.databinding.AccountListItemBinding;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.dialog.AccountInfoDialog;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Action;
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.model.ElectronicPaymentType;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.IntegrityCheckAutobackup;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;
import ru.orangesoftware.orb.EntityManager;

import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemLongClick;
import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import static ru.orangesoftware.financisto.fragment.BlotterFragment.SAVE_FILTER;

public class AccountListFragment extends AbstractRecycleFragment implements ItemClick, ItemLongClick, ItemSwipeable {

    private static final int NEW_ACCOUNT_REQUEST = 1;
    private static final int EDIT_ACCOUNT_REQUEST = 2;
    private static final int VIEW_ACCOUNT_REQUEST = 3;
    private static final int PURGE_ACCOUNT_REQUEST = 4;

    private long selectedId = -1;
    private ConstraintLayout accountBottomSheet;
    private BottomSheetBehavior accountBottomSheetBehavior;
    private TextView accountTitle;
    private ListView accountListView;
    private ImageView accountInfo;
    private AccountTotalsCalculationTask totalCalculationTask;

    public AccountListFragment(){
        super(R.layout.account_list);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getRecyclerView().addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        getRecyclerView().setItemAnimator(new DefaultItemAnimator());

        AccountListBinding binding = (AccountListBinding) getBinding();
        binding.integrityError.setOnClickListener(v -> v.setVisibility(View.GONE));
        binding.bAdd.setOnClickListener(v -> {
            Intent intent = new Intent(context, AccountActivity.class);
            startActivityForResult(intent, NEW_ACCOUNT_REQUEST);
        });

        accountBottomSheet = view.findViewById(R.id.account_bottom_sheet);
        accountBottomSheetBehavior = BottomSheetBehavior.from(accountBottomSheet);
        accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        accountTitle = new TextView(context);
        accountListView = new ListView(context);
        accountInfo = new ImageView(context);

        calculateTotals();
        integrityCheck();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIEW_ACCOUNT_REQUEST || requestCode == PURGE_ACCOUNT_REQUEST) {
            recreateCursor();
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (accountBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        selectedId = getListAdapter().getItemId(position);
        if (MyPreferences.isQuickMenuEnabledForAccount(getContext())){
            setAccountActionGrid();
            accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            showAccountTransactions(selectedId);
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        if (accountBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        selectedId = getListAdapter().getItemId(position);
        setAccountActionGrid();
        accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public Integer[] getSwipeOptions() {
        return new Integer[]{R.id.delete_task, R.id.edit_task};
    }

    @Override
    public void onSwipeClick(int viewID, int position) {
        long id = getListAdapter().getItemId(position);
        switch (viewID) {
            case R.id.delete_task:
                new AlertDialog.Builder(context)
                        .setMessage(R.string.delete_account_confirm)
                        .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                            db.deleteAccount(id);
                            recreateCursor();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                break;
            case R.id.edit_task:
                editAccount(id);
                break;
        }
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(getActivity()).execute(new IntegrityCheckAutobackup(context, TimeUnit.DAYS.toMillis(7)));
    }

    private void setAccountActionGrid() {
        Account a = db.getAccount(selectedId);
        View v = getView();

        accountBottomSheet.removeAllViews();
        accountBottomSheet.addView(accountTitle);
        accountBottomSheet.addView(accountListView);
        accountBottomSheet.addView(accountInfo);

        accountTitle.setTextColor(Color.BLACK);
        accountTitle.setText(a.title);
        accountTitle.setId(R.id.account_title);
        accountTitle.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        accountInfo.setId(R.id.account_info);
        accountInfo.setLayoutParams(new ConstraintLayout.LayoutParams(60,60));
        accountInfo.setImageResource(R.drawable.ic_action_info);
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.activatedBackgroundIndicator, outValue, true);
        accountInfo.setBackgroundResource(outValue.resourceId);

        accountListView.setId(R.id.account_list_view);
        accountListView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        accountListView.setPadding(0,20,0,0);

        ConstraintSet accountBottomSheetSet = new ConstraintSet();
        accountBottomSheetSet.clone(accountBottomSheet);
        accountBottomSheetSet.connect(accountTitle.getId(),ConstraintSet.START,accountBottomSheet.getId(),ConstraintSet.START);
        accountBottomSheetSet.connect(accountTitle.getId(),ConstraintSet.TOP,accountBottomSheet.getId(),ConstraintSet.TOP);
        accountBottomSheetSet.connect(accountInfo.getId(),ConstraintSet.END,accountBottomSheet.getId(),ConstraintSet.END);
        accountBottomSheetSet.connect(accountInfo.getId(),ConstraintSet.TOP,accountBottomSheet.getId(),ConstraintSet.TOP);
        accountBottomSheetSet.connect(accountListView.getId(),ConstraintSet.TOP,accountTitle.getId(),ConstraintSet.BOTTOM);
        accountBottomSheetSet.applyTo(accountBottomSheet);

        ArrayList<Action> accountActionsLst = new ArrayList<>();
        accountActionsLst.add(new Action(R.string.transaction,R.drawable.ic_action_add_small));
        accountActionsLst.add(new Action(R.string.transfer,R.drawable.ic_action_transfer));
        accountActionsLst.add(new Action(R.string.blotter,R.drawable.ic_action_list));
        accountActionsLst.add(new Action(R.string.edit,R.drawable.ic_action_edit));
        accountActionsLst.add(new Action(R.string.more_actions,R.drawable.ic_action_download));
        accountActionsLst.add(new Action(R.string.balance,R.drawable.ic_action_tick));
        accountActionsLst.add(new Action(R.string.delete_old_transactions,R.drawable.ic_action_flash));
        if (a.isActive) {
            accountActionsLst.add(new Action(R.string.close_account,R.drawable.ic_action_lock_closed));
        } else {
            accountActionsLst.add(new Action(R.string.reopen_account,R.drawable.ic_action_lock_open));
        }
        accountActionsLst.add(new Action(R.string.delete_account,R.drawable.ic_action_trash));

        Action[] accountActions = new Action[accountActionsLst.size()];
        accountActionsLst.toArray(accountActions);
        BottomListAdapter accountActionAdapter = new BottomListAdapter(getContext(), accountActions);
        accountListView.setAdapter(accountActionAdapter);

        accountListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        addTransaction(selectedId, TransactionActivity.class);
                        break;
                    case 1:
                        addTransaction(selectedId, TransferActivity.class);
                        break;
                    case 2:
                        showAccountTransactions(selectedId);
                        break;
                    case 3:
                        editAccount(selectedId);
                        break;
                    case 4:
                        accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        break;
                    case 5:
                        updateAccountBalance(selectedId);
                        break;
                    case 6:
                        purgeAccount();
                        break;
                    case 7:
                        closeOrOpenAccount();
                        break;
                    case 8:
                        deleteAccount();
                        break;
                }
                if (position != 4) {
                    accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });

        accountInfo.setOnClickListener(accountInfoListener);
    }

    private View.OnClickListener accountInfoListener = new View.OnClickListener() {
        public void onClick(View v) {
            showAccountInfo(selectedId);
            accountBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    };

    private boolean updateAccountBalance(long id) {
        Account a = db.getAccount(id);
        if (a != null) {
            Intent intent = new Intent(context, TransactionActivity.class);
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id);
            intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
            startActivityForResult(intent, 0);
            return true;
        }
        return false;
    }

    private void showAccountTransactions(long id) {
        Account account = db.getAccount(id);
        if (account != null) {
            Intent intent = new Intent(context, GenericBlotterActivity.class);
            Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(id))
                    .toIntent(account.title, intent);
            intent.putExtra(SAVE_FILTER, false);
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, true);
            startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
        }
    }

    private void showAccountInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);
        AccountInfoDialog accountInfoDialog = new AccountInfoDialog(getActivity(), id, db, inflater);
        accountInfoDialog.show();
    }

    private void editAccount(long id) {
        Intent intent = new Intent(context, AccountActivity.class);
        intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_ACCOUNT_REQUEST);
    }

    private void addTransaction(long accountId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(context, clazz);
        intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
    }

    private void purgeAccount() {
        Intent intent = new Intent(context, PurgeAccountActivity.class);
        intent.putExtra(PurgeAccountActivity.ACCOUNT_ID, selectedId);
        startActivityForResult(intent, PURGE_ACCOUNT_REQUEST);
    }

    private void closeOrOpenAccount() {
        Account a = db.getAccount(selectedId);
        if (a.isActive) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.close_account_confirm)
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> flipAccountActive(a))
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            flipAccountActive(a);
        }
    }

    private void deleteAccount() {
        new AlertDialog.Builder(context)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                    db.deleteAccount(selectedId);
                    recreateCursor();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void flipAccountActive(Account a) {
        a.isActive = !a.isActive;
        db.saveAccount(a);
        recreateCursor();
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null) {
            setListAdapter(new AccountRecyclerAdapter(context, getCursor()));
        } else {
            ((AccountRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
        }
    }

    @Override
    protected Cursor createCursor() {
        if (MyPreferences.isHideClosedAccounts(context)) {
            return db.getAllActiveAccounts();
        } else {
            return db.getAllAccounts();
        }
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    private void calculateTotals() {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        TextView totalText = getView().findViewById(R.id.total);
        totalText.setOnClickListener(view -> showTotals());
        totalCalculationTask = new AccountTotalsCalculationTask(context, db, totalText);
        totalCalculationTask.execute();
    }

    public static class AccountTotalsCalculationTask extends TotalCalculationTask {

        private final DatabaseAdapter db;

        AccountTotalsCalculationTask(Context context, DatabaseAdapter db, TextView totalText) {
            super(context, totalText);
            this.db = db;
        }

        @Override
        public Total getTotalInHomeCurrency() {
            return db.getAccountsTotalInHomeCurrency();
        }

        @Override
        public Total[] getTotals() {
            return new Total[0];
        }

    }

    private void showTotals() {
        Intent intent = new Intent(context, AccountListTotalsDetailsActivity.class);
        startActivityForResult(intent, -1);
    }

    private static class AccountRecyclerItemHolder extends RecyclerView.ViewHolder {

        private final AccountListItemBinding mBinding;

        AccountRecyclerItemHolder(AccountListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(Account a, Context context, Utils u, DateFormat df, boolean isShowAccountLastTransactionDate) {
            mBinding.center.setText(a.title);

            AccountType type = AccountType.valueOf(a.type);
            if (type.isCard && a.cardIssuer != null) {
                CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
                mBinding.icon.setImageResource(cardIssuer.iconId);
            } else if (type.isElectronic && a.cardIssuer != null) {
                ElectronicPaymentType paymentType = ElectronicPaymentType.valueOf(a.cardIssuer);
                mBinding.icon.setImageResource(paymentType.iconId);
            } else {
                mBinding.icon.setImageResource(type.iconId);
            }
            if (a.isActive) {
                mBinding.icon.getDrawable().mutate().setAlpha(0xFF);
                mBinding.activeIcon.setVisibility(View.INVISIBLE);
            } else {
                mBinding.icon.getDrawable().mutate().setAlpha(0x77);
                mBinding.activeIcon.setVisibility(View.VISIBLE);
            }

            StringBuilder sb = new StringBuilder();
            if (!Utils.isEmpty(a.issuer)) {
                sb.append(a.issuer);
            }
            if (!Utils.isEmpty(a.number)) {
                sb.append(" #").append(a.number);
            }
            if (sb.length() == 0) {
                sb.append(context.getString(type.titleId));
            }
            mBinding.top.setText(sb.toString());

            long date = a.creationDate;
            if (isShowAccountLastTransactionDate && a.lastTransactionDate > 0) {
                date = a.lastTransactionDate;
            }
            mBinding.bottom.setText(df.format(new Date(date)));

            long amount = a.totalAmount;
            if (type == AccountType.CREDIT_CARD && a.limitAmount != 0) {
                long limitAmount = Math.abs(a.limitAmount);
                long balance = limitAmount + amount;
                long balancePercentage = 10000 * balance / limitAmount;
                u.setAmountText(mBinding.right, a.currency, amount, false);
                u.setAmountText(mBinding.rightCenter, a.currency, balance, false);
                mBinding.right.setVisibility(View.VISIBLE);
                mBinding.progress.setMax(10000);
                mBinding.progress.setProgress((int) balancePercentage);
                mBinding.progress.setVisibility(View.VISIBLE);
            } else {
                u.setAmountText(mBinding.rightCenter, a.currency, amount, false);
                mBinding.right.setVisibility(View.GONE);
                mBinding.progress.setVisibility(View.GONE);
            }
        }

    }

    public class AccountRecyclerAdapter extends BaseCursorRecyclerAdapter<AccountRecyclerItemHolder> {

        private final Context context;
        private final Utils u;
        private DateFormat df;
        private boolean isShowAccountLastTransactionDate;

        AccountRecyclerAdapter(Context context, Cursor c) {
            super(c);
            this.context = context;
            this.u = new Utils(context);
            this.df = DateUtils.getShortDateFormat(context);
            this.isShowAccountLastTransactionDate = MyPreferences.isShowAccountLastTransactionDate(context);
        }

        @NonNull
        @Override
        public AccountRecyclerItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            AccountListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.account_list_item, parent, false);
            return new AccountRecyclerItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(AccountRecyclerItemHolder holder, Cursor cursor) {
            Account a = EntityManager.loadFromCursor(cursor, Account.class);
            holder.bind(a, context, u, df, isShowAccountLastTransactionDate);
        }
    }

}