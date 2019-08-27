package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.fragment.CategorySelectorFragment;

public class CategorySelectorActivity extends SingleFragmentActivity implements CategorySelectorFragment.Callbacks {

    public static final String EXTRA_SELECTED_CATEGORY_ID = "selected_category_id";

    private static final String SELECTED_CATEGORY_ID = "SELECTED_CATEGORY_ID";
    private static final String EXCLUDED_SUB_TREE_ID = "EXCLUDED_SUB_TREE_ID";
    private static final String INCLUDE_SPLIT_CATEGORY = "INCLUDE_SPLIT_CATEGORY";

    protected static final int CATEGORY_ADD = 11;
    protected static final int CATEGORY_PICK = 12;

    @Override
    protected Fragment createFragment() {
        Intent intent = getIntent();
        if (intent != null) {
            return CategorySelectorFragment.newInstance(
                    this,
                    intent.getLongExtra(SELECTED_CATEGORY_ID, 0),
                    intent.getLongExtra(EXCLUDED_SUB_TREE_ID, -1),
                    intent.getBooleanExtra(INCLUDE_SPLIT_CATEGORY, false)
            );
        } else {
            return CategorySelectorFragment.newInstance(this);
        }
    }

    public static Intent pickCategory(Context context, long selectedId, long excludingTreeId, boolean includeSplit) {
        Intent intent = new Intent(context, CategorySelectorActivity.class);
        intent.putExtra(SELECTED_CATEGORY_ID, selectedId);
        intent.putExtra(EXCLUDED_SUB_TREE_ID, excludingTreeId);
        intent.putExtra(INCLUDE_SPLIT_CATEGORY, includeSplit);
        return intent;
    }

    @Override
    public void onCategorySelected(long category_id) {
        Intent data = new Intent();
        data.putExtra(EXTRA_SELECTED_CATEGORY_ID, category_id);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
