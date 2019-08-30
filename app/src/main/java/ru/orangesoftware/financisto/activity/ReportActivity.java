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
package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.fragment.ReportFragment;

public class ReportActivity extends SingleFragmentActivity {

    private static final String EXTRA_FILTER = "FILTER";
    private static final String EXTRA_REPORT_TYPE = "REPORT_TYPE";
    private static final String EXTRA_FILTER_INCOME_EXPENSE = "FILTER_INCOME_EXPENSE";

    @Override
    protected Fragment createFragment() {
        Intent intent = getIntent();
        return ReportFragment.newInstance(intent.getBundleExtra(EXTRA_FILTER), intent.getStringExtra(EXTRA_REPORT_TYPE), intent.getStringExtra(EXTRA_FILTER_INCOME_EXPENSE));
    }

    public static Intent newIntent(Context context, WhereFilter filter, String reportType, String incomeExpenseName) {
        Intent intent = new Intent(context, ReportActivity.class);
        Bundle filterBundle = new Bundle();
        filter.toBundle(filterBundle);
        intent.putExtra(EXTRA_FILTER, filterBundle);
        intent.putExtra(EXTRA_REPORT_TYPE, reportType);
        intent.putExtra(EXTRA_FILTER_INCOME_EXPENSE, incomeExpenseName);
        return intent;
    }
}
