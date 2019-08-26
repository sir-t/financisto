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
package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AttributeActivity;
import ru.orangesoftware.financisto.adapter.AttributeListAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

public class AttributeListFragment extends AbstractListFragment {

	private static final int NEW_ATTRIBUTE_REQUEST = 1;
	private static final int EDIT_ATTRIBUTE_REQUEST = 2;

	public AttributeListFragment() {
		super(R.layout.attributes_list);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		context = getActivity();
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	protected Cursor createCursor() {
		return db.getAllAttributes();
	}

	@Override
	protected void updateAdapter() {
		if (adapter == null) {
			adapter = new AttributeListAdapter(db, context, cursor);
			setListAdapter(adapter);
		} else {
			((AttributeListAdapter) adapter).changeCursor(cursor);
			((AttributeListAdapter) adapter).notifyDataSetChanged();
		}
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		new AlertDialog.Builder(context)
				.setMessage(R.string.attribute_delete_alert)
				.setPositiveButton(R.string.yes, (arg0, arg1) -> {
						db.deleteAttribute(id);
						recreateCursor();
				})
				.setNegativeButton(R.string.no, null)
				.show();
	}

	@Override
	protected void addItem() {
		addAttribute();
	}

	@Override
	protected void editItem(View v, int position, long id) {
		editAttribute(id);
	}

	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			recreateCursor();
		}
	}

	@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = super.createContextMenus(id);
		for (MenuItemInfo m : menus) {
			if (m.menuId == MENU_VIEW) {
				m.enabled = false;
				break;
			}
		}
		return menus;
	}

	private void addAttribute() {
		Intent intent = new Intent(context, AttributeActivity.class);
		startActivityForResult(intent, NEW_ATTRIBUTE_REQUEST);
	}

	private void editAttribute(long id) {
		Intent intent = new Intent(context, AttributeActivity.class);
		intent.putExtra(AttributeActivity.ATTRIBUTE_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_ATTRIBUTE_REQUEST);
	}

}
