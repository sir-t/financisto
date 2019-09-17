package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.GenericBlotterActivity;
import ru.orangesoftware.financisto.activity.MyEntityActivity;
import ru.orangesoftware.financisto.databinding.EntityListBinding;
import ru.orangesoftware.financisto.databinding.EntityListItemBinding;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.widget.SearchFilterTextWatcherListener;

import static ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.*;

public abstract class MyEntityListFragment<T extends MyEntity> extends AbstractRecycleFragment implements ItemClick, ItemSwipeable {

    private static final int NEW_ENTITY_REQUEST = 1;
    private static final int EDIT_ENTITY_REQUEST = 2;

    public static final int FILTER_DELAY_MILLIS = 500;

    private final Class<T> clazz;
    private final int emptyResId;

    private List<T> entities;
    private CheckBox toggleInactive;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getRecyclerView().addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        getRecyclerView().setItemAnimator(new DefaultItemAnimator());

        EntityListBinding binding = (EntityListBinding) getBinding();
        binding.listEmpty.setText(emptyResId);
        binding.searchFilter.addTextChangedListener(new SearchFilterTextWatcherListener(FILTER_DELAY_MILLIS) {
            @Override
            public void clearFilter(String oldFilter) {
                titleFilter = null;
            }

            @Override
            public void applyFilter(String filter) {
                if (!TextUtils.isEmpty(filter)) titleFilter = filter;
                updateAdapter();
            }
        });
        binding.bAdd.setOnClickListener(v -> {
            Intent intent = new Intent(context, getEditActivityClass());
            startActivityForResult(intent, NEW_ENTITY_REQUEST);
        });

        toggleInactive = binding.toggleInactive;
        toggleInactive.setOnCheckedChangeListener((buttonView, isChecked) -> updateAdapter());

        loadEntities();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            updateAdapter();
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        T e = db.load(clazz, getListAdapter().getItemId(position));
        Intent intent = new Intent(context, GenericBlotterActivity.class);
        Criteria blotterFilter = createBlotterCriteria(e);
        blotterFilter.toIntent(e.title, intent);
        startActivity(intent);
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
                        .setMessage(R.string.delete_account_confirm)
                        .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                            deleteItem(viewID, position, id);
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                break;
            case R.id.edit_task:
                Intent intent = new Intent(context, getEditActivityClass());
                intent.putExtra(MyEntityActivity.ENTITY_ID_EXTRA, id);
                startActivityForResult(intent, EDIT_ENTITY_REQUEST);
                break;
        }
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null) {
            setListAdapter(new EntityListAdapter<>(entities));
        } else {
            loadEntities();
            @SuppressWarnings("unchecked")
            EntityListAdapter<T> adapter = (EntityListAdapter<T>) getListAdapter();
            adapter.setEntities(entities);
        }
    }

    protected void deleteItem(int viewID, int position, long id) {
        db.delete(clazz, id);
        updateAdapter();
    }

    private void loadEntities() {
        boolean showInactive = toggleInactive.isChecked();
        this.entities = loadEntities(!showInactive);
    }

    private List<T> loadEntities(boolean onlyActive) {
        return db.getAllEntitiesList(clazz, false, onlyActive, titleFilter);
    }

    private static class EntityItemHolder<T extends MyEntity> extends RecyclerView.ViewHolder {

        private final EntityListItemBinding mBinding;

        public EntityItemHolder(@NonNull EntityListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(T item) {
            mBinding.title.setText(item.title);
            mBinding.icon.setImageResource(item.isActive ? R.drawable.entity_active_icon : R.drawable.entity_inactive_icon);
        }
    }

    public class EntityListAdapter<TT extends MyEntity> extends RecyclerView.Adapter<EntityItemHolder<TT>> {

        private List<TT> entities;

        public EntityListAdapter(List<TT> entities) {
            this.entities = entities;
        }

        @NonNull
        @Override
        public EntityItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            EntityListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.entity_list_item, parent, false);
            return new EntityItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull EntityItemHolder<TT> holder, int position) {
            holder.bind(entities.get(position));
        }

        @Override
        public int getItemCount() {
            return entities.size();
        }

        @Override
        public long getItemId(int position) {
            return entities.get(position).id;
        }

        public void setEntities(List<TT> entities) {
            this.entities = entities;
            notifyDataSetChanged();
        }

    }

}
