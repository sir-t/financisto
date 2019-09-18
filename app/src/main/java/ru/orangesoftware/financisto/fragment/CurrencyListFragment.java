package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.CurrencyActivity;
import ru.orangesoftware.financisto.activity.CurrencySelector;
import ru.orangesoftware.financisto.activity.ExchangeRatesListActivity;
import ru.orangesoftware.financisto.adapter.BaseCursorRecyclerAdapter;
import ru.orangesoftware.financisto.databinding.CurrencyListBinding;
import ru.orangesoftware.financisto.databinding.GenericRecyclerItemBinding;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemClick;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemMenuShow;
import ru.orangesoftware.financisto.fragment.AbstractRecycleFragment.ItemSwipeable;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.EntityManager;

public class CurrencyListFragment extends AbstractRecycleFragment implements ItemClick, ItemSwipeable, ItemMenuShow {

    private static final int NEW_CURRENCY_REQUEST = 1;
    private static final int EDIT_CURRENCY_REQUEST = 2;

    private static final int MENU_EDIT = Menu.FIRST + 1;
    private static final int MENU_DELETE = Menu.FIRST + 2;
    private static final int MENU_MAKE_DEFAULT = Menu.FIRST + 3;

    public CurrencyListFragment() {
        super(R.layout.currency_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getRecyclerView().addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        getRecyclerView().setItemAnimator(new DefaultItemAnimator());

        CurrencyListBinding binding = (CurrencyListBinding) getBinding();
        binding.bAdd.setOnClickListener(v -> {
            new CurrencySelector(context, db, currencyId -> {
                if (currencyId == 0) {
                    Intent intent = new Intent(context, CurrencyActivity.class);
                    startActivityForResult(intent, NEW_CURRENCY_REQUEST);
                } else {
                    recreateCursor();
                }
            }).show();
        });
        binding.bRates.setOnClickListener(v -> {
            Intent intent = new Intent(context, ExchangeRatesListActivity.class);
            startActivity(intent);
        });

    }

    @Override
    protected Cursor createCursor() {
        return db.getAllCurrencies("name");
    }

    @Override
    protected void updateAdapter() {
        if (getListAdapter() == null) {
            setListAdapter(new CurrencyRecyclerAdapter(getCursor()));
        } else {
            ((CurrencyRecyclerAdapter) getListAdapter()).swapCursor(getCursor());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            recreateCursor();
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        long id = getListAdapter().getItemId(position);
        editCurrency(id);
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
                deleteCurrency(id);
                break;
            case R.id.edit_task:
                editCurrency(id);
                break;
        }
    }

    @Override
    public List<MenuItemInfo> getMenuContext(View view, int position) {
        List<MenuItemInfo> menus = new LinkedList<>();
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
        menus.add(new MenuItemInfo(MENU_MAKE_DEFAULT, R.string.currency_make_default));
        return menus;
    }

    @Override
    public boolean onMenuClick(int menuID, int position) {
        long id = getListAdapter().getItemId(position);
        switch (menuID) {
            case MENU_EDIT: {
                editCurrency(id);
                return true;
            }
            case MENU_DELETE: {
                deleteCurrency(id);
                return true;
            }
            case MENU_MAKE_DEFAULT: {
                makeCurrencyDefault(id);
                return true;
            }
        }
        return false;
    }

    private void editCurrency(long id) {
        Intent intent = new Intent(context, CurrencyActivity.class);
        intent.putExtra(CurrencyActivity.CURRENCY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_CURRENCY_REQUEST);
    }

    private void deleteCurrency(long id) {
        if (db.deleteCurrency(id) == 1) {
            recreateCursor();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete)
                    .setMessage(R.string.currency_delete_alert)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNeutralButton(R.string.ok, null).show();
        }
    }

    private void makeCurrencyDefault(long id) {
        Currency c = db.get(Currency.class, id);
        c.isDefault = true;
        db.saveOrUpdate(c);
        recreateCursor();
    }

    public class CurrencyItemHolder extends RecyclerView.ViewHolder {

        private final GenericRecyclerItemBinding mBinding;

        CurrencyItemHolder(GenericRecyclerItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(Currency c) {
            mBinding.line.setText(c.title);
            mBinding.number.setText(c.name);
            mBinding.amount.setText(Utils.amountToString(c, 100000));
            if (c.isDefault) {
                mBinding.icon.setImageResource(R.drawable.ic_home_currency);
            } else {
                mBinding.icon.setImageDrawable(null);
            }

        }

    }

    public class CurrencyRecyclerAdapter extends BaseCursorRecyclerAdapter<CurrencyItemHolder> {

        CurrencyRecyclerAdapter(Cursor c) {
            super(c);
        }

        @NonNull
        @Override
        public CurrencyItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            GenericRecyclerItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.generic_recycler_item, parent, false);
            return new CurrencyItemHolder(binding);
        }

        @Override
        public void onBindViewHolder(CurrencyItemHolder holder, Cursor cursor) {
            Currency c = EntityManager.loadFromCursor(cursor, Currency.class);
            holder.bind(c);
        }

    }

}
