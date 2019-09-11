package ru.orangesoftware.financisto.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountActivity;
import ru.orangesoftware.financisto.activity.AccountListTotalsDetailsActivity;
import ru.orangesoftware.financisto.activity.BlotterFilterActivity;
import ru.orangesoftware.financisto.activity.GenericBlotterActivity;
import ru.orangesoftware.financisto.activity.MenuListItem;
import ru.orangesoftware.financisto.activity.MyQuickAction;
import ru.orangesoftware.financisto.activity.PurgeAccountActivity;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
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
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.model.ElectronicPaymentType;
import ru.orangesoftware.financisto.model.Total;
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

    private QuickActionWidget accountActionGrid;
    private long selectedId = -1;

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

        final ImageButton bMenu = binding.bMenu;
        if (MyPreferences.isShowMenuButtonOnAccountsScreen(context)) {
            bMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, bMenu);
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.account_list_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    handlePopupMenu(item.getItemId());
                    return true;
                });
                popupMenu.show();
            });
        } else {
            bMenu.setVisibility(View.GONE);
        }

        calculateTotals();
//        integrityCheck();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        long id = getListAdapter().getItemId(position);
        showAccountTransactions(id);
    }

    @Override
    public void onItemLongClick(View view, int position) {
        selectedId = getListAdapter().getItemId(position);
        prepareAccountActionGrid();
        accountActionGrid.show(view);
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

//    @Override
//    public void integrityCheck() {
//        new IntegrityCheckTask(context).execute(new IntegrityCheckAutobackup(context, TimeUnit.DAYS.toMillis(7)));
//    }

    private void handlePopupMenu(int id) {
        switch (id) {
            case R.id.backup:
                MenuListItem.MENU_BACKUP.call(getActivity());
                break;
        }
    }

    private void prepareAccountActionGrid() {
        Account a = db.getAccount(selectedId);
        accountActionGrid = new QuickActionGrid(context);
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_info, R.string.info));
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_list, R.string.blotter));
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_edit, R.string.edit));
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_add, R.string.transaction));
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_transfer, R.string.transfer));
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_tick, R.string.balance));
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_flash, R.string.delete_old_transactions));
        if (a.isActive) {
            accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_lock_closed, R.string.close_account));
        } else {
            accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_lock_open, R.string.reopen_account));
        }
        accountActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_trash, R.string.delete_account));
        accountActionGrid.setOnQuickActionClickListener(accountActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener accountActionListener = (widget, position) -> {
        switch (position) {
            case 0:
                showAccountInfo(selectedId);
                break;
            case 1:
                showAccountTransactions(selectedId);
                break;
            case 2:
                editAccount(selectedId);
                break;
            case 3:
                addTransaction(selectedId, TransactionActivity.class);
                break;
            case 4:
                addTransaction(selectedId, TransferActivity.class);
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
    protected void recreateCursor() {
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
