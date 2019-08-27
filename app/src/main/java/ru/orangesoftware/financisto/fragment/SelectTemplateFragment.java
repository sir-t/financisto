package ru.orangesoftware.financisto.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.TemplatesListActivity;
import ru.orangesoftware.financisto.adapter.TemplateListAdapter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.widget.SearchFilterTextWatcherListener;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.CATEGORY_NAME;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.TEMPLATE_NAME;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.LIKE;
import static ru.orangesoftware.financisto.fragment.MyEntityListFragment.FILTER_DELAY_MILLIS;

public class SelectTemplateFragment extends TemplatesListFragment {

    public static final String TEMPATE_ID = "template_id";
    public static final String MULTIPLIER = "multiplier";
    public static final String EDIT_AFTER_CREATION = "edit_after_creation";

    private TextView multiplierText;
    private EditText searchFilter;
    private int multiplier = 1;

    public SelectTemplateFragment() {
        super(R.layout.templates);
    }

    @Override
    protected void initUI(Bundle savedInstanceState) {
        internalOnCreateTemplates();

        Button b = view.findViewById(R.id.bEditTemplates);
        b.setOnClickListener(arg0 -> {
            context.setResult(RESULT_CANCELED);
            context.finish();
            Intent intent = new Intent(context, TemplatesListActivity.class);
            startActivity(intent);
        });
        b = view.findViewById(R.id.bCancel);
        b.setOnClickListener(arg0 -> {
            context.setResult(RESULT_CANCELED);
            context.finish();
        });
        multiplierText = view.findViewById(R.id.multiplier);
        ImageButton ib = view.findViewById(R.id.bPlus);
        ib.setOnClickListener(arg0 -> incrementMultiplier());
        ib = view.findViewById(R.id.bMinus);
        ib.setOnClickListener(arg0 -> decrementMultiplier());

        searchFilter = view.findViewById(R.id.searchFilter);
        searchFilter.addTextChangedListener(new SearchFilterTextWatcherListener(FILTER_DELAY_MILLIS) {
            @Override
            public void clearFilter(String oldFilter) {
                blotterFilter.remove(TEMPLATE_NAME);
            }

            @Override
            public void applyFilter(String filter) {
                if (!TextUtils.isEmpty(filter)) {
                    filter = "%" + filter.replace(" ", "%") + "%";
                    blotterFilter.put(Criteria.or(
                            new Criteria(TEMPLATE_NAME, LIKE, filter),
                            new Criteria(CATEGORY_NAME, LIKE, filter)));
                }
                recreateCursor();
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setOnItemLongClickListener((parent, v, position, id) -> {
            returnResult(id, true);
            return true;
        });
    }

    @Override
    protected Cursor createCursor() {
        return super.createCursor();
    }

    protected void incrementMultiplier() {
        ++multiplier;
        multiplierText.setText("x" + multiplier);
    }

    protected void decrementMultiplier() {
        --multiplier;
        if (multiplier < 1) {
            multiplier = 1;
        }
        multiplierText.setText("x" + multiplier);
    }

    @Override
    public void registerForContextMenu(View view) {
    }

    @Override
    protected void updateAdapter() {
        if(adapter ==null){
            adapter = new TemplateListAdapter(context, db, cursor);
            setListAdapter(adapter);
        }else {
            ((TemplateListAdapter)adapter).changeCursor(cursor);
            ((TemplateListAdapter)adapter).notifyDataSetChanged();
        }
    }

    @Override
    protected void onItemClick(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
    public void editItem(View v, int position, long id) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        // do nothing
    }

    void returnResult(long id, boolean edit) {
        Intent intent = new Intent();
        intent.putExtra(TEMPATE_ID, id);
        intent.putExtra(MULTIPLIER, multiplier);
        if (edit) intent.putExtra(EDIT_AFTER_CREATION, true);
        context.setResult(RESULT_OK, intent);
        context.finish();
    }
}
