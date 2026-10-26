package com.example.unilib.ui.student;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.unilib.R;
import com.example.unilib.ui.common.Labels;
import com.example.unilib.model.Document;
import java.util.ArrayList;
import java.util.List;

public class DocumentListAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<Document> items = new ArrayList<>();

    public DocumentListAdapter(Context context) {
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
        if (convertView == null) {                       // inflate only when no view can be reused
            convertView = inflater.inflate(R.layout.item_document, parent, false);
            h = new Holder();
            h.title = convertView.findViewById(R.id.tvTitle);
            h.meta = convertView.findViewById(R.id.tvMeta);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        Document d = getItem(position);
        h.title.setText(d.getTitle());
        h.meta.setText(Labels.documentMeta(context, d));
        return convertView;
    }

    private static class Holder {
        TextView title;
        TextView meta;
    }
}