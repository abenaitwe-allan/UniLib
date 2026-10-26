package com.example.unilib.ui.upload;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unilib.R;
import com.example.unilib.data.CatalogLoader;
import com.example.unilib.data.DocumentRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.data.Storagerepository;
import com.example.unilib.model.CourseUnit;
import com.example.unilib.model.DocStatus;
import com.example.unilib.model.Document;
import com.example.unilib.model.Program;
import com.example.unilib.network.ApiCallback;
import com.example.unilib.network.ApiResult;
import com.example.unilib.ui.common.ProgramCoursePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Picks a PDF, sends it to storage and records it in the library. The role is
 * checked again by {@link DocumentRepository} before the record is written, so
 * a student cannot sneak a document in.
 */
public class UploadActivity extends AppCompatActivity {

    private Storagerepository storage;
    private DocumentRepository documents;

    private ActivityResultLauncher<String[]> pickPdf;

    private Uri chosenFile;
    private long chosenSize;
    private String chosenName;

    private TextInputLayout tilTitle;
    private TextInputEditText etTitle;
    private Spinner spCourse;
    private ProgramCoursePicker picker;
    private TextView tvFile;
    private Button btnChoose;
    private Button btnUpload;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Built here, not as a field: the context is not ready until now.
        storage = new Storagerepository(this);
        documents = new DocumentRepository(this);

        tilTitle = findViewById(R.id.tilTitle);
        etTitle = findViewById(R.id.etTitle);
        spCourse = findViewById(R.id.spCourse);
        tvFile = findViewById(R.id.tvFile);
        btnChoose = findViewById(R.id.btnChoosePdf);
        btnUpload = findViewById(R.id.btnUpload);
        progress = findViewById(R.id.progress);

        // The program decides which course units are offered.
        picker = new ProgramCoursePicker(findViewById(R.id.spProgram), spCourse,
                new CatalogLoader(this), false);
        picker.load();
        pickPdf = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) onFileChosen(uri);
                }
        );

        btnChoose.setOnClickListener(v -> pickPdf.launch(new String[]{"application/pdf"}));
        btnUpload.setOnClickListener(v -> attemptUpload());
    }

    /** Reads the name and the size of the picked file and shows them. */
    private void onFileChosen(Uri uri) {
        chosenFile = uri;
        chosenName = nameOf(uri);
        chosenSize = sizeOf(uri);
        tvFile.setText(getString(R.string.upload_file_chosen, chosenName,
                chosenSize / 1024));
    }

    private void attemptUpload() {
        tilTitle.setError(null);
        if (chosenFile == null) {
            showMessage(getString(R.string.upload_pick_first));
            return;
        }
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_doc_title_required));
            return;
        }
        int course = spCourse.getSelectedItemPosition();
        Program program = picker.selectedProgram();
        CourseUnit courseUnit = picker.selectedCourseUnit();
        if (program == null) {
            showMessage(getString(R.string.error_program_required));
            return;
        }
        if (courseUnit == null) {
            showMessage(getString(R.string.err_no_course_units));
            return;
        }

        setBusy(true);
        storage.upload(chosenFile, chosenSize, new ApiCallback<Storagerepository.UploadedFile>() {
            @Override
            public void onResult(ApiResult<Storagerepository.UploadedFile> result) {
                runOnUiThread(() -> {
                    if (!result.isSuccess()) {
                        setBusy(false);
                        showMessage(result.message);
                        return;
                    }
                    saveRecord(title, program, courseUnit, result.data);
                });
            }
        });
    }

    /** Writes the document into Firestore. */
    private void saveRecord(String title, Program program, CourseUnit courseUnit,
                            Storagerepository.UploadedFile file) {
        Document draft = new Document();
        draft.setTitle(title);
        draft.setProgramId(program.getId());
        draft.setProgramName(program.getName());
        draft.setCourseCode(courseUnit.getCode());
        draft.setCourseTitle(courseUnit.getTitle());
        draft.setDescription(chosenName);
        draft.setFileUrl(file.publicUrl);
        draft.setFilePath(file.path);
        draft.setFileSize(chosenSize);

        documents.createDocument(draft, new ResultCallback<Document>() {
            @Override public void onSuccess(Document saved) {
                setBusy(false);
                // A lecturer's upload waits for a reviewer, so the message says so.
                String message = saved.getStatus() == DocStatus.PENDING
                        ? getString(R.string.upload_done_pending)
                        : getString(R.string.upload_done);
                Toast.makeText(UploadActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
            @Override public void onError(String message) {
                setBusy(false);
                showMessage(message);
            }
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnUpload.setEnabled(!busy);
        btnChoose.setEnabled(!busy);
    }

    private void showMessage(String message) {
        if (message == null) return;
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private String nameOf(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) return "";
        try {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (index < 0 || !cursor.moveToFirst()) return "";
            String name = cursor.getString(index);
            return name == null ? "" : name;
        } finally {
            cursor.close();
        }
    }

    private long sizeOf(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) return 0;
        try {
            int index = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (index < 0 || !cursor.moveToFirst()) return 0;
            long size = cursor.getLong(index);
            return size < 0 ? 0 : size;
        } finally {
            cursor.close();
        }
    }
}
