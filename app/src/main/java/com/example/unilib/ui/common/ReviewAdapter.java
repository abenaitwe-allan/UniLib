package com.example.unilib.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.model.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * One row per document waiting for a review, with approve and reject buttons.
 *
 * <p>Both lecturers and admins can approve or reject every pending document.
 */
public class ReviewAdapter extends BaseAdapter {

    /** What the screen does when a reviewer taps a button. */
    public interface OnReview {
        void onApprove(Document document);
        void onReject(Document document);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final OnReview listener;
    private final List<Document> items = new ArrayList<>();

    public ReviewAdapter(Context context, OnReview listener) {
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
            convertView = inflater.inflate(R.layout.item_review, parent, false);
            h = new Holder();
            h.title = convertView.findViewById(R.id.tvTitle);
            h.meta = convertView.findViewById(R.id.tvMeta);
            h.approve = convertView.findViewById(R.id.btnApprove);
            h.reject = convertView.findViewById(R.id.btnReject);
            h.note = convertView.findViewById(R.id.tvOwnUpload);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        Document d = getItem(position);
        h.title.setText(d.getTitle());
        h.meta.setText(Labels.reviewMeta(context, d));

        h.note.setText(R.string.status_pending);
        h.note.setBackgroundResource(R.drawable.bg_status_pending);
        h.note.setTextColor(context.getColor(R.color.unilib_pending));
        h.note.setVisibility(View.VISIBLE);
        h.approve.setVisibility(View.VISIBLE);
        h.reject.setVisibility(View.VISIBLE);
        h.approve.setOnClickListener(v -> listener.onApprove(d));
        h.reject.setOnClickListener(v -> listener.onReject(d));
        return convertView;
    }

    private static class Holder {
        TextView title;
        TextView meta;
        Button approve;
        Button reject;
        TextView note;
    }
}
