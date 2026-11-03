package com.example.unilib.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
 * Embedded in-app PDF viewer activity with Matrix Pan & Zoom, Pinch-to-Zoom,
 * Double-Tap Zoom, and Toolbar Zoom Controls (+/-).
 */
public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_DOC_ID = "extra_doc_id";
    public static final String EXTRA_DOC_TITLE = "extra_doc_title";
    public static final String EXTRA_DOC_URL = "extra_doc_url";
    public static final String EXTRA_DOC_PROGRAM = "extra_doc_program";
    public static final String EXTRA_DOC_COURSE = "extra_doc_course";

    private MaterialToolbar toolbar;
    private RecyclerView rvPages;
    private ProgressBar progress;
    private TextView tvError;

    private OfflineDocumentStore store;
    private Document document;

    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private PageAdapter adapter;

    private float currentZoomScale = 1.0f;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        store = new OfflineDocumentStore(this);

        toolbar = findViewById(R.id.toolbar);
        rvPages = findViewById(R.id.rvPdfPages);
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

        loadPdf();
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
        if (currentZoomScale < 4.0f) {
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

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            Bitmap bitmap = renderPage(position, holder.itemView.getContext(), globalZoomScale);
            if (bitmap != null) {
                holder.ivPage.setImageBitmap(bitmap);
            }
            holder.tvPageNum.setText(holder.itemView.getContext()
                    .getString(R.string.page_indicator, position + 1, renderer.getPageCount()));

            // Setup Touch Matrix Zoom, Pan & Clamping
            setupZoomAndPan(holder.ivPage, holder.itemView.getContext());
        }

        @SuppressLint("ClickableViewAccessibility")
        private void setupZoomAndPan(ImageView imageView, Context context) {
            imageView.setScaleType(ImageView.ScaleType.MATRIX);
            final Matrix matrix = new Matrix();
            final Matrix savedMatrix = new Matrix();
            final PointF start = new PointF();
            final PointF mid = new PointF();

            final int NONE = 0;
            final int DRAG = 1;
            final int ZOOM = 2;
            final int[] mode = {NONE};
            final float[] oldDist = {1f};
            final float[] currentScale = {1f};

            GestureDetector doubleTapDetector = new GestureDetector(context,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(@NonNull MotionEvent e) {
                            if (currentScale[0] > 1.1f) {
                                currentScale[0] = 1.0f;
                                matrix.reset();
                            } else {
                                currentScale[0] = 2.0f;
                                matrix.setScale(2.0f, 2.0f, e.getX(), e.getY());
                                clampMatrix(matrix, imageView, currentScale[0]);
                            }
                            imageView.setImageMatrix(matrix);
                            return true;
                        }
                    });

            imageView.setOnTouchListener((v, event) -> {
                if (doubleTapDetector.onTouchEvent(event)) {
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(currentScale[0] > 1.05f);
                    }
                    return true;
                }

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode[0] = DRAG;
                        if (currentScale[0] > 1.05f && v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist[0] = spacing(event);
                        if (oldDist[0] > 10f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode[0] = ZOOM;
                            if (v.getParent() != null) {
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (mode[0] == DRAG && currentScale[0] <= 1.05f) {
                            v.performClick();
                        }
                        mode[0] = NONE;
                        if (currentScale[0] <= 1.05f && v.getParent() != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode[0] == DRAG) {
                            if (currentScale[0] > 1.05f) {
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(true);
                                }
                                matrix.set(savedMatrix);
                                matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                                clampMatrix(matrix, imageView, currentScale[0]);
                            } else {
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(false);
                                }
                                return false;
                            }
                        } else if (mode[0] == ZOOM) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(true);
                                }
                                matrix.set(savedMatrix);
                                float scale = newDist / oldDist[0];
                                matrix.postScale(scale, scale, mid.x, mid.y);
                                float[] values = new float[9];
                                matrix.getValues(values);
                                currentScale[0] = values[Matrix.MSCALE_X];
                                clampMatrix(matrix, imageView, currentScale[0]);
                            }
                        }
                        break;
                }

                imageView.setImageMatrix(matrix);
                return currentScale[0] > 1.05f;
            });
        }

        private static void clampMatrix(Matrix matrix, View view, float scale) {
            float[] values = new float[9];
            matrix.getValues(values);

            float transX = values[Matrix.MTRANS_X];
            float transY = values[Matrix.MTRANS_Y];

            int viewWidth = view.getWidth();
            int viewHeight = view.getHeight();

            if (viewWidth <= 0 || viewHeight <= 0) return;

            float scaledWidth = viewWidth * scale;
            float scaledHeight = viewHeight * scale;

            if (scaledWidth > viewWidth) {
                float minX = viewWidth - scaledWidth;
                if (transX < minX) transX = minX;
                if (transX > 0) transX = 0;
            } else {
                transX = (viewWidth - scaledWidth) / 2f;
            }

            if (scaledHeight > viewHeight) {
                float minY = viewHeight - scaledHeight;
                if (transY < minY) transY = minY;
                if (transY > 0) transY = 0;
            } else {
                transY = (viewHeight - scaledHeight) / 2f;
            }

            values[Matrix.MTRANS_X] = transX;
            values[Matrix.MTRANS_Y] = transY;
            matrix.setValues(values);
        }

        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
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
