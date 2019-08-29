/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.orangesoftware.financisto.helper;

import android.graphics.Canvas;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * usage
 *
 Adapter implements ItemTouchHelperAdapter {

     public RecyclerListAdapter(RecyclerView recyclerView) {
         UtilDrag.attach(this,recyclerView);
      }

    @Override
    public boolean onDrag(int fromPosition, int toPosition) {
        Collections.swap(mItems, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    //view dropped,need commit data
    @Override
    public void onDrop(int fromPosition, int toPosition) {

    }
    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {

        // Start a drag whenever the handle view it touched
        holder.handleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                UtilDrag.doTouch(RecyclerListAdapter.this,viewHolder, event);
                return false;
            }
        });
    }
 }
 *
 *
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the <code>RecyclerView.Adapter</code> to listen for {@link
 * ItemTouchHelperAdapter} callbacks and the <code>RecyclerView.ViewHolder</code> to implement
 * {@link ItemTouchHelperViewHolder}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private static final float ALPHA_FULL = 1.0f;
    private final ItemTouchHelperAdapter mAdapter;
    private boolean canDragOrNot = true;
    private boolean canDragHorizontal = false, canDragVertical = false, canSwipeDismiss = false;
    private int dragFrom = -1;
    private int dragTo = -1;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
    }

    public SimpleItemTouchHelperCallback setDragHorizontal(boolean val) {
        this.canDragHorizontal = val;
        return this;
    }

    public SimpleItemTouchHelperCallback setDragVertical(boolean val) {
        this.canDragVertical = val;
        return this;
    }

    public SimpleItemTouchHelperCallback setEnableSwipe(boolean val) {
        this.canSwipeDismiss = val;
        return this;
    }

    public SimpleItemTouchHelperCallback setCanDragOrNot(boolean val) {
        this.canDragOrNot = val;
        return this;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Set movement flags based on the layout manager
        int dragFlags = 0;
        int swipeFlags = 0;
        if (canDragOrNot) {
            if (canDragHorizontal)
                dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            if (canDragVertical)
                dragFlags |= ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            if (canSwipeDismiss)
                swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) {
            return false;
        }

        int fromPosition = source.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        if(dragFrom == -1) {
            dragFrom =  fromPosition;
        }
        dragTo = toPosition;
        // Notify the adapter of the move
        mAdapter.onDrag(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        // Notify the adapter of the dismissal
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition(), i);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Fade out the view as it is swiped out of the parent's bounds
            final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof ItemTouchHelperViewHolder) {
                // Let the view holder know that this item is being moved or dragged
                ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(ALPHA_FULL);

        if (viewHolder instanceof ItemTouchHelperViewHolder) {
            // Tell the view holder it's time to restore the idle state
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }

        if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
            mAdapter.onDrop(dragFrom, dragTo);
        }
        dragFrom = dragTo = -1;
    }
}
