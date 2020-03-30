package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Action;

public class BottomListAdapter<T extends Action> extends BaseAdapter {

    private Context mContext;
    private final T[] values;

    public BottomListAdapter(Context context,  T[] values){
        mContext = context;
        this.values = values;
    }

    public int getCount() {
        return values.length;
    }

    public Object getItem(int arg0) {
        return null;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = BottomListAdapter.EntityEnumViewHolder.create(mContext, convertView, parent);
        BottomListAdapter.EntityEnumViewHolder holder = (BottomListAdapter.EntityEnumViewHolder) view.getTag();
        T v = values[position];
        holder.icon.setImageResource(v.iconId);
        holder.title.setText(v.titleId);
        return view;
    }

    private static final class EntityEnumViewHolder {

        public final ImageView icon;
        public final TextView title;

        private EntityEnumViewHolder(ImageView icon, TextView title) {
            this.icon = icon;
            this.title = title;
        }

        private static View create(Context context, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.account_bottom_sheet_list_item, parent, false);
                view.setTag(create(view));
                return view;
            } else {
                return convertView;
            }
        }

        private static BottomListAdapter.EntityEnumViewHolder create(View convertView) {
            ImageView icon = convertView.findViewById(R.id.account_action_image);
            TextView title = convertView.findViewById(R.id.account_action_item);
            BottomListAdapter.EntityEnumViewHolder holder = new BottomListAdapter.EntityEnumViewHolder(icon, title);
            convertView.setTag(holder);
            return holder;
        }
    }
}