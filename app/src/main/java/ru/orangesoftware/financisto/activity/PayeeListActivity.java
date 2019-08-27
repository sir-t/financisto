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

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.fragment.MyEntityListFragment;
import ru.orangesoftware.financisto.model.Payee;

public class PayeeListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new PayeeListFragment();
    }

    public static class PayeeListFragment extends MyEntityListFragment<Payee> {

        public PayeeListFragment() {
            super(Payee.class, R.string.no_payees);
        }

        @Override
        protected Class<PayeeActivity> getEditActivityClass() {
            return PayeeActivity.class;
        }

        @Override
        protected Criteria createBlotterCriteria(Payee p) {
            return Criteria.eq(BlotterFilter.PAYEE_ID, String.valueOf(p.id));
        }

    }

}
