package ru.orangesoftware.financisto.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    private final int mContentId;
    private ViewDataBinding mBinding;
    private RecyclerView mList;
    private View mEmptyView;
    private boolean mListShown;

    private ItemTouchHelper.Callback mItemTouchHelperCallback;
    private RecyclerTouchListener mTouchListener;

    private Cursor mCursor;
    private RecyclerView.Adapter mAdapter;
    protected DatabaseAdapter db;
    protected Context context;

    private boolean enablePin = true;

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

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCursor = createCursor();
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
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if (!mListShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setListShown(true, requireView().getWindowToken() != null);
            }
        }
    }

    protected Cursor createCursor() {
        return null;
    }

    protected void recreateCursor() {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = createCursor();
        if (mCursor != null) {
            updateAdapter();
        }
    }

    protected Cursor getCursor() {
        return mCursor;
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

    public interface ItemDragAndDrop {
        boolean onDragAndDrop(int fromPosition, int toPosition);
    }

    public interface ItemClick {
        void onItemClick(View view, int position);
    }

    public interface ItemLongClick {
        void onItemLongClick(View view, int position);
    }

}
