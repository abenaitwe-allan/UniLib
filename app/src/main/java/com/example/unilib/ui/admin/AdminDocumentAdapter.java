package com.example.unilib.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.model.Document;
import com.example.unilib.ui.common.Labels;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Admin viewing documents by program with a Delete action.
 */
public class AdminDocumentAdapter extends BaseAdapter {

    public interface OnDeleteListener {
        void onDelete(Document document);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final OnDeleteListener listener;
    private final List<Document> items = new ArrayList<>();

    public AdminDocumentAdapter(Context context, OnDeleteListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submit(List<Document> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Document getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_admin_document, parent, false);
            h = new Holder();
            h.title = convertView.findViewById(R.id.tvTitle);
            h.meta = convertView.findViewById(R.id.tvMeta);
            h.delete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        Document d = getItem(position);
        h.title.setText(d.getTitle());
        h.meta.setText(Labels.documentMeta(context, d));
        h.delete.setOnClickListener(v -> listener.onDelete(d));
        return convertView;
    }

    private static class Holder {
        TextView title;
        TextView meta;
        Button delete;
    }
}
