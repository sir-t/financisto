package ru.orangesoftware.financisto.fragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterRecyclerAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class TemplatesListFragment extends BlotterFragment {

    public TemplatesListFragment() {
        super(R.layout.blotter);
    }

    @SuppressLint("ValidFragment")
    public TemplatesListFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // remove filter button and totals
        bFilter.setVisibility(View.GONE);
        if (showAllBlotterButtons) {
            bTemplate.setVisibility(View.GONE);
        }
        view.findViewById(R.id.total).setVisibility(View.GONE);
        internalOnCreateTemplates();
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null){
            setListAdapter(new BlotterRecyclerAdapter(context, db, getCursor()) {
                @Override
                protected boolean isShowRunningBalance() {
                    return false;
                }
            });
        } else {
            ((BlotterRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
        }
    }

    @Override
    protected Cursor createCursor() {
        String sortOrder;

        switch (MyPreferences.getTemplatesSortOrder(context)) {
            case NAME:
                sortOrder = BlotterFilter.SORT_BY_TEMPLATE_NAME;
                break;

            case ACCOUNT:
                sortOrder = BlotterFilter.SORY_BY_ACCOUNT_NAME;
                break;

            default:
                sortOrder = BlotterFilter.SORT_NEWER_TO_OLDER;
                break;
        }

        return db.getAllTemplates(blotterFilter, sortOrder);
    }

    @Override
    protected boolean addTemplateToAddButton() {
        return false;
    }

    @Override
    protected void calculateTotals() {
        // do nothing
    }

    protected void internalOnCreateTemplates() {
        // change empty list message
        ((TextView) getView().findViewById(R.id.list_empty)).setText(R.string.no_templates);
        // fix filter
        blotterFilter = new WhereFilter("templates");
        blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(1));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
    }

}
