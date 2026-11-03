package com.example.unilib.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.unilib.R;
import com.example.unilib.model.DocStatus;
import com.example.unilib.model.Document;
import com.example.unilib.network.HttpClientProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Keeps PDFs on the phone so a student can read them without a connection.
 *
 * <p>Each signed in student has their own list. The PDF itself lives in the
 * app's private files folder, and the details of the document are kept in
 * SharedPreferences so the dashboard can draw the list without the network.
 */
public class OfflineDocumentStore {

    private static final String PREFS = "unilib_offline_documents";
    private static final String DIR = "offline_docs";

    private final Context ctx;
    private final OkHttpClient client = HttpClientProvider.get();
    private final Handler main = new Handler(Looper.getMainLooper());

    public OfflineDocumentStore(Context context) {
        this.ctx = context.getApplicationContext();
    }

    private String prefsKey() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return "docs_" + (user == null ? "guest" : user.getUid());
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The saved documents of the signed in student. */
    public List<Document> list() {
        List<Document> result = new ArrayList<>();
        JSONArray array = readArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            Document d = fromJson(o);
            if (d != null && fileFor(d.getId()).exists()) result.add(d);
        }
        return result;
    }

    /** The ids that are saved on this phone. */
    public Set<String> savedIds() {
        Set<String> ids = new HashSet<>();
        JSONArray array = readArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", "");
            if (!id.isEmpty() && fileFor(id).exists()) ids.add(id);
        }
        return ids;
    }

    public boolean isSaved(String documentId) {
        return documentId != null && fileFor(documentId).exists() && findEntry(documentId) != null;
    }

    /** The local PDF for a document, whether or not it has been saved yet. */
    public File fileFor(String documentId) {
        return new File(offlineDir(), safeName(documentId) + ".pdf");
    }

    /**
     * Downloads the PDF and remembers it. The callback runs on the main thread.
     */
    public void download(Document document, ResultCallback<File> callback) {
        String url = document == null ? null : document.getFileUrl();
        if (url == null || url.trim().isEmpty()) {
            callback.onError(ctx.getString(R.string.offline_failed));
            return;
        }
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fail(callback);
            }

            @Override public void onResponse(Call call, Response response) {
                File target = fileFor(document.getId());
                File temp = new File(offlineDir(), safeName(document.getId()) + ".part");
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        fail(callback);
                        return;
                    }
                    try (InputStream in = r.body().byteStream();
                         FileOutputStream out = new FileOutputStream(temp)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                    if (target.exists() && !target.delete()) {
                        fail(callback);
                        return;
                    }
                    if (!temp.renameTo(target)) {
                        fail(callback);
                        return;
                    }
                    putEntry(document);
                    main.post(() -> callback.onSuccess(target));
                } catch (IOException | JSONException e) {
                    if (temp.exists()) temp.delete();
                    fail(callback);
                }
            }
        });
    }

    /**
     * Downloads the PDF for viewing without saving it to the permanent offline store.
     */
    public void downloadToTempCache(Document document, ResultCallback<File> callback) {
        String url = document == null ? null : document.getFileUrl();
        if (url == null || url.trim().isEmpty()) {
            callback.onError(ctx.getString(R.string.offline_failed));
            return;
        }
        File tempDir = ctx.getCacheDir();
        File target = new File(tempDir, "temp_view_" + safeName(document.getId()) + ".pdf");
        if (target.exists()) {
            callback.onSuccess(target);
            return;
        }
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fail(callback);
            }

            @Override public void onResponse(Call call, Response response) {
                File tempPart = new File(tempDir, "temp_view_" + safeName(document.getId()) + ".part");
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        fail(callback);
                        return;
                    }
                    try (InputStream in = r.body().byteStream();
                         FileOutputStream out = new FileOutputStream(tempPart)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                    if (target.exists()) target.delete();
                    if (!tempPart.renameTo(target)) {
                        fail(callback);
                        return;
                    }
                    main.post(() -> callback.onSuccess(target));
                } catch (IOException e) {
                    if (tempPart.exists()) tempPart.delete();
                    fail(callback);
                }
            }
        });
    }

    /** Deletes the saved PDF and its details. */
    public void remove(String documentId) {
        File file = fileFor(documentId);
        if (file.exists()) file.delete();
        JSONArray array = readArray();
        JSONArray kept = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            if (!documentId.equals(o.optString("id", ""))) kept.put(o);
        }
        prefs().edit().putString(prefsKey(), kept.toString()).apply();
    }

    // ---------- private helpers ----------

    private void fail(ResultCallback<File> callback) {
        main.post(() -> callback.onError(ctx.getString(R.string.offline_failed)));
    }

    private File offlineDir() {
        File dir = new File(ctx.getFilesDir(), DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private String safeName(String id) {
        if (id == null || id.isEmpty()) return "document";
        return id.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private JSONArray readArray() {
        SharedPreferences p = prefs();
        String key = prefsKey();
        String raw = p.getString(key, "[]");
        if (raw.equals("[]") && !key.equals("docs_guest")) {
            raw = p.getString("docs_guest", "[]");
        }
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private JSONObject findEntry(String documentId) {
        JSONArray array = readArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o != null && documentId.equals(o.optString("id", ""))) return o;
        }
        return null;
    }

    private void putEntry(Document document) throws JSONException {
        JSONObject entry = toJson(document);
        JSONArray array = readArray();
        JSONArray updated = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            if (!entry.getString("id").equals(o.optString("id", ""))) updated.put(o);
        }
        updated.put(entry);
        String jsonStr = updated.toString();
        prefs().edit().putString(prefsKey(), jsonStr)
                .putString("docs_guest", jsonStr)
                .apply();
    }

    private JSONObject toJson(Document d) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", nullToEmpty(d.getId()));
        o.put("title", nullToEmpty(d.getTitle()));
        o.put("programId", nullToEmpty(d.getProgramId()));
        o.put("programName", nullToEmpty(d.getProgramName()));
        o.put("courseCode", nullToEmpty(d.getCourseCode()));
        o.put("courseTitle", nullToEmpty(d.getCourseTitle()));
        o.put("description", nullToEmpty(d.getDescription()));
        o.put("fileUrl", nullToEmpty(d.getFileUrl()));
        o.put("filePath", nullToEmpty(d.getFilePath()));
        o.put("fileSize", d.getFileSize());
        o.put("uploaderName", nullToEmpty(d.getUploaderName()));
        return o;
    }

    private Document fromJson(JSONObject o) {
        String id = o.optString("id", "");
        if (id.isEmpty()) return null;
        Document d = new Document();
        d.setId(id);
        d.setTitle(o.optString("title", ""));
        d.setProgramId(o.optString("programId", ""));
        d.setProgramName(o.optString("programName", ""));
        d.setCourseCode(o.optString("courseCode", ""));
        d.setCourseTitle(o.optString("courseTitle", ""));
        d.setDescription(o.optString("description", ""));
        d.setFileUrl(o.optString("fileUrl", ""));
        d.setFilePath(o.optString("filePath", ""));
        d.setFileSize(o.optLong("fileSize", 0L));
        d.setUploaderName(o.optString("uploaderName", ""));
        d.setStatus(DocStatus.APPROVED);
        return d;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
