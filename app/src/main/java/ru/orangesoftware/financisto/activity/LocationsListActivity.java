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

import android.view.View;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.fragment.MyEntityListFragment;
import ru.orangesoftware.financisto.model.MyLocation;

public class LocationsListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new LocationsListFragment();
    }

    public static class LocationsListFragment extends MyEntityListFragment<MyLocation> {

        public LocationsListFragment() {
            super(MyLocation.class, R.string.no_locations);
        }

        @Override
        protected Class<LocationActivity> getEditActivityClass() {
            return LocationActivity.class;
        }

        @Override
        protected Criteria createBlotterCriteria(MyLocation location) {
            return Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(location.id));
        }

        @Override
        protected void deleteItem(View v, int position, long id) {
            db.deleteLocation(id);
            recreateCursor();
        }

    }

}
