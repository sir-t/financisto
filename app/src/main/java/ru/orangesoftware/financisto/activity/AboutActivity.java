/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.fragment.WebViewFragment;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class AboutActivity extends AppCompatActivity {
    private static final int ABOUT_TAB_COUNT = 4;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager_activity);
        String[] tabTitles = new String[ABOUT_TAB_COUNT];
        tabTitles[0] = getString(R.string.about);
        tabTitles[1] = getString(R.string.privacy_policy);
        tabTitles[2] = getString(R.string.whats_new);
        tabTitles[3] = getString(R.string.license);
        ViewPager viewPager = findViewById(R.id.view_pager);
        MyFragmentPagerAdapter adapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), tabTitles);
        viewPager.setAdapter(adapter);
    }

    private static class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        private final String[] mTabTitles;

        MyFragmentPagerAdapter(FragmentManager fragmentManager, String[] tabTitles) {
            super(fragmentManager);
            mTabTitles = tabTitles;
        }

        @Override
        public Fragment getItem(int position) {
            String fileName = "file:///android_asset/";
            switch (position) {
                case 0:
                    fileName += "about";
                    break;
                case 1:
                    fileName += "privacy";
                    break;
                case 2:
                    fileName += "whatsnew";
                    break;
                case 3:
                    fileName += "gpl-2.0-standalone";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown position");
            }
            fileName += ".htm";
            return WebViewFragment.newInstance(fileName);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }

        @Override
        public int getCount() {
            return mTabTitles.length;
        }
    }
}
