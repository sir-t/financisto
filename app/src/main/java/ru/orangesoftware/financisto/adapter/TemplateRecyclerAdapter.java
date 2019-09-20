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
package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.database.Cursor;

import ru.orangesoftware.financisto.db.DatabaseAdapter;

public class TemplateRecyclerAdapter extends BlotterRecyclerAdapter {

	public TemplateRecyclerAdapter(Context context, DatabaseAdapter db, Cursor c) {
		super(context, db, c);
	}

    @Override
    protected boolean isShowRunningBalance() {
        return false;
    }

}
