package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.GenericBlotterActivity;
import ru.orangesoftware.financisto.activity.MyEntityActivity;
import ru.orangesoftware.financisto.adapter.EntityListAdapter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.widget.SearchFilterTextWatcherListener;

public abstract class MyEntityListFragment<T extends MyEntity> extends AbstractListFragment {

    private static final int NEW_ENTITY_REQUEST = 1;
    private static final int EDIT_ENTITY_REQUEST = 2;

    public static final int FILTER_DELAY_MILLIS = 500;

    private final Class<T> clazz;
    private final int emptyResId;

    private List<T> entities;
    private EditText searchFilter;
    protected volatile String titleFilter;

    protected abstract Class<? extends MyEntityActivity> getEditActivityClass();
    protected abstract Criteria createBlotterCriteria(T e);

    public MyEntityListFragment(Class<T> clazz, int emptyResId) {
        this(clazz, R.layout.entity_list, emptyResId);
    }

    private MyEntityListFragment(Class<T> clazz, int layoutId, int emptyResId) {
        super(layoutId);
        this.clazz = clazz;
        this.emptyResId = emptyResId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        loadEntities();
        TextView emptyView = view.findViewById(android.R.id.empty);
        emptyView.setText(emptyResId);

        searchFilter = view.findViewById(R.id.searchFilter);
        CheckBox checkBox = view.findViewById(R.id.toggleInactive);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> recreateCursor());
        if (searchFilter != null) {
            searchFilter.addTextChangedListener(new SearchFilterTextWatcherListener(FILTER_DELAY_MILLIS) {
                @Override
                public void clearFilter(String oldFilter) {
                    titleFilter = null;
                }

                @Override
                public void applyFilter(String filter) {
                    if (!TextUtils.isEmpty(filter)) titleFilter = filter;
                    recreateCursor();
                }
            });
        }

        return view;
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void recreateCursor() {
        loadEntities();
        @SuppressWarnings("unchecked")
        EntityListAdapter<T> a = (EntityListAdapter<T>) adapter;
        a.setEntities(entities);
    }

    @Override
    protected void updateAdapter() {
        if (adapter == null) {
            adapter = new EntityListAdapter<>(context, entities);
            setListAdapter(adapter);
        }
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(context, getEditActivityClass());
        startActivityForResult(intent, NEW_ENTITY_REQUEST);
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        db.delete(clazz, id);
        recreateCursor();
    }

    @Override
    protected void editItem(View v, int position, long id) {
        Intent intent = new Intent(context, getEditActivityClass());
        intent.putExtra(MyEntityActivity.ENTITY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_ENTITY_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        T e = db.load(clazz, id);
        Intent intent = new Intent(context, GenericBlotterActivity.class);
        Criteria blotterFilter = createBlotterCriteria(e);
        blotterFilter.toIntent(e.title, intent);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            recreateCursor();
        }
    }

    protected void loadEntities() {
        CheckBox checkBox = view.findViewById(R.id.toggleInactive);
        boolean showInactive = checkBox.isChecked();
        this.entities = loadEntities(!showInactive);
    }

    private List<T> loadEntities(boolean onlyActive) {
        return db.getAllEntitiesList(clazz, false, onlyActive, titleFilter);
    }
}
