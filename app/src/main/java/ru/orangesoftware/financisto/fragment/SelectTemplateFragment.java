package ru.orangesoftware.financisto.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.TemplatesListActivity;
import ru.orangesoftware.financisto.adapter.TemplateRecyclerAdapter;
import ru.orangesoftware.financisto.databinding.TemplatesBinding;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemLongClick;
import ru.orangesoftware.financisto.widget.SearchFilterTextWatcherListener;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.CATEGORY_NAME;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.TEMPLATE_NAME;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.LIKE;
import static ru.orangesoftware.financisto.fragment.MyEntityListFragment.FILTER_DELAY_MILLIS;

public class SelectTemplateFragment extends TemplatesListFragment implements ItemLongClick {

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        internalOnCreateTemplates();

        TemplatesBinding binding = (TemplatesBinding) getBinding();

        binding.bEditTemplates.setOnClickListener(arg0 -> {
            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
            Intent intent = new Intent(context, TemplatesListActivity.class);
            startActivity(intent);
        });
        binding.bCancel.setOnClickListener(arg0 -> {
            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
        });
        multiplierText = binding.multiplier;
        binding.bPlus.setOnClickListener(arg0 -> incrementMultiplier());
        binding.bMinus.setOnClickListener(arg0 -> decrementMultiplier());

        searchFilter = binding.searchFilter;
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getTouchListener().setSwipeable(false);
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null){
            setListAdapter(new TemplateRecyclerAdapter(context, db, getCursor()));
        } else {
            ((TemplateRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        long id = getListAdapter().getItemId(position);
        returnResult(id, false);
    }

    @Override
    public void onItemLongClick(View view, int position) {
        long id = getListAdapter().getItemId(position);
        returnResult(id, true);
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

    void returnResult(long id, boolean edit) {
        Intent intent = new Intent();
        intent.putExtra(TEMPATE_ID, id);
        intent.putExtra(MULTIPLIER, multiplier);
        if (edit) intent.putExtra(EDIT_AFTER_CREATION, true);
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    }
}
