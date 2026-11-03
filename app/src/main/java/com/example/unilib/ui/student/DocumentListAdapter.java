package com.example.unilib.ui.student;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.model.Document;
import com.example.unilib.ui.common.Labels;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocumentListAdapter extends BaseAdapter {

    /** What the screen does when a row is opened or saved for offline use. */
    public interface Listener {
        void onOpen(Document document);
        void onSaveToggle(Document document);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<Document> items = new ArrayList<>();
    private final Set<String> savedIds = new HashSet<>();

    public DocumentListAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submit(List<Document> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Tells the rows which documents are already on the phone. */
    public void setSavedIds(Set<String> ids) {
        savedIds.clear();
        if (ids != null) savedIds.addAll(ids);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Document getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_document, parent, false);
            h = new Holder();
            h.title = convertView.findViewById(R.id.tvTitle);
            h.meta = convertView.findViewById(R.id.tvMeta);
            h.save = convertView.findViewById(R.id.btnSave);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        Document d = getItem(position);
        h.title.setText(d.getTitle());
        h.meta.setText(Labels.documentMeta(context, d));

        boolean saved = savedIds.contains(d.getId());
        int color = context.getColor(saved ? R.color.unilib_success : R.color.unilib_accent);
        h.save.setText(saved ? R.string.offline_saved : R.string.offline_save);
        h.save.setIconResource(saved ? R.drawable.ic_offline_done : R.drawable.ic_download);
        h.save.setTextColor(color);
        h.save.setIconTint(ColorStateList.valueOf(color));
        h.save.setStrokeColor(ColorStateList.valueOf(color));
        h.save.setOnClickListener(v -> listener.onSaveToggle(d));
        return convertView;
    }

    private static class Holder {
        TextView title;
        TextView meta;
        MaterialButton save;
    }
}
