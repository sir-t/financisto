package ru.orangesoftware.financisto.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AttributeListActivity;
import ru.orangesoftware.financisto.activity.CategoryActivity;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class CategoryListFragment extends AbstractListFragment {

    private static final int EMPTY_REQUEST = 0;
    private static final int NEW_CATEGORY_REQUEST = 1;
    private static final int EDIT_CATEGORY_REQUEST = 2;

    private CategoryTree<Category> categories;
    private Map<Long, String> attributes;

    public CategoryListFragment() {
        super(R.layout.category_list);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        categories = db.getCategoriesTree(false);
        attributes = db.getAllAttributesMap();
        ImageButton b = view.findViewById(R.id.bAttributes);
        b.setOnClickListener(v -> {
            Intent intent = new Intent(context, AttributeListActivity.class);
            startActivityForResult(intent, EMPTY_REQUEST);
        });
        b = view.findViewById(R.id.bCollapseAll);
        b.setOnClickListener(v -> ((CategoryListAdapter) adapter).collapseAllCategories());
        b = view.findViewById(R.id.bExpandAll);
        b.setOnClickListener(v -> ((CategoryListAdapter) adapter).expandAllCategories());
        b = view.findViewById(R.id.bSort);
        b.setOnClickListener(v -> sortByTitle());
        b = view.findViewById(R.id.bFix);
        b.setOnClickListener(v -> reIndex());

        return view;
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void recreateCursor() {
        long t0 = System.currentTimeMillis();
        categories = db.getCategoriesTree(false);
        attributes = db.getAllAttributesMap();
        updateAdapter();
        long t1 = System.currentTimeMillis();
        Log.d("CategoryListActivity", "Requery in " + (t1 - t0) + "ms");
    }

    @Override
    protected void updateAdapter() {
        if (adapter == null) {
            adapter = new CategoryListAdapter(context, categories);
            ((CategoryListAdapter) adapter).setAttributes(attributes);
            setListAdapter(adapter);
        } else {
            ((CategoryListAdapter) adapter).setCategories(categories);
            ((CategoryListAdapter) adapter).setAttributes(attributes);
            notifyDataSetChanged();
        }
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(context, CategoryActivity.class);
        startActivityForResult(intent, NEW_CATEGORY_REQUEST);
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        Category c = (Category) getListAdapter().getItem(position);
        new AlertDialog.Builder(context)
                .setTitle(c.getTitle())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.delete_category_dialog)
                .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                    db.deleteCategory(id);
                    recreateCursor();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    protected void editItem(View v, int position, long id) {
        Intent intent = new Intent(context, CategoryActivity.class);
        intent.putExtra(CategoryActivity.CATEGORY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_CATEGORY_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        final Category c = (Category) getListAdapter().getItem(position);
        final ArrayList<PositionAction> actions = new ArrayList<>();
        Category p = c.parent;
        CategoryTree<Category> categories = (p == null ? this.categories : p.children);
        final int pos = categories.indexOf(c);
        if (pos > 0) {
            actions.add(top);
            actions.add(up);
        }
        if (pos < categories.size() - 1) {
            actions.add(down);
            actions.add(bottom);
        }
        if (c.hasChildren()) {
            actions.add(sortByTitle);
        }
        final ListAdapter a = new CategoryPositionListAdapter(actions);
        final CategoryTree<Category> tree = categories;
        new AlertDialog.Builder(context)
                .setTitle(c.getTitle())
                .setAdapter(a, (dialog, which) -> {
                    PositionAction action = actions.get(which);
                    if (action.execute(tree, pos)) {
                        db.updateCategoryTree(tree);
                        notifyDataSetChanged();
                    }
                })
                .show();
    }

    private void sortByTitle() {
        if (categories.sortByTitle()) {
            db.updateCategoryTree(categories);
            recreateCursor();
        }
    }

    private void reIndex() {
        db.restoreSystemEntities();
        recreateCursor();
    }

    protected void notifyDataSetChanged() {
        ((CategoryListAdapter) adapter).notifyDataSetChanged();
    }

    private abstract class PositionAction {
        final int icon;
        final int title;

        public PositionAction(int icon, int title) {
            this.icon = icon;
            this.title = title;
        }

        public abstract boolean execute(CategoryTree<Category> tree, int pos);
    }

    private final PositionAction top = new PositionAction(R.drawable.ic_btn_round_top, R.string.position_move_top) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryToTheTop(pos);
        }
    };

    private final PositionAction up = new PositionAction(R.drawable.ic_btn_round_up, R.string.position_move_up) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryUp(pos);
        }
    };

    private final PositionAction down = new PositionAction(R.drawable.ic_btn_round_down, R.string.position_move_down) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryDown(pos);
        }
    };

    private final PositionAction bottom = new PositionAction(R.drawable.ic_btn_round_bottom, R.string.position_move_bottom) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryToTheBottom(pos);
        }
    };

    private final PositionAction sortByTitle = new PositionAction(R.drawable.ic_btn_round_sort_by_title, R.string.sort_by_title) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.sortByTitle();
        }
    };

    private class CategoryPositionListAdapter extends BaseAdapter {

        private final ArrayList<PositionAction> actions;

        public CategoryPositionListAdapter(ArrayList<PositionAction> actions) {
            this.actions = actions;
        }

        @Override
        public int getCount() {
            return actions.size();
        }

        @Override
        public PositionAction getItem(int position) {
            return actions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return actions.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.position_list_item, parent, false);
            }
            ImageView v = convertView.findViewById(R.id.icon);
            TextView t = convertView.findViewById(R.id.line1);
            PositionAction a = actions.get(position);
            v.setImageResource(a.icon);
            t.setText(a.title);
            return convertView;
        }

    }

    private class CategoryListAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private CategoryTree<Category> categories;
        private Map<Long, String> attributes;

        private final ArrayList<Category> list = new ArrayList<Category>();
        private final HashSet<Long> state = new HashSet<Long>();

        private final Drawable expandedDrawable;
        private final Drawable collapsedDrawable;
        private final int incomeColor;
        private final int expenseColor;

        private final int levelPadding;

        public CategoryListAdapter(Context context, CategoryTree<Category> categories) {
            this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.categories = categories;
            Resources resources = context.getResources();
            this.expandedDrawable = resources.getDrawable(R.drawable.expander_ic_maximized);
            this.collapsedDrawable = resources.getDrawable(R.drawable.expander_ic_minimized);
            this.incomeColor = resources.getColor(R.color.category_type_income);
            this.expenseColor = resources.getColor(R.color.category_type_expense);
            this.levelPadding = resources.getDimensionPixelSize(R.dimen.category_padding);
            recreatePlainList();
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

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Category getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.category_list_item2, parent, false);
                h = Holder.create(convertView);
            } else {
                h = (Holder) convertView.getTag();
            }
            TextView indicator = h.indicator;
            ImageView span = h.span;
            TextView title = h.title;
            TextView label = h.label;
            final Category c = getItem(position);
            title.setText(c.title);
            int padding  = levelPadding*(c.level-1);
            if (c.hasChildren()) {
                span.setImageDrawable(state.contains(c.id) ? expandedDrawable : collapsedDrawable);
                span.setClickable(true);
                span.setOnClickListener(v -> onListItemClick(c.id));
                span.setPadding(padding, 0, 0, 0);
                span.setVisibility(View.VISIBLE);
                padding += collapsedDrawable.getMinimumWidth();
            } else {
                padding += levelPadding/2;
                span.setVisibility(View.GONE);
            }
            title.setPadding(padding, 0, 0, 0);
            label.setPadding(padding, 0, 0, 0);
            long id = c.id;
            if (attributes != null && attributes.containsKey(id)) {
                label.setText(attributes.get(id));
                label.setVisibility(View.VISIBLE);
            } else {
                label.setVisibility(View.GONE);
            }
            if (c.isIncome()) {
                indicator.setBackgroundColor(incomeColor);
            } else if (c.isExpense()) {
                indicator.setBackgroundColor(expenseColor);
            } else {
                indicator.setBackgroundColor(Color.WHITE);
            }
            return convertView;
        }

        public void onListItemClick(long id) {
            if (state.contains(id)) {
                state.remove(id);
            } else {
                state.add(id);
            }
            notifyDataSetChanged();
        }

        public void collapseAllCategories() {
            state.clear();
            notifyDataSetChanged();
        }

        public void expandAllCategories() {
            expandAllCategories(categories);
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

        @Override
        public void notifyDataSetChanged() {
            recreatePlainList();
            super.notifyDataSetChanged();
        }

        public void setCategories(CategoryTree<Category> categories) {
            this.categories = categories;
            recreatePlainList();
        }

        public void setAttributes(Map<Long, String> attributes) {
            this.attributes = attributes;
        }

    }

    private static class Holder {

        public TextView indicator;
        public ImageView span;
        public TextView title;
        public TextView label;

        public static Holder create(View convertView) {
            Holder h = new Holder();
            h.indicator = convertView.findViewById(R.id.indicator);
            h.span = convertView.findViewById(R.id.span);
            h.title = convertView.findViewById(R.id.line1);
            h.label = convertView.findViewById(R.id.label);
            convertView.setTag(h);
            return h;
        }

    }
}
