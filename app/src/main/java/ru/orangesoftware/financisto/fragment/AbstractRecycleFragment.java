package ru.orangesoftware.financisto.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.helper.ItemTouchHelperAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.helper.RecyclerTouchListener;
import ru.orangesoftware.financisto.helper.SimpleItemTouchHelperCallback;
import ru.orangesoftware.financisto.utils.PinProtection;

public abstract class AbstractRecycleFragment extends Fragment {

//    protected static final int MENU_VIEW = Menu.FIRST + 1;
//    protected static final int MENU_EDIT = Menu.FIRST + 2;
//    protected static final int MENU_DELETE = Menu.FIRST + 3;
//    protected static final int MENU_ADD = Menu.FIRST + 4;

    private final int mContentId;
    private ViewDataBinding mBinding;
    private RecyclerView mList;
    private View mEmptyView;
    private boolean mListShown;

    private RecyclerView.Adapter mAdapter;
    protected DatabaseAdapter db;
    protected Context context;

    protected boolean enablePin = true;

    protected abstract void updateAdapter();
//    protected abstract void viewItem(View v, int position, long id);
//    protected abstract void editItem(View v, int position, long id);
//    protected abstract void deleteItem(View v, int position, long id);
//    protected abstract void addItem();

    AbstractRecycleFragment(int contentId) {
        mContentId = contentId;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        this.context = context;
        db = new DatabaseAdapter(context);
        db.open();
    }

    @Override
    public void onDetach() {
        db.close();
        context = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (enablePin) PinProtection.unlock(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (enablePin) PinProtection.lock(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = DataBindingUtil.inflate(inflater, mContentId, container, false);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();

//        getListView().setOnItemLongClickListener((parent, v, pos, id) -> {
//            PopupMenu popupMenu = new PopupMenu(context, v);
//            Menu menu = popupMenu.getMenu();
//            List<MenuItemInfo> menus = createContextMenus(id);
//            int i = 0;
//            for (MenuItemInfo m : menus) {
//                if (m.enabled) {
//                    menu.add(0, m.menuId, i++, m.titleId);
//                }
//            }
//            popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), v, pos, id));
//            popupMenu.show();
//            return true;
//        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateAdapter();
    }

    @Override
    public void onDestroyView() {
        mList = null;
        mEmptyView = null;
        mBinding = null;
        super.onDestroyView();
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof RecyclerView) {
            mList = (RecyclerView) root;
        } else {
            mEmptyView = root.findViewById(R.id.list_empty);
            View rawListView = root.findViewById(R.id.list);
            if (!(rawListView instanceof RecyclerView)) {
                if (rawListView == null) {
                    throw new RuntimeException(
                            "Your content must have a RecyclerView whose id attribute is " +
                                    "'R.id.list'");
                }
                throw new RuntimeException(
                        "Content has view with id attribute 'R.id.list' "
                                + "that is not a RecyclerView class");
            }
            mList = (RecyclerView) rawListView;
        }
        mList.setLayoutManager(new LinearLayoutManager(context));
        mListShown = true;
        if (mAdapter != null) {
            RecyclerView.Adapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            setListShown(false, false);
        }
    }

//    protected List<MenuItemInfo> createContextMenus(long id) {
//        List<MenuItemInfo> menus = new LinkedList<>();
//        menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
//        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
//        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
//        return menus;
//    }
//
//    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
//        switch (itemId) {
//            case MENU_VIEW: {
//                viewItem(view, position, id);
//                return true;
//            }
//            case MENU_EDIT: {
//                editItem(view, position, id);
//                return true;
//            }
//            case MENU_DELETE: {
//                deleteItem(view, position, id);
//                return true;
//            }
//        }
//        return false;
//    }

    @Nullable
    public RecyclerView.Adapter getListAdapter() {
        return mAdapter;
    }

    @NonNull
    public final RecyclerView.Adapter requireListAdapter() {
        RecyclerView.Adapter listAdapter = getListAdapter();
        if (listAdapter == null) {
            throw new IllegalStateException("ListFragment " + this
                    + " does not have a ListAdapter.");
        }
        return listAdapter;
    }

    public void setListAdapter(@Nullable RecyclerView.Adapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);

            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(new Test()).setDragVertical(true);
            ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
            mItemTouchHelper.attachToRecyclerView(mList);

            RecyclerTouchListener touchListener = new RecyclerTouchListener(getActivity(), mList)
                    .setClickable(new RecyclerTouchListener.OnRowClickListener() {
                        @Override
                        public void onRowClicked(int position) {
                            Toast.makeText(context, "Clicked #" + position, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onIndependentViewClicked(int independentViewID, int position) {

                        }
                    })
                    .setLongClickable(false, position -> Toast.makeText(context, "Long clicked #" + position, Toast.LENGTH_SHORT).show())
                    .setSwipeOptionViews(R.id.delete_task, R.id.edit_task)
                    .setSwipeable(R.id.rowFG, R.id.rowBG, (viewID, position) -> {
                        switch (viewID){
                            case R.id.delete_task:
                                Toast.makeText(context, "Delete Not Available", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.edit_task:
                                Toast.makeText(context, "Edit Not Available", Toast.LENGTH_SHORT).show();
                                break;

                        }
                    });
            mList.addOnItemTouchListener(touchListener);

            if (!mListShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setListShown(true, requireView().getWindowToken() != null);
            }
        }
    }

    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }

    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            mEmptyView.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        }
    }

    public ViewDataBinding getBinding() {
        return mBinding;
    }

    @NonNull
    public RecyclerView getListView() {
        ensureList();
        return mList;
    }

    private class Test implements ItemTouchHelperAdapter {

        @Override
        public boolean onDrag(int fromPosition, int toPosition) {
            Log.e("Financisto", "onDrag from " + fromPosition + " to " + toPosition);
            mAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onDrop(int fromPosition, int toPosition) {
            Log.e("Financisto", "onDrop from " + fromPosition + " to " + toPosition);
        }

        @Override
        public void onItemDismiss(int position, int i) {
            Log.e("Financisto", "onItemDismiss " + position+ " -> " + i);
        }

    }

}
