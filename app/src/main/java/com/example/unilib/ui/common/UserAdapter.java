package com.example.unilib.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.model.User;

import java.util.ArrayList;
import java.util.List;

/** One row per member, showing the role that member currently holds. */
public class UserAdapter extends BaseAdapter {

    /** Called when a row is tapped. */
    public interface OnUserTap {
        void onTap(User user);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final OnUserTap listener;
    private final List<User> items = new ArrayList<>();

    public UserAdapter(Context context, OnUserTap listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submit(List<User> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public User getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_user, parent, false);
            h = new Holder();
            h.name = convertView.findViewById(R.id.tvName);
            h.email = convertView.findViewById(R.id.tvEmail);
            h.role = convertView.findViewById(R.id.tvRole);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        User u = getItem(position);
        h.name.setText(u.getName() == null ? "" : u.getName());
        h.email.setText(Labels.memberMeta(u));
        h.role.setText(Labels.role(context, u.parsedRole()));
        h.role.setTextColor(context.getColor(Labels.roleColor(context, u.parsedRole())));
        h.name.setAlpha(u.isActive() ? 1f : 0.5f);
        h.email.setAlpha(u.isActive() ? 1f : 0.5f);
        convertView.setOnClickListener(v -> listener.onTap(u));
        return convertView;
    }

    private static class Holder {
        TextView name;
        TextView email;
        TextView role;
    }
}
