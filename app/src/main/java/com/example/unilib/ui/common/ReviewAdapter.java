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
 * <p>A lecturer who shared a document themselves cannot check their own work,
 * so for that row the buttons are replaced by a note saying an admin has to
 * approve it.
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
    /** The id of the signed in reviewer, so their own upload can be marked. */
    private final String reviewerId;
    private final List<Document> items = new ArrayList<>();

    public ReviewAdapter(Context context, String reviewerId, OnReview listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.reviewerId = reviewerId;
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

        boolean ownUpload = reviewerId != null && reviewerId.equals(d.getUploaderId());
        h.approve.setVisibility(ownUpload ? View.GONE : View.VISIBLE);
        h.reject.setVisibility(ownUpload ? View.GONE : View.VISIBLE);
        h.note.setVisibility(ownUpload ? View.VISIBLE : View.GONE);
        if (!ownUpload) {
            h.approve.setOnClickListener(v -> listener.onApprove(d));
            h.reject.setOnClickListener(v -> listener.onReject(d));
        }
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
