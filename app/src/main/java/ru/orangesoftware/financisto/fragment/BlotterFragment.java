package ru.orangesoftware.financisto.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.AccountTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.BlotterTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.dialog.TransactionInfoDialog;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.IntegrityCheckRunningBalance;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.view.NodeInflater;

import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.utils.MyPreferences.isQuickMenuEnabledForTransaction;

public class BlotterFragment extends AbstractListFragment {

    public static final String SAVE_FILTER = "saveFilter";
    public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";

    private static final int NEW_TRANSACTION_REQUEST = 1;
    private static final int NEW_TRANSFER_REQUEST = 3;
    private static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5;
    private static final int MONTHLY_VIEW_REQUEST = 6;
    private static final int BILL_PREVIEW_REQUEST = 7;

    protected static final int FILTER_REQUEST = 6;
    private static final int MENU_DUPLICATE = MENU_ADD + 1;
    private static final int MENU_SAVE_AS_TEMPLATE = MENU_ADD + 2;
    public static final int RESULT_CREATE_ANOTHER_TRANSACTION = 100 ;
    public static final int RESULT_CREATE_ANOTHER_TRANSFER = 101 ;

    protected TextView totalText;
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

    public BlotterFragment() {
        super(R.layout.blotter);
    }

    @SuppressLint("ValidFragment")
    public BlotterFragment(int layout) {
        super(layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = new NodeInflater(inflater);
        View view = super.onCreateView(inflater, container, savedInstanceState);
        integrityCheck();
        return view;
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

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }


    @Override
    protected void initUI(Bundle savedInstanceState) {
        super.initUI(savedInstanceState);

        bFilter = view.findViewById(R.id.bFilter);
        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(context, BlotterFilterActivity.class);
            blotterFilter.toIntent(intent);
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.getAccountId() > 0);
            startActivityForResult(intent, FILTER_REQUEST);
        });

        totalText = view.findViewById(R.id.total);
        totalText.setOnClickListener(view -> showTotals());

        Intent intent = context.getIntent();
        if (intent != null) {
            blotterFilter = WhereFilter.fromIntent(intent);
            saveFilter = intent.getBooleanExtra(SAVE_FILTER, true);
            isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
        }
        if (savedInstanceState != null) {
            blotterFilter = WhereFilter.fromBundle(savedInstanceState);
        }
        if (saveFilter && blotterFilter.isEmpty()) {
            blotterFilter = WhereFilter.fromSharedPreferences(context.getPreferences(0));
        }

        showAllBlotterButtons = !isAccountBlotter && !MyPreferences.isCollapseBlotterButtons(context);

        if (showAllBlotterButtons) {
            bTransfer = view.findViewById(R.id.bTransfer);
            bTransfer.setVisibility(View.VISIBLE);
            bTransfer.setOnClickListener(arg0 -> addItem(NEW_TRANSFER_REQUEST, TransferActivity.class));

            bTemplate = view.findViewById(R.id.bTemplate);
            bTemplate.setVisibility(View.VISIBLE);
            bTemplate.setOnClickListener(v -> createFromTemplate());
        }

        bSearch = view.findViewById(R.id.bSearch);
        bSearch.setOnClickListener(method -> {
            EditText searchText = view.findViewById(R.id.search_text);
            FrameLayout searchLayout = view.findViewById(R.id.search_text_frame);
            ImageButton searchTextClearButton = view.findViewById(R.id.search_text_clear);
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

            searchText.setOnFocusChangeListener((view, b) -> {
                if (!view.hasFocus()) {
                    imm.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
                }
            });

            searchTextClearButton.setOnClickListener(view -> {
                searchText.setText("");
            });

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

    private void applyPopupMenu() {
        bMenu = view.findViewById(R.id.bMenu);
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
                        MenuInflater inflater = context.getMenuInflater();
                        inflater.inflate(R.menu.ccard_blotter_menu, popupMenu.getMenu());
                    } else {
                        // Show menu for other accounts - monthly view
                        MenuInflater inflater = context.getMenuInflater();
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
        transactionActionGrid.setOnQuickActionClickListener(transactionActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener transactionActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
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
            }
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
                addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                break;
            case 1:
                addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        blotterFilter.toBundle(outState);
    }

    protected void createFromTemplate() {
        Intent intent = new Intent(context, SelectTemplateActivity.class);
        startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
    }

    @Override
    protected List<MenuItemInfo> createContextMenus(long id) {
        if (blotterFilter.isTemplate() || blotterFilter.isSchedule()) {
            return super.createContextMenus(id);
        } else {
            List<MenuItemInfo> menus = super.createContextMenus(id);
            menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
            menus.add(new MenuItemInfo(MENU_SAVE_AS_TEMPLATE, R.string.save_as_template));
            return menus;
        }
    }

    @Override
    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
        if (!super.onPopupItemSelected(itemId, view, position, id)) {
            switch (itemId) {
                case MENU_DUPLICATE:
                    duplicateTransaction(id, 1);
                    return true;
                case MENU_SAVE_AS_TEMPLATE:
                    new BlotterOperations(this, db, id).duplicateAsTemplate();
                    Toast.makeText(context, R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
                    return true;
            }
        }
        return false;
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

    @Override
    protected void addItem() {
        if (showAllBlotterButtons) {
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
        } else {
            addButtonActionGrid.show(bAdd);
        }
    }

    protected void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(context, clazz);
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        }
        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        startActivityForResult(intent, requestId);
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
    protected ListAdapter createAdapter(Cursor cursor) {
        if (isAccountBlotter) {
            return new TransactionsListAdapter(context, db, cursor);
        } else {
            return new BlotterListAdapter(context, db, cursor);
        }
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        deleteTransaction(id);
    }

    private void deleteTransaction(long id) {
        new BlotterOperations(this, db, id).deleteTransaction();
    }

    public void afterDeletingTransaction(long id) {
        recreateCursor();
        AccountWidget.updateWidgets(context);
    }

    @Override
    public void editItem(View v, int position, long id) {
        editTransaction(id);
    }

    private void editTransaction(long id) {
        new BlotterOperations(this, db, id).editTransaction();
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
        if(requestCode == NEW_TRANSACTION_REQUEST && resultCode == RESULT_CREATE_ANOTHER_TRANSACTION){
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
        }
        if(requestCode == NEW_TRANSFER_REQUEST && resultCode == RESULT_CREATE_ANOTHER_TRANSFER){
            addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
        }
        if (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER) {
            calculateTotals();
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
        SharedPreferences preferences = context.getPreferences(0);
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
            context.setTitle(getString(R.string.blotter) + " : " + title);
        }
        updateFilterImage();
    }

    protected void updateFilterImage() {
        FilterState.updateFilterColor(context, blotterFilter, bFilter);
    }

    private NodeInflater inflater;
    private long selectedId = -1;

    @Override
    protected void onItemClick(View v, int position, long id) {
        if (isQuickMenuEnabledForTransaction(context)) {
            selectedId = id;
            transactionActionGrid.show(v);
        } else {
            showTransactionInfo(id);
        }
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        showTransactionInfo(id);
    }

    private void showTransactionInfo(long id) {
        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(context, db, inflater);
        transactionInfoView.show(this, id);
    }

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(context).execute(new IntegrityCheckRunningBalance(context, db));
    }

//    @Override
//    public void onBackPressed() {
//        FrameLayout searchLayout = view.findViewById(R.id.search_text_frame);
//        if (searchLayout != null && searchLayout.getVisibility() == View.VISIBLE) {
//            searchLayout.setVisibility(View.GONE);
//        } else {
//            super.onBackPressed();
//        }
//    }
}
