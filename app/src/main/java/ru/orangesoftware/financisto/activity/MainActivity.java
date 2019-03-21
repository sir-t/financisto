/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.bus.GreenRobotBus_;
import ru.orangesoftware.financisto.bus.RefreshCurrentTab;
import ru.orangesoftware.financisto.bus.SwitchToMenuTabEvent;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.dialog.WebViewDialog;
import ru.orangesoftware.financisto.fragment.AccountListFragment;
import ru.orangesoftware.financisto.fragment.BlotterFragment;
import ru.orangesoftware.financisto.fragment.BudgetListFragment;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

public class MainActivity extends FragmentActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private GreenRobotBus greenRobotBus;
    private FrameLayout main;
    private Fragment accListFragment;
    private Fragment blotterFragment;
    private Fragment budgetsFragment;
    private Fragment reportsFragment;
    private Fragment menuFragment;

    Fragment fragment;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        greenRobotBus = GreenRobotBus_.getInstance_(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        initialLoad();

//        RelativeLayout content = new RelativeLayout(this);
//        main = new FrameLayout(this);
//
//        FrameLayout.LayoutParams mainLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        content.addView(main, mainLP);
//
//        BottomNavigationView bottomNavigationView = new BottomNavigationView(this);
//        bottomNavigationView.inflateMenu(R.menu.main_views_menu);
//        bottomNavigationView.setOnNavigationItemSelectedListener(this);
//        bottomNavigationView.setBackgroundResource(R.color.bottom_bar_tint);
//
//
//        BottomNavigationView.LayoutParams bottomNavViewLP = new BottomNavigationView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        bottomNavViewLP.gravity = Gravity.BOTTOM;
//
//        content.addView(bottomNavigationView, bottomNavViewLP);
//
//        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setContentView(R.layout.main_layout);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.inflateMenu(R.menu.main_views_menu);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        accListFragment = new AccountListFragment();
        blotterFragment = new BlotterFragment();
        budgetsFragment = new BudgetListFragment();
        reportsFragment = new ReportsFragment();
        menuFragment = new MenuFragment();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSwitchToMenuTab(SwitchToMenuTabEvent event) {

//        getTabHost().setCurrentTabByTag("menu");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshCurrentTab(RefreshCurrentTab e) {
//        refreshCurrentTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        greenRobotBus.register(this);
        PinProtection.unlock(this);
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        greenRobotBus.unregister(this);
        PinProtection.lock(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinProtection.immediateLock(this);
    }

    private void initialLoad() {
        long t3, t2, t1, t0 = System.currentTimeMillis();
        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            SQLiteDatabase x = db.db();
            x.beginTransaction();
            t1 = System.currentTimeMillis();
            try {
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category));
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split));
                updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "name", getString(R.string.current_location));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "title", getString(R.string.current_location));
                x.setTransactionSuccessful();
            } finally {
                x.endTransaction();
            }
            t2 = System.currentTimeMillis();
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency();
            }
            CurrencyCache.initialize(db);
            t3 = System.currentTimeMillis();
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances();
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate();
            }
        } finally {
            db.close();
        }
        long t4 = System.currentTimeMillis();
        Log.d("Financisto", "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms");
    }

    private void updateFieldInTable(SQLiteDatabase db, String table, long id, String field, String value) {
        db.execSQL("update " + table + " set " + field + "=? where _id=?", new Object[]{value, id});
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setEnabled(true);
        switch (item.getItemId()) {
            case R.id.accounts_tab:
                fragment = accListFragment;
                break;
            case R.id.blotter_tab:
                fragment = blotterFragment;
                break;
            case R.id.budgets_tab:
                fragment = budgetsFragment;
                break;
            case R.id.reports_tab:
                fragment = reportsFragment;
                break;
            case R.id.menu_tab:
                fragment = menuFragment;
                break;

        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_container, fragment).commit();
        return true;
    }

//    @Override
//    public void onTabChanged(String tabId) {
//        Log.d("Financisto", "About to update tab " + tabId);
//        long t0 = System.currentTimeMillis();
//        refreshCurrentTab();
//        long t1 = System.currentTimeMillis();
//        Log.d("Financisto", "Tab " + tabId + " updated in " + (t1 - t0) + "ms");
//    }

//    public void refreshCurrentTab() {
//        Activity currentActivity = getLocalActivityManager().getCurrentActivity();
//        if (currentActivity instanceof RefreshSupportedActivity) {
//            RefreshSupportedActivity activity = (RefreshSupportedActivity) currentActivity;
//            activity.recreateCursor();
//            activity.integrityCheck();
//        }
//    }


//    private void setupAccountsTab(BottomNavigationView tabHost) {
//        addTab(tabHost.newTabSpec("accounts")
//                .setIndicator(getString(R.string.accounts), getResources().getDrawable(R.drawable.ic_tab_accounts))
//                .setContent(new Intent(this, AccountListActivity.class)));
//    }
//
//    private void setupBlotterTab(TabHost tabHost) {
//        Intent intent = new Intent(this, BlotterActivity.class);
//        intent.putExtra(BlotterActivity.SAVE_FILTER, true);
//        intent.putExtra(BlotterActivity.EXTRA_FILTER_ACCOUNTS, true);
//        tabHost.addTab(tabHost.newTabSpec("blotter")
//                .setIndicator(getString(R.string.blotter), getResources().getDrawable(R.drawable.ic_tab_blotter))
//                .setContent(intent));
//    }
//
//    private void setupBudgetsTab(TabHost tabHost) {
//        tabHost.addTab(tabHost.newTabSpec("budgets")
//                .setIndicator(getString(R.string.budgets), getResources().getDrawable(R.drawable.ic_tab_budgets))
//                .setContent(new Intent(this, BudgetListActivity.class)));
//    }
//
//    private void setupReportsTab(TabHost tabHost) {
//        tabHost.addTab(tabHost.newTabSpec("reports")
//                .setIndicator(getString(R.string.reports), getResources().getDrawable(R.drawable.ic_tab_reports))
//                .setContent(new Intent(this, ReportsListActivity.class)));
//    }
//
//    private void setupMenuTab(TabHost tabHost) {
//        tabHost.addTab(tabHost.newTabSpec("menu")
//                .setIndicator(getString(R.string.menu), getResources().getDrawable(R.drawable.ic_tab_menu))
//                .setContent(new Intent(this, MenuListActivity_.class)));
//    }


    public static class ReportsFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.reports_list, container, false);
        }
    }

    public static class MenuFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.activity_menu_list, container, false);
        }
    }
}
