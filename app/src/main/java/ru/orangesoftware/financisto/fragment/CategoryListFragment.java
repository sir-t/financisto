package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AttributeListActivity;
import ru.orangesoftware.financisto.activity.CategoryActivity;
import ru.orangesoftware.financisto.databinding.CategoryListBinding;
import ru.orangesoftware.financisto.databinding.CategoryListItemBinding;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemMenuShow;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

public class CategoryListFragment extends AbstractRecycleFragment implements ItemClick, ItemSwipeable, ItemMenuShow {

    private static final int EMPTY_REQUEST = 0;
    private static final int NEW_CATEGORY_REQUEST = 1;
    private static final int EDIT_CATEGORY_REQUEST = 2;

    private CategoryTree<Category> categories;
    private Map<Long, String> attributes;

    public CategoryListFragment() {
        super(R.layout.category_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CategoryListBinding binding = (CategoryListBinding) getBinding();
        binding.bAdd.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoryActivity.class);
            startActivityForResult(intent, NEW_CATEGORY_REQUEST);
        });
        binding.bAttributes.setOnClickListener(v -> {
            Intent intent = new Intent(context, AttributeListActivity.class);
            startActivityForResult(intent, EMPTY_REQUEST);
        });
        binding.bCollapseAll.setOnClickListener(v -> ((CategoryRecyclerAdapter) getListAdapter()).collapseAllCategories());
        binding.bExpandAll.setOnClickListener(v -> ((CategoryRecyclerAdapter) getListAdapter()).expandAllCategories());
        binding.bSort.setOnClickListener(v -> sortByTitle());
        binding.bFix.setOnClickListener(v -> reIndex());

        getTouchListener().setIgnoredViewTypes(R.id.span);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            updateAdapter();
        }
    }

    @Override
    protected void updateAdapter() {
        categories = db.getCategoriesTree(false);
        attributes = db.getAllAttributesMap();

        if (getListAdapter() == null) {
            setListAdapter(new CategoryRecyclerAdapter(categories));
        } else {
            ((CategoryRecyclerAdapter) getListAdapter()).setCategories(categories);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        CategoryRecyclerAdapter adapter = (CategoryRecyclerAdapter) getListAdapter();
        Category c = adapter.getItem(position);
        if (c.hasChildren()) {
            adapter.onListItemClick(c.id);
        }
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
                Category c = ((CategoryRecyclerAdapter) getListAdapter()).getItem(position);
                new AlertDialog.Builder(context)
                        .setTitle(c.getTitle())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.delete_category_dialog)
                        .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                            db.deleteCategory(id);
                            updateAdapter();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                break;
            case R.id.edit_task:
                editCategory(id);
                break;
        }
    }

    @Override
    public List<MenuItemInfo> getMenuContext(View view, int position) {
        final Category c = ((CategoryRecyclerAdapter) getListAdapter()).getItem(position);
        Category p = c.parent;
        CategoryTree<Category> tree = (p == null ? this.categories : p.children);
        final int pos = tree.indexOf(c);

        List<MenuItemInfo> menus = new LinkedList<>();
        if (pos > 0) {
            menus.add(new MenuItemInfo(1, R.string.position_move_top, R.drawable.ic_btn_round_top));
            menus.add(new MenuItemInfo(2, R.string.position_move_up, R.drawable.ic_btn_round_up));
        }
        if (pos < tree.size() - 1) {
            menus.add(new MenuItemInfo(3, R.string.position_move_down, R.drawable.ic_btn_round_down));
            menus.add(new MenuItemInfo(4, R.string.position_move_bottom, R.drawable.ic_btn_round_bottom));
        }
        if (c.hasChildren()) {
            menus.add(new MenuItemInfo(5, R.string.sort_by_title, R.drawable.ic_btn_round_sort_by_title));
        }
        return menus;
    }

    @Override
    public boolean onMenuClick(int menuID, int position) {
        final Category c = ((CategoryRecyclerAdapter) getListAdapter()).getItem(position);
        Category p = c.parent;
        CategoryTree<Category> tree = (p == null ? this.categories : p.children);
        final int pos = tree.indexOf(c);

        boolean result = false;
        switch (menuID) {
            case 1: {
                result = tree.moveCategoryToTheTop(pos);
                break;
            }
            case 2: {
                result = tree.moveCategoryUp(pos);
                break;
            }
            case 3: {
                result = tree.moveCategoryDown(pos);
                break;
            }
            case 4: {
                result = tree.moveCategoryToTheBottom(pos);
                break;
            }
            case 5: {
                result = c.children.sortByTitle();
                break;
            }
        }
        if (result) {
            db.updateCategoryTree(tree);
            updateAdapter();
        }
        return result;
    }

    private void editCategory(long id) {
        Intent intent = new Intent(context, CategoryActivity.class);
        intent.putExtra(CategoryActivity.CATEGORY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_CATEGORY_REQUEST);
    }

    private void sortByTitle() {
        if (categories.sortByTitle()) {
            db.updateCategoryTree(categories);
            updateAdapter();
        }
    }

    private void reIndex() {
        db.restoreSystemEntities();
        updateAdapter();
    }

    private class CategoryItemHolder extends RecyclerView.ViewHolder {

        private final CategoryListItemBinding mBinding;

        private final Drawable expandedDrawable;
        private final Drawable collapsedDrawable;
        private final int incomeColor;
        private final int expenseColor;
        private final int levelPadding;

        CategoryItemHolder(CategoryListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            Resources resources = context.getResources();
            this.expandedDrawable = resources.getDrawable(R.drawable.expander_ic_maximized);
            this.collapsedDrawable = resources.getDrawable(R.drawable.expander_ic_minimized);
            this.incomeColor = resources.getColor(R.color.category_type_income);
            this.expenseColor = resources.getColor(R.color.category_type_expense);
            this.levelPadding = 2 * resources.getDimensionPixelSize(R.dimen.category_padding);
        }

        void bind(Category c, boolean isExpanded) {
            mBinding.title.setText(c.title);
            int padding = levelPadding * (c.level - 1);
            if (c.hasChildren()) {
                mBinding.span.setImageDrawable(isExpanded ? expandedDrawable : collapsedDrawable);
                mBinding.span.setPadding(padding, 0, 0, 0);
                mBinding.span.setVisibility(View.VISIBLE);
                padding += collapsedDrawable.getMinimumWidth();
            } else {
                padding += levelPadding / 2;
                mBinding.span.setVisibility(View.GONE);
            }
            mBinding.title.setPadding(padding, 0, 0, 0);
            mBinding.label.setPadding(padding, 0, 0, 0);
            long id = c.id;
            if (attributes != null && attributes.containsKey(id)) {
                mBinding.label.setText(attributes.get(id));
                mBinding.label.setVisibility(View.VISIBLE);
            } else {
                mBinding.label.setVisibility(View.GONE);
            }
            if (c.isIncome()) {
                mBinding.indicator.setBackgroundColor(incomeColor);
            } else if (c.isExpense()) {
                mBinding.indicator.setBackgroundColor(expenseColor);
            } else {
                mBinding.indicator.setBackgroundColor(Color.WHITE);
            }
        }
    }

    private class CategoryRecyclerAdapter extends RecyclerView.Adapter<CategoryItemHolder> {

        private CategoryTree<Category> categories;

        private final ArrayList<Category> list = new ArrayList<>();
        private final HashSet<Long> state = new HashSet<>();

        public CategoryRecyclerAdapter(CategoryTree<Category> categories) {
            this.categories = categories;
            recreatePlainList();
        }

        @NonNull
        @Override
        public CategoryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            CategoryListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.category_list_item, parent, false);
            return new CategoryItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryItemHolder holder, int position) {
            Category c = list.get(position);
            holder.bind(c, state.contains(c.id));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        public Category getItem(int position) {
            return list.get(position);
        }

        public void onListItemClick(long id) {
            if (state.contains(id)) {
                state.remove(id);
            } else {
                state.add(id);
            }
            recreatePlainList();
            notifyDataSetChanged();
        }

        private void recreatePlainList() {
            list.clear();
            addCategories(categories);
        }

        private void addCategories(CategoryTree<Category> categories) {
            if (categories == null || categories.isEmpty()) {
                return;
            }
            for (Category c : categories) {
                list.add(c);
                if (state.contains(c.id)) {
                    addCategories(c.children);
                }
            }
        }

        public void collapseAllCategories() {
            state.clear();
            recreatePlainList();
            notifyDataSetChanged();
        }

        public void expandAllCategories() {
            expandAllCategories(categories);
            recreatePlainList();
            notifyDataSetChanged();
        }

        private void expandAllCategories(CategoryTree<Category> categories) {
            if (categories == null || categories.isEmpty()) {
                return;
            }
            for (Category c : categories) {
                state.add(c.id);
                expandAllCategories(c.children);
            }
        }

        public void setCategories(CategoryTree<Category> categories) {
            this.categories = categories;
            recreatePlainList();
            notifyDataSetChanged();
        }

    }

}
