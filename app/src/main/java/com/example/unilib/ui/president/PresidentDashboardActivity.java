package com.example.unilib.ui.president;

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
import com.example.unilib.ui.common.DocumentStatusAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * The class president's screen: everything this member has shared, and where
 * each upload currently sits in the review.
 */
public class PresidentDashboardActivity extends BaseDashboardActivity {

    private DocumentRepository repo;

    private DocumentStatusAdapter adapter;
    private ListView list;
    private TextView empty;
    private ProgressBar progress;

    @Override protected int layoutResource() {
        return R.layout.activity_president_dashboard_activivty;
    }

    @Override protected Role requiredRole() { return Role.PRESIDENT; }

    @Override protected int titleResource() { return R.string.dash_title_president; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Built here, not as a field: the context is not ready until now.
        repo = new DocumentRepository(this);

        list = findViewById(R.id.listMyUploads);
        empty = findViewById(R.id.tvEmpty);
        progress = findViewById(R.id.progress);

        adapter = new DocumentStatusAdapter(this);
        list.setAdapter(adapter);
        list.setEmptyView(empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUploads();
    }

    /** Reads back what this member has uploaded so the statuses stay current. */
    private void loadUploads() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        progress.setVisibility(View.VISIBLE);
        repo.listByUploader(firebaseUser.getUid(), new ResultCallback<List<Document>>() {
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
