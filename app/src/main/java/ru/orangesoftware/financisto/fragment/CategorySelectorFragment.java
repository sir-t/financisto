package ru.orangesoftware.financisto.fragment;

import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.CategoryTreeNavigator;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class CategorySelectorFragment extends AbstractListFragment {

    private static final String ARG_SELECTED_CATEGORY_ID = "selected_category_id";
    private static final String ARG_EXCLUDED_SUB_TREE_ID = "excluded_sub_tree_id";
    private static final String ARG_INCLUDE_SPLIT_CATEGORY = "included_split_category";

    private int incomeColor;
    private int expenseColor;

    private CategoryTreeNavigator navigator;
    private Map<Long, String> attributes;

    private Button bBack;
    private Callbacks mCallbacks;

    public interface Callbacks {
        void onCategorySelected(long category_id);
    }

    public static CategorySelectorFragment newInstance(Callbacks callbacks) {
        return new CategorySelectorFragment(callbacks);
    }

    public static CategorySelectorFragment newInstance(Callbacks callbacks, long selectedId, long excludedSubTreeId, boolean includedSplit) {
        Bundle args = new Bundle();
        args.putLong(ARG_SELECTED_CATEGORY_ID, selectedId);
        args.putLong(ARG_EXCLUDED_SUB_TREE_ID, excludedSubTreeId);
        args.putBoolean(ARG_INCLUDE_SPLIT_CATEGORY, includedSplit);

        CategorySelectorFragment fragment = new CategorySelectorFragment(callbacks);
        fragment.setArguments(args);
        return fragment;
    }

    private CategorySelectorFragment(Callbacks callbacks) {
        super(R.layout.category_selector);
        enablePin = false;
        mCallbacks = callbacks;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Resources resources = getResources();
        incomeColor = resources.getColor(R.color.category_type_income);
        expenseColor = resources.getColor(R.color.category_type_expense);

        long excTreeId = getArguments().getLong(ARG_EXCLUDED_SUB_TREE_ID, -1);
        Bundle args = getArguments();
        if (args != null)
            excTreeId = args.getLong(ARG_EXCLUDED_SUB_TREE_ID, -1);

        navigator = new CategoryTreeNavigator(db, excTreeId);
        if (MyPreferences.isSeparateIncomeExpense(context)) {
            navigator.separateIncomeAndExpense();
        }
        attributes = db.getAllAttributesMap();

        if (args != null) {
            boolean includeSplit = args.getBoolean(ARG_INCLUDE_SPLIT_CATEGORY, false);
            if (includeSplit) {
                navigator.addSplitCategoryToTheTop();
            }
            navigator.selectCategory(args.getLong(ARG_SELECTED_CATEGORY_ID, 0));
        }

        bBack = view.findViewById(R.id.bBack);
        bBack.setOnClickListener(view -> {
            if (navigator.goBack()) {
                updateAdapter();
            }
        });

        Button bSelect = view.findViewById(R.id.bSelect);
        bSelect.setOnClickListener(view -> confirmSelection());

        updateAdapter();

        return view;
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected void updateAdapter() {
        if (navigator != null) {
            bBack.setEnabled(navigator.canGoBack());
            adapter = new CategoryAdapter(navigator.categories);
            setListAdapter(adapter);
        }
    }

    @Override
    protected void deleteItem(View v, int position, long id) { }

    @Override
    protected void editItem(View v, int position, long id) { }

    @Override
    protected void viewItem(View v, int position, long id) {
        if (navigator.navigateTo(id)) {
            updateAdapter();
        } else {
            if (MyPreferences.isAutoSelectChildCategory(context)) {
                confirmSelection();
            }
        }
    }

    @Override
    protected List<MenuItemInfo> createContextMenus(long id) {
        return Collections.emptyList();
    }

    private void confirmSelection() {
        mCallbacks.onCategorySelected(navigator.selectedCategoryId);
    }

    private class CategoryAdapter extends BaseAdapter {

        private final CategoryTree<Category> categories;

        private CategoryAdapter(CategoryTree<Category> categories) {
            this.categories = categories;
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Category getItem(int i) {
            return categories.getAt(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BlotterListAdapter.BlotterViewHolder v;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.blotter_list_item, parent, false);
                v = new BlotterListAdapter.BlotterViewHolder(convertView);
                convertView.setTag(v);
            } else {
                v = (BlotterListAdapter.BlotterViewHolder)convertView.getTag();
            }
            Category c = getItem(position);
            if (c.id == CategoryTreeNavigator.INCOME_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.income));
            } else if (c.id == CategoryTreeNavigator.EXPENSE_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.expense));
            } else {
                v.centerView.setText(c.title);
            }
            v.bottomView.setText(c.tag);
            v.indicator.setBackgroundColor(c.isIncome() ? incomeColor : expenseColor);
            v.rightCenterView.setVisibility(View.INVISIBLE);
            v.iconView.setVisibility(View.INVISIBLE);
            if (attributes != null && attributes.containsKey(c.id)) {
                v.rightView.setText(attributes.get(c.id));
                v.rightView.setVisibility(View.VISIBLE);
            } else {
                v.rightView.setVisibility(View.GONE);
            }
            v.topView.setVisibility(View.INVISIBLE);
            if (navigator.isSelected(c.id)) {
                v.layout.setBackgroundResource(R.drawable.list_selector_background_focus);
            } else {
                v.layout.setBackgroundResource(0);
            }
            v.eReceiptView.setVisibility(View.GONE);
            return convertView;
        }
    }

}
