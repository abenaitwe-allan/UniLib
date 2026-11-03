package com.example.unilib.ui.common;

import android.content.Context;
import android.widget.Toast;

import com.example.unilib.R;
import com.example.unilib.data.OfflineDocumentStore;
import com.example.unilib.model.Document;
import com.example.unilib.network.NetworkUtils;

import java.io.File;

/** Opens a document using the embedded PDF viewer activity. */
public final class DocumentActions {

    private DocumentActions() { }

    /** Opens a document in the in-app PDF viewer. */
    public static void open(Context context, Document document) {
        open(context, document, new OfflineDocumentStore(context));
    }

    /**
     * Opens the in-app PDF viewer for the given document.
     * If the document is saved, it opens instantly offline.
     * If not saved, it streams/caches it temporarily for viewing without saving to offline store.
     */
    public static void open(Context context, Document document, OfflineDocumentStore store) {
        if (document == null) return;

        File local = store.fileFor(document.getId());
        boolean isSaved = store.isSaved(document.getId());

        if (!isSaved && !NetworkUtils.isOnline(context) && (local == null || !local.exists())) {
            Toast.makeText(context, R.string.offline_need_internet, Toast.LENGTH_LONG).show();
            return;
        }

        PdfViewerActivity.start(context, document);
    }
}
