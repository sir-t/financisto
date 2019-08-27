package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.CurrencyActivity;
import ru.orangesoftware.financisto.activity.CurrencySelector;
import ru.orangesoftware.financisto.activity.ExchangeRatesListActivity;
import ru.orangesoftware.financisto.adapter.CurrencyListAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

public class CurrencyListFragment extends AbstractListFragment {

    private static final int NEW_CURRENCY_REQUEST = 1;
    private static final int EDIT_CURRENCY_REQUEST = 2;

    private static final int MENU_MAKE_DEFAULT = MENU_ADD + 1;

    public CurrencyListFragment() {
        super(R.layout.currency_list);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ImageButton bRates = view.findViewById(R.id.bRates);
        bRates.setOnClickListener(view -> {
            Intent intent = new Intent(context, ExchangeRatesListActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    protected List<MenuItemInfo> createContextMenus(long id) {
        List<MenuItemInfo> menus = super.createContextMenus(id);
        for (MenuItemInfo m : menus) {
            if (m.menuId == MENU_VIEW) {
                m.enabled = false;
                break;
            }
        }
        menus.add(new MenuItemInfo(MENU_MAKE_DEFAULT, R.string.currency_make_default));
        return menus;
    }

    @Override
    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
        if (super.onPopupItemSelected(itemId, view, position, id)) return true;
        switch (itemId) {
            case MENU_MAKE_DEFAULT: {
                makeCurrencyDefault(id);
                return true;
            }
        }
        return false;
    }

    @Override
    protected Cursor createCursor() {
        return db.getAllCurrencies("name");
    }

    @Override
    protected void updateAdapter() {
        if (adapter == null) {
            adapter = new CurrencyListAdapter(db, context, cursor);
            setListAdapter(adapter);
        } else {
            ((CurrencyListAdapter) adapter).changeCursor(cursor);
            ((CurrencyListAdapter) adapter).notifyDataSetChanged();
        }
    }

    @Override
    protected void addItem() {
        new CurrencySelector(context, db, currencyId -> {
            if (currencyId == 0) {
                Intent intent = new Intent(context, CurrencyActivity.class);
                startActivityForResult(intent, NEW_CURRENCY_REQUEST);
            } else {
                recreateCursor();
            }
        }).show();
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
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

    @Override
    protected void editItem(View v, int position, long id) {
        Intent intent = new Intent(context, CurrencyActivity.class);
        intent.putExtra(CurrencyActivity.CURRENCY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_CURRENCY_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        editItem(v, position, id);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            recreateCursor();
        }
    }

    private void makeCurrencyDefault(long id) {
        Currency c = db.get(Currency.class, id);
        c.isDefault = true;
        db.saveOrUpdate(c);
        recreateCursor();
    }

}
