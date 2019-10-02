package ru.orangesoftware.financisto.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.RefreshSupportedActivity;
import ru.orangesoftware.financisto.helper.ItemTouchHelperAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.helper.RecyclerTouchListener;
import ru.orangesoftware.financisto.helper.SimpleItemTouchHelperCallback;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.PinProtection;

public abstract class AbstractRecycleFragment extends Fragment implements RefreshSupportedActivity, BottomNavigationSupported {

    private final int mContentId;
    private ViewDataBinding mBinding;
    private View root;
    private RecyclerView mList;
    private View mEmptyView;

    private ItemTouchHelper.Callback mItemTouchHelperCallback;
    private RecyclerTouchListener mTouchListener;

    private Cursor mCursor;
    private RecyclerView.Adapter mAdapter;
    protected DatabaseAdapter db;
    protected Context context;

    protected boolean enablePin = true;
    protected SelectionMode selectionMode = SelectionMode.OFF;

    protected abstract void updateAdapter();

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
        root = mBinding.getRoot();
        ensureList();

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCursor = createCursor();
        updateAdapter();

        ItemSwipeable swipeable = (AbstractRecycleFragment.this instanceof ItemSwipeable) ? ((ItemSwipeable) AbstractRecycleFragment.this) : null;
        if (swipeable != null) {
            getTouchListener().setSwipeOptionViews(swipeable.getSwipeOptions())
                    .setSwipeable(R.id.rowFG, R.id.rowBG, swipeable::onSwipeClick);
        }
    }

    @Override
    public void onDestroyView() {
        mList = null;
        mEmptyView = null;
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void refreshFragment() {
        getRecyclerView().smoothScrollToPosition(0);
    }

    @Override
    public void willBeDisplayed() {
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        getRecyclerView().startAnimation(fadeIn);
    }

    @Override
    public void willBeHidden() {
        Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        getRecyclerView().startAnimation(fadeOut);
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
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
        if (mAdapter != null) {
            RecyclerView.Adapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            setListShown(false);
        }

        if (AbstractRecycleFragment.this instanceof ItemDragAndDrop) {
            mItemTouchHelperCallback = new SimpleItemTouchHelperCallback(new DragAndDropHelper()).setDragVertical(true);
            ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(mItemTouchHelperCallback);
            mItemTouchHelper.attachToRecyclerView(mList);
        }

        mTouchListener = new RecyclerTouchListener(getActivity(), mList)
                .setClickable(new RecyclerTouchListener.OnRowClickListener() {
                    @Override
                    public void onRowClicked(View view, int position) {
                        ItemClick listener = (AbstractRecycleFragment.this instanceof ItemClick) ? ((ItemClick) AbstractRecycleFragment.this) : null;
                        if (listener != null) {
                            Log.e("Financisto", "onItemClick " + position);
                            listener.onItemClick(view, position);
                        }
                        ItemSelection selection = (AbstractRecycleFragment.this instanceof ItemSelection) ? ((ItemSelection) AbstractRecycleFragment.this) : null;
                        if (selection != null && selectionMode == SelectionMode.DISPLAY || selectionMode == SelectionMode.ALWAYS_ON) {
                            checkItem(position);
                        }
                    }

                    @Override
                    public void onIndependentViewClicked(int independentViewID, int position) {

                    }
                })
                .setLongClickable(false, (view, position) -> {
                    ItemLongClick listener = (AbstractRecycleFragment.this instanceof ItemLongClick) ? ((ItemLongClick) AbstractRecycleFragment.this) : null;
                    if (listener != null) {
                        Log.e("Financisto", "onItemLongClick " + position);
                        listener.onItemLongClick(view, position);
                        return;
                    }
                    ItemMenuShow menuListener = (AbstractRecycleFragment.this instanceof ItemMenuShow) ? ((ItemMenuShow) AbstractRecycleFragment.this) : null;
                    if (menuListener != null) {
                        Log.e("Financisto", "MenuShow" + position);
                        List<MenuItemInfo> menus = menuListener.getMenuContext(view, position);
                        if (menus != null) {
                            MenuBuilder mMenu = new MenuBuilder(context);
                            mMenu.setCallback(new MenuBuilder.Callback() {
                                @Override
                                public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                                    return menuListener.onMenuClick(item.getItemId(), position);
                                }

                                @Override
                                public void onMenuModeChange(MenuBuilder menu) {

                                }
                            });
                            boolean showIcons = false;
                            int i = 0;
                            for (MenuItemInfo m : menus) {
                                if (m.enabled) {
                                    MenuItem item = mMenu.add(0, m.menuId, i++, m.titleId);
                                    if (m.iconId != 0) {
                                        showIcons = true;
                                        item.setIcon(m.iconId);
                                    }
                                }
                            }
                            MenuPopupHelper mPopup = new MenuPopupHelper(context, mMenu, view);
                            mPopup.setForceShowIcon(showIcons);
                            mPopup.show();
                        }
                        return;
                    }
                    ItemSelection selection = (AbstractRecycleFragment.this instanceof ItemSelection) ? ((ItemSelection) AbstractRecycleFragment.this) : null;
                    if (selection != null && selectionMode != SelectionMode.OFF) {
                        checkItem(position);
                    }
                });
        mList.addOnItemTouchListener(mTouchListener);
    }

    @Nullable
    RecyclerView.Adapter getListAdapter() {
        return mAdapter;
    }

    ViewDataBinding getBinding() {
        return mBinding;
    }

    @NonNull
    RecyclerView getRecyclerView() {
        ensureList();
        return mList;
    }

    @NonNull
    RecyclerTouchListener getTouchListener() {
        ensureList();
        return mTouchListener;
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
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            setListShown(true);
        }
    }

    protected Cursor getCursor() {
        return mCursor;
    }

    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void recreateCursor() {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = createCursor();
        if (mCursor != null) {
            updateAdapter();
        }
    }

    @Override
    public void integrityCheck() {

    }

    private void setListShown(boolean shown) {
        ensureList();
        if (shown) {
            mEmptyView.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        }
    }

    protected void checkItem(int position) {
        if (getListAdapter() instanceof AdapterSelection) {
            ((AdapterSelection) getListAdapter()).checkItem(position);
            if (selectionMode != SelectionMode.ALWAYS_ON) {
                updateSelectionMode(((AdapterSelection) getListAdapter()).getCheckedCount() > 0 ? SelectionMode.DISPLAY : SelectionMode.ON);
            }
            updateCount();
        }
    }

    protected void checkAll() {
        if (getListAdapter() instanceof AdapterSelection) {
            ((AdapterSelection) getListAdapter()).checkAll();
            updateCount();
        }
    }

    protected void uncheckAll() {
        if (getListAdapter() instanceof AdapterSelection) {
            ((AdapterSelection) getListAdapter()).uncheckAll();
            if (selectionMode != SelectionMode.ALWAYS_ON) {
                updateSelectionMode(SelectionMode.ON);
            }
            updateCount();
        }
    }

    protected void updateSelectionMode(SelectionMode mode) {
        if (selectionMode == mode)
            return;

        boolean toggleSelection = false;
        boolean isShow = selectionMode == SelectionMode.DISPLAY || selectionMode == SelectionMode.ALWAYS_ON ;

        switch (mode) {
            case OFF:
            case ON:
                if (isShow)
                    toggleSelection = true;
                break;
            case DISPLAY:
            case ALWAYS_ON:
                if (!isShow)
                    toggleSelection = true;
                break;
        }
        if (toggleSelection && AbstractRecycleFragment.this instanceof ItemSelection) {
            ((ItemSelection) AbstractRecycleFragment.this).toggleSelectionMode(isShow);
        }
        selectionMode = mode;
    }

    protected void updateCount() {
        int count = ((AdapterSelection) getListAdapter()).getCheckedCount();
        if (count == 0 && selectionMode != SelectionMode.ALWAYS_ON) {
            updateSelectionMode(SelectionMode.ON);
        }
        if (AbstractRecycleFragment.this instanceof ItemSelection) {
            ((ItemSelection) AbstractRecycleFragment.this).updateCount(count);
        }
    }

    private class DragAndDropHelper implements ItemTouchHelperAdapter {

        @Override
        public boolean onDrag(int fromPosition, int toPosition) {
            mAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onDrop(int fromPosition, int toPosition) {
            ItemDragAndDrop listener = (AbstractRecycleFragment.this instanceof ItemDragAndDrop) ? ((ItemDragAndDrop) AbstractRecycleFragment.this) : null;
            if (listener != null) {
                if (!listener.onDragAndDrop(fromPosition, toPosition)) {
                    mAdapter.notifyItemMoved(toPosition, fromPosition);
                }
            }
        }

        @Override
        public void onItemDismiss(int position, int i) {

        }

    }

    public enum SelectionMode {
        OFF,
        ON,
        DISPLAY,
        ALWAYS_ON;
    }

    public interface ItemDragAndDrop {
        boolean onDragAndDrop(int fromPosition, int toPosition);
    }

    public interface ItemClick {
        void onItemClick(View view, int position);
    }

    public interface ItemLongClick {
        void onItemLongClick(View view, int position);
    }

    public interface ItemSwipeable {
        Integer[] getSwipeOptions();
        void onSwipeClick(int viewID, int position);
    }

    public interface ItemMenuShow {
        List<MenuItemInfo> getMenuContext(View view, int position);
        boolean onMenuClick(int menuID, int position);
    }

    public interface ItemSelection {
        void toggleSelectionMode(boolean hide);
        void updateCount(int count);
    }

    public interface AdapterSelection {
        void checkItem(int position);
        void checkAll();
        void uncheckAll();
        int getCheckedCount();
    }

}
