package com.example.unilib.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.model.Document;

import java.util.ArrayList;
import java.util.List;

/** One row per document, with the status of that document. */
public class DocumentStatusAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<Document> items = new ArrayList<>();

    public DocumentStatusAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
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
            convertView = inflater.inflate(R.layout.item_document_status, parent, false);
            h = new Holder();
            h.title = convertView.findViewById(R.id.tvTitle);
            h.meta = convertView.findViewById(R.id.tvMeta);
            h.status = convertView.findViewById(R.id.tvStatus);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        Document d = getItem(position);
        h.title.setText(d.getTitle());
        h.meta.setText(Labels.documentMeta(context, d));
        h.status.setText(Labels.status(context, d.getStatus()));
        h.status.setTextColor(context.getColor(Labels.statusColor(context, d.getStatus())));
        h.status.setBackgroundResource(Labels.statusBackground(context, d.getStatus()));
        return convertView;
    }

    private static class Holder {
        TextView title;
        TextView meta;
        TextView status;
    }
}
