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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationViewPager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

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
import ru.orangesoftware.financisto.fragment.BottomNavigationSupported;
import ru.orangesoftware.financisto.fragment.BudgetListFragment;
import ru.orangesoftware.financisto.fragment.MenuFragment;
import ru.orangesoftware.financisto.fragment.ReportFragment;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

public class MainActivity extends FragmentActivity {

    private GreenRobotBus greenRobotBus;

    Fragment fragment;
    private AHBottomNavigationViewPager viewPager;
    private AHBottomNavigation bottomNavigationView;
    private AHBottomNavigationAdapter navigationAdapter;
    private ViewPagerAdapter adapter;

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

        setContentView(R.layout.main_layout);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.main_container);

        navigationAdapter = new AHBottomNavigationAdapter(this, R.menu.main_views_menu);
        navigationAdapter.setupWithBottomNavigation(bottomNavigationView);
        bottomNavigationView.setDefaultBackgroundResource(R.color.colorBottomNavigationBackground);
        bottomNavigationView.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                if (fragment == null) {
                    fragment = adapter.getCurrentFragment();
                }
                if (wasSelected) {
                    if (fragment instanceof BottomNavigationSupported)
                        ((BottomNavigationSupported) fragment).refreshFragment();
                    return true;
                }
                if (fragment != null && fragment instanceof BottomNavigationSupported) {
                    ((BottomNavigationSupported) fragment).willBeHidden();
                }

                viewPager.setCurrentItem(position, false);

                if (fragment == null) {
                    return true;
                }

                fragment = adapter.getCurrentFragment();
                if (fragment instanceof BottomNavigationSupported) {
                    ((BottomNavigationSupported) fragment).willBeDisplayed();
                }

                return true;
            }
        });

        viewPager.setOffscreenPageLimit(4);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        MyPreferences.StartupScreen screen = MyPreferences.getStartupScreen(this);
        switch (screen) {
            case ACCOUNTS:
                bottomNavigationView.setCurrentItem(0);
                break;
            case BUDGETS:
                bottomNavigationView.setCurrentItem(2);
                break;
            case REPORTS:
                bottomNavigationView.setCurrentItem(3);
                break;
            case BLOTTER:
            default:
                bottomNavigationView.setCurrentItem(1);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshCurrentTab(RefreshCurrentTab e) {
        refreshCurrentTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        greenRobotBus.register(this);
        PinProtection.unlock(this);
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);
        }
        refreshCurrentTab();
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
    public void onBackPressed() {
        if (fragment instanceof BlotterFragment) {
            FrameLayout searchLayout = fragment.getView().findViewById(R.id.search_text_frame);
            if (searchLayout != null && searchLayout.getVisibility() == View.VISIBLE) {
                searchLayout.setVisibility(View.GONE);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void refreshCurrentTab() {
        if (fragment instanceof RefreshSupportedActivity) {
            ((RefreshSupportedActivity) fragment).recreateCursor();
            ((RefreshSupportedActivity) fragment).integrityCheck();
        }
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments = new ArrayList<>();
        private Fragment currentFragment;

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);

            fragments.clear();
            fragments.add(new AccountListFragment());
            fragments.add(new BlotterFragment());
            fragments.add(new BudgetListFragment());
            fragments.add(new ReportFragment());
            fragments.add(new MenuFragment());
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            if (getCurrentFragment() != object)
                currentFragment = (Fragment) object;
            super.setPrimaryItem(container, position, object);
        }

        public Fragment getCurrentFragment() {
            return currentFragment;
        }
    }
}
