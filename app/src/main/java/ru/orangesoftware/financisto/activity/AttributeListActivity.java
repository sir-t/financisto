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

import ru.orangesoftware.financisto.fragment.AttributeListFragment;

import androidx.fragment.app.Fragment;

public class AttributeListActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new AttributeListFragment();
	}

}
