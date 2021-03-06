package ru.orangesoftware.financisto.adapter.async;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.SmsDragListActivity;
import ru.orangesoftware.financisto.activity.SmsTemplateActivity;
import ru.orangesoftware.financisto.databinding.GenericRecyclerItemBinding;
import ru.orangesoftware.financisto.helper.ItemTouchHelperAdapter;
import ru.orangesoftware.financisto.helper.ItemTouchHelperViewHolder;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

import static androidx.recyclerview.widget.ItemTouchHelper.END;
import static androidx.recyclerview.widget.ItemTouchHelper.START;
import static ru.orangesoftware.financisto.activity.SmsDragListActivity.EDIT_REQUEST_CODE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns._id;

/**
 * Based on <a href=https://github.com/jasonwyatt/AsyncListUtil-Example>AsyncListUtil-Example</a> and
 * <a href=https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf>drag-and-swipe-with-recyclerview</a>
 */
public class SmsTemplateListAsyncAdapter extends AsyncAdapter<SmsTemplate, SmsTemplateListAsyncAdapter.LocalViewHolder>
        implements ItemTouchHelperAdapter {
    public static final String TAG = "Financisto." + SmsTemplateListAsyncAdapter.class.getSimpleName();

    static final int MENU_EDIT = Menu.FIRST + 1;
    static final int MENU_DUPLICATE = Menu.FIRST + 2;

    static final int MENU_DELETE = Menu.FIRST + 3;
    private final DatabaseAdapter db;
    private final Context context;
    private final SmsDragListActivity activity;

    public SmsTemplateListAsyncAdapter(int chunkSize,
                                       DatabaseAdapter db,
                                       SmsTemplateListSource itemSource,
                                       RecyclerView recyclerView,
                                       SmsDragListActivity activity) {
        super(chunkSize, itemSource, recyclerView);
        this.context = recyclerView.getContext();
        this.db = db;
        this.activity = activity;
    }

    @Override
    public LocalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        GenericRecyclerItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.generic_recycler_item, parent, false);
        binding.getRoot().setOnClickListener(clickedView -> {
            final PopupMenu popupMenu = new PopupMenu(context, clickedView);
            int i = 0;
            for (MenuItemInfo m : createContextMenus()) {
                if (m.enabled) {
                    popupMenu.getMenu().add(0, m.menuId, i++, m.titleId);
                }
            }
            popupMenu.setOnMenuItemClickListener(item -> onItemAction(item.getItemId(), clickedView));
            popupMenu.show();
        });
        return new LocalViewHolder(binding);
    }

    protected boolean onItemAction(int menuId, View itemView) {
        final Long id = (Long) itemView.getTag(R.id.sms_tpl_id);
        switch (menuId) {
            case MENU_EDIT: {
                editItem(id);
                return true;
            }
            case MENU_DUPLICATE: {
                if (db.duplicateSmsTemplateBelowOriginal(id) > 0) {
                    Toast.makeText(activity, R.string.duplicate_sms_template, Toast.LENGTH_LONG).show();
                    reloadAsyncSource();
                }
                return true;
            }
            case MENU_DELETE: {
                deleteItem(id, -1);
                return true;
            }
        }
        return false;
    }

    protected List<MenuItemInfo> createContextMenus() {
        List<MenuItemInfo> menus = new ArrayList<>(4);
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
        return menus;
    }

    private void editItem(long id) {
        Intent intent = new Intent(activity, SmsTemplateActivity.class);
        intent.putExtra(_id.name(), id);
        activity.startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    private void deleteItem(long id, int position) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.sms_delete_alert)
                .setPositiveButton(R.string.delete, (arg0, arg1) -> new DeleteTask().execute(id))
                .setNegativeButton(R.string.cancel, (arg0, arg1) -> revertSwipeBack())
                .setOnCancelListener(dialog -> revertSwipeBack())
                .show();
    }

    public void revertSwipeBack() {
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(LocalViewHolder holder, int position) {
        final SmsTemplate item = listUtil.getItem(position);
        holder.bindView(item, position);
    }

    @Override
    public boolean onDrag(int fromPosition, int toPosition) {
        Log.d(TAG, String.format("dragged %s item to %s item", fromPosition, toPosition));
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onDrop(int fromPosition, int toPosition) {
        final SmsTemplate itemSrc = listUtil.getItem(fromPosition);
        final SmsTemplate itemTarget = listUtil.getItem(toPosition);
        Log.d(TAG, String.format("dropped %s item to %s item", itemSrc.getId(), itemTarget.getId()));
        new UpdateSortOrderTask().execute(itemSrc.getId(), itemTarget.getId());
    }

    @Override
    public void onItemDismiss(int position, int dir) {
        Log.d(TAG, String.format("swipped %s pos to %s (%s)",
                position, dir == START ? "left" : dir == END ? "right" : "??", dir));

        final long itemId = listUtil.getItem(position).id;
        switch (dir) {
            case START: // left swipe
                editItem(itemId);
                break;
            case END: // right swipe
                deleteItem(itemId, position);
                break;
            default:
                Log.e(TAG, "unknown move: " + dir);
        }
    }

    class LocalViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        public GenericRecyclerItemBinding mBinding;

        public LocalViewHolder(GenericRecyclerItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        public void bindView(SmsTemplate item, Integer ignore) {
            if (item != null) {
                itemView.setTag(R.id.sms_tpl_id, item.getId());
                mBinding.line.setText(item.title);
                mBinding.number.setText(item.template);
                mBinding.amount.setVisibility(View.VISIBLE);
                mBinding.amount.setText(Category.getTitle(item.categoryName, item.categoryLevel));
            }
        }

        @Override
        public void onItemSelected() {
            //numberView.setTextColor(Color.RED);
            Log.i(TAG, String.format("selected: %s", mBinding.number.getText()));
        }

        @Override
        public void onItemClear() {
            //numberView.setTextColor(Color.WHITE);
        }
    }

    class UpdateSortOrderTask extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... ids) {
            return db.moveItemByChangingOrder(SmsTemplate.class, ids[0], ids[1]);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            super.onPostExecute(res);
            Log.d(TAG, "moved finished: " + res);
            if (res) {
                reloadVisibleItems();
            }
        }
    }

    class DeleteTask extends AsyncTask<Long, Void, Integer> {

        @Override
        protected Integer doInBackground(Long... ids) {
            return db.delete(SmsTemplate.class, ids[0]);
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            Log.d(TAG, "deleted: " + res);
            if (res > 0) {
                reloadAsyncSource();
            }
        }
    }
}
