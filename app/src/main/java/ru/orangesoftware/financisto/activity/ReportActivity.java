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

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;
import ru.orangesoftware.financisto.R;

public class ReportActivity extends FragmentActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout content = new LinearLayout(this);
        content.setId(R.id.main_container);
        setContentView(content);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_container, new ru.orangesoftware.financisto.fragment.ReportFragment())
                .commit();
    }

}
