package ru.orangesoftware.financisto.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.activity.BlotterFilterActivity;
import ru.orangesoftware.financisto.activity.BlotterOperations;
import ru.orangesoftware.financisto.activity.BlotterTotalsDetailsActivity;
import ru.orangesoftware.financisto.activity.FilterState;
import ru.orangesoftware.financisto.activity.IntegrityCheckTask;
import ru.orangesoftware.financisto.activity.MonthlyViewActivity;
import ru.orangesoftware.financisto.activity.MyQuickAction;
import ru.orangesoftware.financisto.activity.SelectTemplateActivity;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
import ru.orangesoftware.financisto.adapter.BlotterRecyclerAdapter;
import ru.orangesoftware.financisto.adapter.TransactionsRecyclerAdapter;
import ru.orangesoftware.financisto.blotter.AccountTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.BlotterTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.databinding.BlotterBinding;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.dialog.AbstractDialogFragment;
import ru.orangesoftware.financisto.dialog.MassOperationsDialog;
import ru.orangesoftware.financisto.dialog.TransactionInfoDialog;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSelection;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.IntegrityCheckRunningBalance;
import ru.orangesoftware.financisto.utils.LocalizableEnum;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.view.NodeInflater;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.utils.MyPreferences.isQuickMenuEnabledForTransaction;

public class BlotterFragment extends AbstractRecycleFragment implements ItemClick, ItemSelection, ItemSwipeable {

    public static final String SAVE_FILTER = "saveFilter";
    public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";

    private static final int NEW_TRANSACTION_REQUEST = 1;
    private static final int NEW_TRANSFER_REQUEST = 3;
    private static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5;
    private static final int MONTHLY_VIEW_REQUEST = 6;
    private static final int BILL_PREVIEW_REQUEST = 7;
    private static final int FILTER_REQUEST = 6;

    public static final int RESULT_CREATE_ANOTHER_TRANSACTION = 100;
    public static final int RESULT_CREATE_ANOTHER_TRANSFER = 101;
    public static final int MASS_OPERATION = 1001;
    public static final int MASS_OPERATION_CLEAR = 1002;
    public static final int MASS_OPERATION_DELETE = 1003;
    public static final int MASS_OPERATION_RECONCILE = 1004;
    public static final int MASS_OPERATION_PENDING = 1005;

    protected TextView totalText;
    protected ImageButton bAdd;
    protected ImageButton bFilter;
    protected ImageButton bTransfer;
    protected ImageButton bTemplate;
    protected ImageButton bSearch;
    protected ImageButton bMenu;

    protected QuickActionGrid transactionActionGrid;
    protected QuickActionGrid addButtonActionGrid;

    private TotalCalculationTask calculationTask;

    protected boolean saveFilter = true;
    protected WhereFilter blotterFilter = WhereFilter.empty();

    protected boolean isAccountBlotter = false;
    protected boolean showAllBlotterButtons = true;

    private long selectedId = -1;

    private View defaultBottomBar;
    private View selectionBottomBar;
    private TextView selectionCountText;
    private int shortAnimationDuration;

    public BlotterFragment() {
        super(R.layout.blotter);
    }

    @SuppressLint("ValidFragment")
    public BlotterFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        integrityCheck();

        BlotterBinding binding = (BlotterBinding) getBinding();
        defaultBottomBar = binding.defaultBottomBar;
        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        selectionMode = SelectionMode.ON;
        selectionBottomBar = binding.selectionBottomBar;
        selectionBottomBar.findViewById(R.id.bCheckAll).setOnClickListener(arg0 -> checkAll());
        selectionBottomBar.findViewById(R.id.bUncheckAll).setOnClickListener(arg0 -> uncheckAll());
        selectionBottomBar.findViewById(R.id.bSelectionAction).setOnClickListener(arg0 -> showSelectionActionDialog());

        selectionCountText = binding.selectionCount;

        bAdd = binding.bAdd;
        bAdd.setOnClickListener(v -> {
            if (showAllBlotterButtons) {
                addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class, null);
            } else {
                addButtonActionGrid.show(bAdd);
            }
        });

        bFilter = binding.bFilter;
        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(context, BlotterFilterActivity.class);
            blotterFilter.toIntent(intent);
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.getAccountId() > 0);
            startActivityForResult(intent, FILTER_REQUEST);
        });

        totalText = view.findViewById(R.id.total);
        totalText.setOnClickListener(v -> showTotals());

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            blotterFilter = WhereFilter.fromIntent(intent);
            saveFilter = intent.getBooleanExtra(SAVE_FILTER, true);
            isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
        }
        if (savedInstanceState != null) {
            blotterFilter = WhereFilter.fromBundle(savedInstanceState);
        }
        if (saveFilter && blotterFilter.isEmpty()) {
            blotterFilter = WhereFilter.fromSharedPreferences(getActivity().getPreferences(0));
        }

        showAllBlotterButtons = !isAccountBlotter && !MyPreferences.isCollapseBlotterButtons(context);

        if (showAllBlotterButtons) {
            bTransfer = binding.bTransfer;
            bTransfer.setVisibility(View.VISIBLE);
            bTransfer.setOnClickListener(arg0 -> addItem(NEW_TRANSFER_REQUEST, TransferActivity.class, null));

            bTemplate = binding.bTemplate;
            bTemplate.setVisibility(View.VISIBLE);
            bTemplate.setOnClickListener(v -> createFromTemplate());
        }

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
                    blotterFilter.remove(BlotterFilter.NOTE);

                    if (!text.isEmpty()) {
                        blotterFilter.contains(BlotterFilter.NOTE, text);
                        clearButton.setVisibility(View.VISIBLE);
                    } else {
                        clearButton.setVisibility(View.GONE);
                    }

                    recreateCursor();
                    applyFilter();
                    saveFilter();
                }
            });

            if (blotterFilter.get(BlotterFilter.NOTE) != null) {
                String searchFilterText = blotterFilter.get(BlotterFilter.NOTE).getStringValue();
                if (!searchFilterText.isEmpty()) {
                    searchFilterText = searchFilterText.substring(1, searchFilterText.length() - 1);
                    searchText.setText(searchFilterText);
                }
            }
        });

        applyFilter();
        applyPopupMenu();
        calculateTotals();
        prepareTransactionActionGrid();
        prepareAddButtonActionGrid();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILTER_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                blotterFilter.clear();
            } else if (resultCode == RESULT_OK) {
                blotterFilter = WhereFilter.fromIntent(data);
            }
            if (saveFilter) {
                saveFilter();
            }
            applyFilter();
            recreateCursor();
        } else if (resultCode == RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
        }
        if (requestCode == NEW_TRANSACTION_REQUEST && resultCode == RESULT_CREATE_ANOTHER_TRANSACTION) {
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class, data);
        }
        if (requestCode == NEW_TRANSFER_REQUEST && resultCode == RESULT_CREATE_ANOTHER_TRANSFER) {
            addItem(NEW_TRANSFER_REQUEST, TransferActivity.class, data);
        }
        if (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER) {
            recreateCursor();
        }
        if (requestCode == MASS_OPERATION) {
            switch (resultCode) {
                case MASS_OPERATION_CLEAR:
                    applyMassOp(BlotterFragment.MassOp.CLEAR);
                    break;
                case MASS_OPERATION_DELETE:
                    applyMassOp(BlotterFragment.MassOp.DELETE);
                    break;
                case MASS_OPERATION_PENDING:
                    applyMassOp(BlotterFragment.MassOp.PENDING);
                    break;
                case MASS_OPERATION_RECONCILE:
                    applyMassOp(BlotterFragment.MassOp.RECONCILE);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (blotterFilter != null)
            blotterFilter.toBundle(outState);
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null) {
            if (isAccountBlotter) {
                setListAdapter(new TransactionsRecyclerAdapter(context, db, getCursor()));
            } else {
                setListAdapter(new BlotterRecyclerAdapter(context, db, getCursor()));
            }
        } else {
            ((BaseCursorRecyclerAdapter<RecyclerView.ViewHolder>) getListAdapter()).swapCursor(getCursor());
        }
    }

    @Override
    protected Cursor createCursor() {
        if (isAccountBlotter) {
            return db.getBlotterForAccount(blotterFilter);
        } else {
            return db.getBlotter(blotterFilter);
        }
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    @Override
    public void onItemClick(View view, int position) {
        if (selectionMode == SelectionMode.OFF || selectionMode == SelectionMode.ON) {
            long id = getListAdapter().getItemId(position);
            if (isQuickMenuEnabledForTransaction(context)) {
                selectedId = id;
                transactionActionGrid.show(view);
            } else {
                showTransactionInfo(id);
            }
        }
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
                deleteTransaction(id);
                break;
            case R.id.edit_task:
                editTransaction(id);
                break;
        }
    }

    @Override
    public void toggleSelectionMode(boolean hide) {
        if (hide) {
            crossfade(defaultBottomBar, selectionBottomBar);
        } else {
            crossfade(selectionBottomBar, defaultBottomBar);
        }
    }

    @Override
    public void updateCount(int count) {
        selectionCountText.setText(String.valueOf(count));
    }

    private void showSelectionActionDialog() {
        AbstractDialogFragment massOperationsDialog = new MassOperationsDialog();
        massOperationsDialog.setTargetFragment(this, 1001);
        massOperationsDialog.show(getActivity().getSupportFragmentManager(), "MultiSelectActionsDialog");
    }

    private void crossfade(View in, View out) {
        in.setAlpha(0f);
        in.setVisibility(View.VISIBLE);

        in.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        out.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        out.setVisibility(View.GONE);
                    }
                });
    }

    protected void calculateTotals() {
        if (calculationTask != null) {
            calculationTask.stop();
            calculationTask.cancel(true);
        }
        calculationTask = createTotalCalculationTask();
        calculationTask.execute();
    }

    protected TotalCalculationTask createTotalCalculationTask() {
        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
        if (filter.getAccountId() > 0) {
            return new AccountTotalCalculationTask(context, db, filter, totalText);
        } else {
            return new BlotterTotalCalculationTask(context, db, filter, totalText);
        }
    }

    private void applyPopupMenu() {
        bMenu = getView().findViewById(R.id.bMenu);
        if (isAccountBlotter) {
            bMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, bMenu);
                long accountId = blotterFilter.getAccountId();
                if (accountId != -1) {
                    // get account type
                    Account account = db.getAccount(accountId);
                    AccountType type = AccountType.valueOf(account.type);
                    if (type.isCreditCard) {
                        // Show menu for Credit Cards - bill
                        MenuInflater inflater = getActivity().getMenuInflater();
                        inflater.inflate(R.menu.ccard_blotter_menu, popupMenu.getMenu());
                    } else {
                        // Show menu for other accounts - monthly view
                        MenuInflater inflater = getActivity().getMenuInflater();
                        inflater.inflate(R.menu.blotter_menu, popupMenu.getMenu());
                    }
                    popupMenu.setOnMenuItemClickListener(item -> {
                        onPopupMenuSelected(item.getItemId());
                        return true;
                    });
                    popupMenu.show();
                }
            });
        } else {
            bMenu.setVisibility(View.GONE);
        }
    }

    public void onPopupMenuSelected(int id) {

        long accountId = blotterFilter.getAccountId();

        Intent intent = new Intent(context, MonthlyViewActivity.class);
        intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);

        switch (id) {

            case R.id.opt_menu_month:
                // call credit card bill activity sending account id
                intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false);
                startActivityForResult(intent, MONTHLY_VIEW_REQUEST);
                break;

            case R.id.opt_menu_bill:
                if (accountId != -1) {
                    Account account = db.getAccount(accountId);

                    // call credit card bill activity sending account id
                    if (account.paymentDay > 0 && account.closingDay > 0) {
                        intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true);
                        startActivityForResult(intent, BILL_PREVIEW_REQUEST);
                    } else {
                        // display message: need payment and closing day
                        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(context);
                        dlgAlert.setMessage(R.string.statement_error);
                        dlgAlert.setTitle(R.string.ccard_statement);
                        dlgAlert.setPositiveButton(R.string.ok, null);
                        dlgAlert.setCancelable(true);
                        dlgAlert.create().show();
                    }
                }
        }
    }

    private void showTotals() {
        Intent intent = new Intent(context, BlotterTotalsDetailsActivity.class);
        blotterFilter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    protected void prepareTransactionActionGrid() {
        transactionActionGrid = new QuickActionGrid(context);
        transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_info, R.string.info));
        transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_edit, R.string.edit));
        transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_trash, R.string.delete));
        transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_copy, R.string.duplicate));
        transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_tick, R.string.clear));
        transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_double_tick, R.string.reconcile));
        if (!(blotterFilter.isTemplate() || blotterFilter.isSchedule())) {
            transactionActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_add, R.string.save_as_template));
        }
        transactionActionGrid.setOnQuickActionClickListener(transactionActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener transactionActionListener = (widget, position) -> {
        switch (position) {
            case 0:
                showTransactionInfo(selectedId);
                break;
            case 1:
                editTransaction(selectedId);
                break;
            case 2:
                deleteTransaction(selectedId);
                break;
            case 3:
                duplicateTransaction(selectedId, 1);
                break;
            case 4:
                clearTransaction(selectedId);
                break;
            case 5:
                reconcileTransaction(selectedId);
                break;
            case 6:
                new BlotterOperations(BlotterFragment.this, db, selectedId).duplicateAsTemplate();
                Toast.makeText(context, R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
                break;
        }
    };

    private void prepareAddButtonActionGrid() {
        addButtonActionGrid = new QuickActionGrid(context);
        addButtonActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.actionbar_add_big, R.string.transaction));
        addButtonActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.ic_action_transfer, R.string.transfer));
        if (addTemplateToAddButton()) {
            addButtonActionGrid.addQuickAction(new MyQuickAction(context, R.drawable.actionbar_tiles_large, R.string.template));
        } else {
            addButtonActionGrid.setNumColumns(2);
        }
        addButtonActionGrid.setOnQuickActionClickListener(addButtonActionListener);
    }

    protected boolean addTemplateToAddButton() {
        return true;
    }

    private QuickActionWidget.OnQuickActionClickListener addButtonActionListener = (widget, position) -> {
        switch (position) {
            case 0:
                addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class, null);
                break;
            case 1:
                addItem(NEW_TRANSFER_REQUEST, TransferActivity.class, null);
                break;
            case 2:
                createFromTemplate();
                break;
        }
    };

    private void clearTransaction(long selectedId) {
        new BlotterOperations(this, db, selectedId).clearTransaction();
        recreateCursor();
    }

    private void reconcileTransaction(long selectedId) {
        new BlotterOperations(this, db, selectedId).reconcileTransaction();
        recreateCursor();
    }

    protected void createFromTemplate() {
        Intent intent = new Intent(context, SelectTemplateActivity.class);
        startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
    }

    private long duplicateTransaction(long id, int multiplier) {
        long newId = new BlotterOperations(this, db, id).duplicateTransaction(multiplier);
        String toastText;
        if (multiplier > 1) {
            toastText = getString(R.string.duplicate_success_with_multiplier, multiplier);
        } else {
            toastText = getString(R.string.duplicate_success);
        }
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();
        recreateCursor();
        AccountWidget.updateWidgets(context);
        return newId;
    }

    private void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz, Intent data) {
        Intent intent = new Intent(context, clazz);
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        }
        if (data != null) {
            intent.putExtras(data);
        }
        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        startActivityForResult(intent, requestId);
    }

    private void deleteTransaction(long id) {
        new BlotterOperations(this, db, id).deleteTransaction();
    }

    public void afterDeletingTransaction(long id) {
        recreateCursor();
        AccountWidget.updateWidgets(context);
    }

    private void editTransaction(long id) {
        new BlotterOperations(this, db, id).editTransaction();
    }

    private void applyMassOp(final MassOp op) {
        BlotterRecyclerAdapter blotterAdapter = (BlotterRecyclerAdapter) getListAdapter();
        int count = blotterAdapter.getCheckedCount();
        if (count > 0) {
            new AlertDialog.Builder(context)
                    .setMessage(getString(R.string.apply_mass_op, getString(op.getTitleId()), count))
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                        long[] ids = blotterAdapter.getAllCheckedIds();
                        Log.d("Financisto", "Will apply " + op + " on " + Arrays.toString(ids));
                        op.apply(db, ids);
                        uncheckAll();
                        recreateCursor();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            Toast.makeText(context, R.string.apply_mass_op_zero_count, Toast.LENGTH_SHORT).show();
        }
    }

    private void createTransactionFromTemplate(Intent data) {
        long templateId = data.getLongExtra(SelectTemplateFragment.TEMPATE_ID, -1);
        int multiplier = data.getIntExtra(SelectTemplateFragment.MULTIPLIER, 1);
        boolean edit = data.getBooleanExtra(SelectTemplateFragment.EDIT_AFTER_CREATION, false);
        if (templateId > 0) {
            long id = duplicateTransaction(templateId, multiplier);
            Transaction t = db.getTransaction(id);
            if (t.fromAmount == 0 || edit) {
                new BlotterOperations(this, db, id).asNewFromTemplate().editTransaction();
            }
        }
    }

    private void saveFilter() {
        SharedPreferences preferences = getActivity().getPreferences(0);
        blotterFilter.toSharedPreferences(preferences);
    }

    protected void applyFilter() {
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            Account a = db.getAccount(accountId);
            bAdd.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
            if (showAllBlotterButtons) {
                bTransfer.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
            }
        }
        String title = blotterFilter.getTitle();
        if (title != null) {
            getActivity().setTitle(getString(R.string.blotter) + " : " + title);
        }
        updateFilterImage();
    }

    protected void updateFilterImage() {
        FilterState.updateFilterColor(context, blotterFilter, bFilter);
    }

    private void showTransactionInfo(long id) {
        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(getActivity(), db, new NodeInflater(getLayoutInflater()));
        transactionInfoView.show(this, id);
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(getActivity()).execute(new IntegrityCheckRunningBalance(context, db));
    }

    public enum MassOp implements LocalizableEnum {
        CLEAR(R.string.mass_operations_clear_all) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.clearSelectedTransactions(ids);
            }
        },
        RECONCILE(R.string.mass_operations_reconcile) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.reconcileSelectedTransactions(ids);
            }
        },
        PENDING(R.string.transaction_status_pending) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                db.setPendingSelectedTransactions(ids);
            }
        },
        DELETE(R.string.mass_operations_delete) {
            @Override
            public void apply(DatabaseAdapter db, long[] ids) {
                List<Account> accounts = db.getAllAccountsForTransactionsIDs(ids);
                db.deleteSelectedTransactions(ids);
                for (Account account : accounts) {
                    db.rebuildRunningBalanceForAccount(account);
                }
            }
        };

        private final int titleId;

        MassOp(int titleId) {
            this.titleId = titleId;
        }

        public abstract void apply(DatabaseAdapter db, long[] ids);

        @Override
        public int getTitleId() {
            return titleId;
        }
    }
}
