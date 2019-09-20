package ru.orangesoftware.financisto.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.databinding.BlotterListItemBinding;
import ru.orangesoftware.financisto.databinding.CategorySelectorBinding;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.CategoryTreeNavigator;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class CategorySelectorFragment extends AbstractRecycleFragment implements AbstractRecycleFragment.ItemClick {

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        CategorySelectorBinding binding = (CategorySelectorBinding) getBinding();
        bBack = binding.bBack;
        bBack.setOnClickListener(v -> {
            if (navigator.goBack()) {
                updateAdapter();
            }
        });
        binding.bSelect.setOnClickListener(v -> confirmSelection());
    }

    @Override
    protected void updateAdapter() {
        if (navigator != null) {
            bBack.setEnabled(navigator.canGoBack());
            setListAdapter(new CategoryRecyclerAdapter(navigator.categories));
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        long id = getListAdapter().getItemId(position);
        if (navigator.navigateTo(id)) {
            updateAdapter();
        } else {
            if (MyPreferences.isAutoSelectChildCategory(context)) {
                confirmSelection();
            }
        }
    }

    private void confirmSelection() {
        mCallbacks.onCategorySelected(navigator.selectedCategoryId);
    }

    private class CategoryItemHolder extends RecyclerView.ViewHolder {

        private final BlotterListItemBinding mBinding;

        CategoryItemHolder(BlotterListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(Category c) {
            if (c.id == CategoryTreeNavigator.INCOME_CATEGORY_ID) {
                mBinding.center.setText(getString(R.string.income));
            } else if (c.id == CategoryTreeNavigator.EXPENSE_CATEGORY_ID) {
                mBinding.center.setText(getString(R.string.expense));
            } else {
                mBinding.center.setText(c.title);
            }
            mBinding.bottom.setText(c.tag);
            mBinding.indicator.setBackgroundColor(c.isIncome() ? incomeColor : expenseColor);
            mBinding.rightCenter.setVisibility(View.INVISIBLE);
            mBinding.icon.setVisibility(View.INVISIBLE);
            if (attributes != null && attributes.containsKey(c.id)) {
                mBinding.right.setText(attributes.get(c.id));
                mBinding.right.setVisibility(View.VISIBLE);
            } else {
                mBinding.right.setVisibility(View.GONE);
            }
            mBinding.top.setVisibility(View.INVISIBLE);
            if (navigator.isSelected(c.id)) {
                mBinding.rowFG.setBackgroundResource(R.drawable.list_selector_background_focus);
            } else {
                mBinding.rowFG.setBackgroundResource(0);
            }
            mBinding.rightQrCode.setVisibility(View.GONE);
            mBinding.getRoot().findViewById(R.id.rowBG).setVisibility(View.GONE);
        }

    }

    public class CategoryRecyclerAdapter extends RecyclerView.Adapter<CategoryItemHolder> {

        private final CategoryTree<Category> categories;

        CategoryRecyclerAdapter(CategoryTree<Category> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public CategoryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            BlotterListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.blotter_list_item, parent, false);
            return new CategoryItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryItemHolder holder, int position) {
            Category c = categories.getAt(position);
            holder.bind(c);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        @Override
        public long getItemId(int position) {
            return categories.getAt(position).id;
        }

    }

}
