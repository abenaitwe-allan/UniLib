package com.example.unilib.ui.lecturer;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.unilib.R;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.Document;
import com.example.unilib.model.Role;
import com.example.unilib.ui.BaseDashboardActivity;
import com.example.unilib.ui.common.ReviewAdapter;

import java.util.List;

/**
 * The lecturer's inbox: the documents that are waiting to be published, with a
 * button to approve or reject each one.
 */
public class LecturerInboxActivity extends BaseDashboardActivity {

    private DocumentRepository repo;

    private ReviewAdapter adapter;
    private ProgressBar progress;

    @Override protected int layoutResource() { return R.layout.activity_lecture_inbox; }

    @Override protected Role requiredRole() { return Role.LECTURER; }

    @Override protected int titleResource() { return R.string.dash_title_lecturer; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Built here, not as a field: the context is not ready until now.
        repo = new DocumentRepository(this);

        ListView list = findViewById(R.id.listPending);
        TextView empty = findViewById(R.id.tvEmpty);
        progress = findViewById(R.id.progress);

        adapter = new ReviewAdapter(this, new ReviewAdapter.OnReview() {
            @Override public void onApprove(Document document) {
                approveDocument(repo, document, LecturerInboxActivity.this::loadPending);
            }
            @Override public void onReject(Document document) {
                rejectDocument(repo, document, LecturerInboxActivity.this::loadPending);
            }
        });
        list.setAdapter(adapter);
        list.setEmptyView(empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPending();
    }

    /** Reads the documents that are still waiting for a decision. */
    private void loadPending() {
        progress.setVisibility(View.VISIBLE);
        repo.listPending(new ResultCallback<List<Document>>() {
            @Override public void onSuccess(List<Document> documents) {
                progress.setVisibility(View.GONE);
                adapter.submit(documents);
            }
            @Override public void onError(String message) {
                progress.setVisibility(View.GONE);
                showMessage(message);
            }
        });
    }
}
