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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AttributeActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
import ru.orangesoftware.financisto.databinding.AttributesListBinding;
import ru.orangesoftware.financisto.databinding.GenericRecyclerItemBinding;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import ru.orangesoftware.financisto.model.Attribute;

public class AttributeListFragment extends AbstractRecycleFragment implements ItemClick, ItemSwipeable {

	private static final int NEW_ATTRIBUTE_REQUEST = 1;
	private static final int EDIT_ATTRIBUTE_REQUEST = 2;

	private String[] attributeTypes;

	public AttributeListFragment() {
		super(R.layout.attributes_list);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		attributeTypes = getResources().getStringArray(R.array.attribute_types);

		getRecyclerView().addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
		getRecyclerView().setItemAnimator(new DefaultItemAnimator());

		AttributesListBinding binding = (AttributesListBinding) getBinding();
		binding.bAdd.setOnClickListener(v -> {
			Intent intent = new Intent(context, AttributeActivity.class);
			startActivityForResult(intent, NEW_ATTRIBUTE_REQUEST);
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			recreateCursor();
		}
	}

	@Override
	protected Cursor createCursor() {
		return db.getAllAttributes();
	}

	@Override
	protected void updateAdapter() {
		if (getListAdapter() == null) {
			setListAdapter(new AttributeRecyclerAdapter(getCursor()));
		} else {
			((AttributeRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
		}
	}

	@Override
	public void onItemClick(View view, int position) {
		long id = getListAdapter().getItemId(position);
		editAttribute(id);
	}

	@Override
	public Integer[] getSwipeOptions() {
		return new Integer[]{R.id.delete_task, R.id.edit_task};
	}

	@Override
	public void onSwipeClick(int viewID, int position) {
		long id = getListAdapter().getItemId(position);
		switch (viewID) {
			case R.id.delete_task:
				new AlertDialog.Builder(context)
						.setMessage(R.string.attribute_delete_alert)
						.setPositiveButton(R.string.yes, (arg0, arg1) -> {
							db.deleteAttribute(id);
							recreateCursor();
						})
						.setNegativeButton(R.string.no, null)
						.show();
				break;
			case R.id.edit_task:
				editAttribute(id);
				break;
		}
	}

	private void editAttribute(long id) {
		Intent intent = new Intent(context, AttributeActivity.class);
		intent.putExtra(AttributeActivity.ATTRIBUTE_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_ATTRIBUTE_REQUEST);
	}

	private class AttributeRecyclerItemHolder extends RecyclerView.ViewHolder {

		private final GenericRecyclerItemBinding mBinding;

		AttributeRecyclerItemHolder(GenericRecyclerItemBinding binding) {
			super(binding.getRoot());
			mBinding = binding;
		}

		void bind(Attribute a) {
			mBinding.line.setText(a.title);
			mBinding.number.setText(attributeTypes[a.type - 1]);
			String defaultValue = a.getDefaultValue();
			if (defaultValue != null) {
				mBinding.amount.setVisibility(View.VISIBLE);
				mBinding.amount.setText(defaultValue);
			} else {
				mBinding.amount.setVisibility(View.GONE);
			}
		}

	}

	public class AttributeRecyclerAdapter extends BaseCursorRecyclerAdapter<AttributeRecyclerItemHolder> {

		AttributeRecyclerAdapter(Cursor c) {
			super(c);
		}

		@NonNull
		@Override
		public AttributeRecyclerItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			GenericRecyclerItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.generic_recycler_item, parent, false);
			return new AttributeRecyclerItemHolder(binding);
		}

		@Override
		public void onBindViewHolder(AttributeRecyclerItemHolder holder, Cursor cursor) {
			Attribute a = Attribute.fromCursor(cursor);
			holder.bind(a);
		}

	}

}
