package com.example.unilib.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.example.unilib.BuildConfig;
import com.example.unilib.R;
import com.example.unilib.network.ApiCallback;
import com.example.unilib.network.ApiResult;
import com.example.unilib.network.ErrorClassifier;
import com.example.unilib.network.HttpClientProvider;
import com.example.unilib.network.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

/** Uploads PDF files to the Supabase Storage bucket "documents". */
public class Storagerepository {

    private static final String BUCKET = "documents";

    /** What we need to remember about an uploaded file. */
    public static class UploadedFile {
        public final String path;       // name inside the bucket
        public final String publicUrl;  // link stored in Firestore

        public UploadedFile(String path, String publicUrl) {
            this.path = path;
            this.publicUrl = publicUrl;
        }
    }

    private final Context ctx;
    private final OkHttpClient client = HttpClientProvider.forUpload();

    public Storagerepository(Context context) {
        this.ctx = context.getApplicationContext();
    }

    /** The callback may run on a background thread. Use postValue() in the ViewModel. */
    public void upload(Uri fileUri, long fileSize, ApiCallback<UploadedFile> callback) {
        // 1. Internet?
        if (!NetworkUtils.isOnline(ctx)) {
            callback.onResult(ApiResult.error(ApiResult.Status.NO_INTERNET,
                    ctx.getString(R.string.err_network)));
            return;
        }

        // 2. Keys present?
        String baseUrl = BuildConfig.SUPABASE_URL;
        String key = BuildConfig.SUPABASE_KEY;
        if (baseUrl == null || baseUrl.isEmpty() || key == null || key.isEmpty()) {
            callback.onResult(ApiResult.error(ApiResult.Status.UNKNOWN,
                    ctx.getString(R.string.err_upload_denied)));
            return;
        }

        // 3. Random, unguessable file name
        final String path = UUID.randomUUID() + ".pdf";
        String url = baseUrl + "/storage/v1/object/" + BUCKET + "/" + path;
        final String publicUrl = baseUrl + "/storage/v1/object/public/" + BUCKET + "/" + path;

        // 4. Stream the file straight from the phone
        final ContentResolver resolver = ctx.getContentResolver();
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/pdf");
            }

            @Override
            public long contentLength() {
                return fileSize;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream in = resolver.openInputStream(fileUri)) {
                    if (in == null) throw new IOException("Cannot open file");
                    sink.writeAll(Okio.source(in));
                }
            }
        };

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", key)
                .post(body);
        // Old-style anon keys are JWTs (start with "eyJ") and also need the Bearer header.
        if (key.startsWith("eyJ")) {
            builder.addHeader("Authorization", "Bearer " + key);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onResult(ErrorClassifier.fromException(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    int code = r.code();
                    if (r.isSuccessful()) {
                        callback.onResult(ApiResult.success(new UploadedFile(path, publicUrl)));
                    } else if (code == 401 || code == 403) {
                        callback.onResult(ApiResult.error(ApiResult.Status.UNAUTHORIZED,
                                ctx.getString(R.string.err_upload_denied)));
                    } else if (code == 413) {
                        callback.onResult(ApiResult.error(ApiResult.Status.HTTP_ERROR,
                                ctx.getString(R.string.err_file_too_large)));
                    } else {
                        callback.onResult(ErrorClassifier.fromHttpCode(code));
                    }
                } catch (Exception e) {
                    callback.onResult(ApiResult.error(ApiResult.Status.UNKNOWN,
                            ctx.getString(R.string.err_generic)));
                }
            }
        });
    }
}