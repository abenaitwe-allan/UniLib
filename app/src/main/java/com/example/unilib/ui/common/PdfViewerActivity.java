package com.example.unilib.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unilib.R;
import com.example.unilib.data.OfflineDocumentStore;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.Document;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;

/**
 * Embedded in-app PDF viewer activity with synchronized document-wide zoom scaling
 * and high-resolution crisp rendering across all pages.
 */
public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_DOC_ID = "extra_doc_id";
    public static final String EXTRA_DOC_TITLE = "extra_doc_title";
    public static final String EXTRA_DOC_URL = "extra_doc_url";
    public static final String EXTRA_DOC_PROGRAM = "extra_doc_program";
    public static final String EXTRA_DOC_COURSE = "extra_doc_course";

    private MaterialToolbar toolbar;
    private RecyclerView rvPages;
    private View hsvContainer;
    private ProgressBar progress;
    private TextView tvError;

    private OfflineDocumentStore store;
    private Document document;

    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private PageAdapter adapter;

    private float currentZoomScale = 1.0f;
    private ScaleGestureDetector pinchDetector;

    public static void start(Context context, Document doc) {
        if (doc == null) return;
        Intent intent = new Intent(context, PdfViewerActivity.class);
        intent.putExtra(EXTRA_DOC_ID, doc.getId());
        intent.putExtra(EXTRA_DOC_TITLE, doc.getTitle());
        intent.putExtra(EXTRA_DOC_URL, doc.getFileUrl());
        intent.putExtra(EXTRA_DOC_PROGRAM, doc.getProgramName());
        intent.putExtra(EXTRA_DOC_COURSE, doc.getCourseCode());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        store = new OfflineDocumentStore(this);

        toolbar = findViewById(R.id.toolbar);
        rvPages = findViewById(R.id.rvPdfPages);
        hsvContainer = findViewById(R.id.hsvContainer);
        progress = findViewById(R.id.progressLoading);
        tvError = findViewById(R.id.tvError);

        rvPages.setLayoutManager(new LinearLayoutManager(this));

        reconstructDocument();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(document.getTitle() == null ? "" : document.getTitle());
            String sub = Labels.programAndCourse(document);
            if (!sub.isEmpty()) getSupportActionBar().setSubtitle(sub);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        setupPinchZoom();

        loadPdf();
    }

    private void setupPinchZoom() {
        pinchDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float accumulatedFactor = 1.0f;

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                accumulatedFactor = currentZoomScale;
                return true;
            }

            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                accumulatedFactor *= detector.getScaleFactor();
                accumulatedFactor = Math.max(1.0f, Math.min(accumulatedFactor, 3.5f));
                if (Math.abs(accumulatedFactor - currentZoomScale) > 0.15f) {
                    currentZoomScale = accumulatedFactor;
                    if (adapter != null) adapter.applyZoomToAll(currentZoomScale);
                }
                return true;
            }
        });

        if (hsvContainer != null) {
            hsvContainer.setOnTouchListener((v, event) -> {
                pinchDetector.onTouchEvent(event);
                return false;
            });
        }
    }

    private void reconstructDocument() {
        document = new Document();
        document.setId(getIntent().getStringExtra(EXTRA_DOC_ID));
        document.setTitle(getIntent().getStringExtra(EXTRA_DOC_TITLE));
        document.setFileUrl(getIntent().getStringExtra(EXTRA_DOC_URL));
        document.setProgramName(getIntent().getStringExtra(EXTRA_DOC_PROGRAM));
        document.setCourseCode(getIntent().getStringExtra(EXTRA_DOC_COURSE));
    }

    private void loadPdf() {
        File localFile = store.fileFor(document.getId());
        if (localFile != null && localFile.exists()) {
            renderPdfFile(localFile);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        store.downloadToTempCache(document, new ResultCallback<File>() {
            @Override
            public void onSuccess(File file) {
                progress.setVisibility(View.GONE);
                renderPdfFile(file);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(message == null ? getString(R.string.open_failed) : message);
            }
        });
    }

    private void renderPdfFile(File file) {
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pfd);
            adapter = new PageAdapter(pdfRenderer);
            rvPages.setAdapter(adapter);
            tvError.setVisibility(View.GONE);
        } catch (IOException | SecurityException e) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(R.string.open_failed);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save_offline);
        if (saveItem != null) {
            boolean isSaved = store.isSaved(document.getId());
            saveItem.setTitle(isSaved ? R.string.offline_saved : R.string.offline_save);
            saveItem.setIcon(isSaved ? R.drawable.ic_offline_done : R.drawable.ic_download);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save_offline) {
            toggleSaveOffline();
            return true;
        } else if (id == R.id.action_zoom_in) {
            zoomIn();
            return true;
        } else if (id == R.id.action_zoom_out) {
            zoomOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void zoomIn() {
        if (currentZoomScale < 3.5f) {
            currentZoomScale += 0.5f;
            if (adapter != null) adapter.applyZoomToAll(currentZoomScale);
            Toast.makeText(this, "Zoom: " + Math.round(currentZoomScale * 100) + "%", Toast.LENGTH_SHORT).show();
        }
    }

    private void zoomOut() {
        if (currentZoomScale > 1.0f) {
            currentZoomScale -= 0.5f;
            if (currentZoomScale < 1.0f) currentZoomScale = 1.0f;
            if (adapter != null) adapter.applyZoomToAll(currentZoomScale);
            Toast.makeText(this, "Zoom: " + Math.round(currentZoomScale * 100) + "%", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSaveOffline() {
        if (store.isSaved(document.getId())) {
            store.remove(document.getId());
            Toast.makeText(this, getString(R.string.offline_removed_toast, document.getTitle()),
                    Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
            return;
        }

        Toast.makeText(this, getString(R.string.offline_downloading, document.getTitle()),
                Toast.LENGTH_SHORT).show();
        store.download(document, new ResultCallback<File>() {
            @Override
            public void onSuccess(File file) {
                Toast.makeText(PdfViewerActivity.this,
                        getString(R.string.offline_saved_toast, document.getTitle()),
                        Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PdfViewerActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (pfd != null) pfd.close();
        } catch (IOException ignored) { }
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageHolder> {
        private final PdfRenderer renderer;
        private float globalZoomScale = 1.0f;

        PageAdapter(PdfRenderer renderer) {
            this.renderer = renderer;
        }

        @SuppressLint("NotifyDataSetChanged")
        public void applyZoomToAll(float scale) {
            this.globalZoomScale = scale;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pdf_page, parent, false);
            return new PageHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            Bitmap bitmap = renderPage(position, holder.itemView.getContext(), globalZoomScale);
            if (bitmap != null) {
                holder.ivPage.setImageBitmap(bitmap);
                ViewGroup.LayoutParams lp = holder.ivPage.getLayoutParams();
                if (lp != null) {
                    lp.width = bitmap.getWidth();
                    lp.height = bitmap.getHeight();
                    holder.ivPage.setLayoutParams(lp);
                }
            }
            holder.tvPageNum.setText(holder.itemView.getContext()
                    .getString(R.string.page_indicator, position + 1, renderer.getPageCount()));
        }

        private Bitmap renderPage(int index, Context context, float zoom) {
            try (PdfRenderer.Page page = renderer.openPage(index)) {
                int density = context.getResources().getDisplayMetrics().densityDpi;
                float widthPx = (density * (float) page.getWidth()) / 72f;
                float heightPx = (density * (float) page.getHeight()) / 72f;

                int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
                if (screenWidth > 0 && widthPx > 0) {
                    heightPx = (heightPx * screenWidth) / widthPx;
                    widthPx = screenWidth;
                }

                int renderWidth = Math.max(1, (int) (widthPx * Math.max(1.0f, zoom)));
                int renderHeight = Math.max(1, (int) (heightPx * Math.max(1.0f, zoom)));

                Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public int getItemCount() {
            return renderer == null ? 0 : renderer.getPageCount();
        }

        static class PageHolder extends RecyclerView.ViewHolder {
            final ImageView ivPage;
            final TextView tvPageNum;

            PageHolder(View itemView) {
                super(itemView);
                ivPage = itemView.findViewById(R.id.ivPage);
                tvPageNum = itemView.findViewById(R.id.tvPageNumber);
            }
        }
    }
}
