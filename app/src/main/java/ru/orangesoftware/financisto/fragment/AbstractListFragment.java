package ru.orangesoftware.financisto.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.LinkedList;
import java.util.List;

import androidx.fragment.app.ListFragment;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.RefreshSupportedActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.PinProtection;

import static android.app.Activity.RESULT_OK;

public abstract class AbstractListFragment extends ListFragment implements RefreshSupportedActivity {

    protected static final int MENU_VIEW = Menu.FIRST + 1;
    protected static final int MENU_EDIT = Menu.FIRST + 2;
    protected static final int MENU_DELETE = Menu.FIRST + 3;
    protected static final int MENU_ADD = Menu.FIRST + 4;
    private static final int LOADER_ID = 0;

    private final int contentId;

    protected LayoutInflater inflater;
    protected Cursor cursor;
    protected ListAdapter adapter;
    protected DatabaseAdapter db;
    protected ImageButton bAdd;

    protected boolean enablePin = true;

    Activity context;
    View view;

    protected AbstractListFragment(int contentId) {
        this.contentId = contentId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.inflater = inflater;

        this.context = getActivity();

        this.db = new DatabaseAdapter(context);
        db.open();

        this.view = inflater.inflate(contentId, container, false);

        initUI(savedInstanceState);

        cursor = createCursor();

//        getLoaderManager().initLoader(LOADER_ID, null, this);
//        if (cursor != null) {
//            startManagingCursor(cursor);
//        }

        updateAdapter();


        return view;
    }
//
//    @Override
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        createCursor();
//
//        CursorLoader cursorLoader = new CursorLoader(getActivity());
//        cursorLoader.setUri(cursor.ge);
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        recreateAdapter();
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> loader) {
//        recreateAdapter();
//    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemLongClickListener((parent, view2, position, id) -> {
            PopupMenu popupMenu = new PopupMenu(context, view2);
            Menu menu = popupMenu.getMenu();
            List<MenuItemInfo> menus = createContextMenus(id);
            int i = 0;
            for (MenuItemInfo m : menus) {
                if (m.enabled) {
                    menu.add(0, m.menuId, i++, m.titleId);
                }
            }
            popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), view2, position, id));
            popupMenu.show();
            return true;
        });
    }

    protected abstract Cursor createCursor();

    protected abstract void updateAdapter();

    protected void initUI(Bundle savedInstanceState) {
        bAdd = view.findViewById(R.id.bAdd);
        bAdd.setOnClickListener(arg0 -> addItem());
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (enablePin) PinProtection.lock(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (enablePin) PinProtection.unlock(context);
    }

    protected List<MenuItemInfo> createContextMenus(long id) {
        List<MenuItemInfo> menus = new LinkedList<>();
        menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
        return menus;
    }

    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
        switch (itemId) {
            case MENU_VIEW: {
                viewItem(view, position, id);
                return true;
            }
            case MENU_EDIT: {
                editItem(view, position, id);
                return true;
            }
            case MENU_DELETE: {
                deleteItem(view, position, id);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        onItemClick(v, position, id);
    }

    protected void onItemClick(View v, int position, long id) {
        viewItem(v, position, id);
    }

    protected void addItem() {
    }

    protected abstract void deleteItem(View v, int position, long id);

    protected abstract void editItem(View v, int position, long id);

    protected abstract void viewItem(View v, int position, long id);

    public void recreateCursor() {
        Log.i("AbstractListActivity", "Recreating cursor");
        Parcelable state = getListView().onSaveInstanceState();
        try {
            if (cursor != null) {
                cursor.close();
            }
            cursor = createCursor();
            if (cursor != null) {
                updateAdapter();
            }
        } finally {
            getListView().onRestoreInstanceState(state);
        }
    }

    @Override
    public void integrityCheck() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            recreateCursor();
        }
    }
}
