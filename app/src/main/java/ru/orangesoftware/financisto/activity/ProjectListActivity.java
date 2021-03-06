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

import android.view.View;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.fragment.MyEntityListFragment;
import ru.orangesoftware.financisto.model.Project;

public class ProjectListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new ProjectListFragment();
    }

    public static class ProjectListFragment extends MyEntityListFragment<Project> {

        public ProjectListFragment() {
            super(Project.class, R.string.no_projects);
        }

        @Override
        protected Class<ProjectActivity> getEditActivityClass() {
            return ProjectActivity.class;
        }

        @Override
        protected Criteria createBlotterCriteria(Project p) {
            return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(p.id));
        }

        @Override
        protected void deleteItem(int viewID, int position, long id) {
            db.deleteProject(id);
            updateAdapter();
        }

    }

}
