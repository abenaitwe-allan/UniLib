package com.example.unilib.data;

import android.content.Context;
import com.example.unilib.model.Document;
import com.example.unilib.network.ApiCallback;
import com.example.unilib.network.ApiResult;
import com.example.unilib.network.ErrorClassifier;
import com.example.unilib.network.HttpClientProvider;
import com.example.unilib.network.NetworkUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;

public class DocumentRestApi {
    private static final String QUERY_APPROVED =
            "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"documents\"}],"
                    + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"status\"},"
                    + "\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"APPROVED\"}}}}}";

    private final Context appContext;
    private final OkHttpClient client = HttpClientProvider.get();

    public DocumentRestApi(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void fetchApprovedDocuments(ApiCallback<List<Document>> callback) {
        // 1. No internet? Stop early with a clear message.
        if (!NetworkUtils.isOnline(appContext)) {
            callback.onResult(ApiResult.error(ApiResult.Status.NO_INTERNET, "No internet connection."));
            return;
        }
        // 2. Need a signed-in user to get a token.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(ApiResult.error(ApiResult.Status.UNAUTHORIZED, "Please log in again."));
            return;
        }
        // 3. Get the token, then call the API.
        user.getIdToken(false)
                .addOnSuccessListener(r -> callApi(r.getToken(), callback))
                .addOnFailureListener(e -> callback.onResult(
                        ApiResult.error(ApiResult.Status.UNKNOWN, "Could not verify your session. Please try again.")));
    }

    private void callApi(String token, ApiCallback<List<Document>> callback) {
        String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
        String url = "https://firestore.googleapis.com/v1/projects/" + projectId
                + "/databases/(default)/documents:runQuery";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(QUERY_APPROVED, MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(ErrorClassifier.fromException(e));        // offline, timeout, etc.
            }

            @Override public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    if (!r.isSuccessful()) {                                 // 401, 403, 404, 429, 5xx...
                        callback.onResult(ErrorClassifier.fromHttpCode(r.code()));
                        return;
                    }
                    String body = r.body() != null ? r.body().string() : "";
                    callback.onResult(ApiResult.success(DocumentJsonParser.parse(body)));
                } catch (JSONException e) {                                  // unexpected JSON shape
                    callback.onResult(ApiResult.error(ApiResult.Status.PARSE_ERROR,
                            "Received unexpected data from the server."));
                } catch (IOException e) {                                    // connection dropped mid-read
                    callback.onResult(ErrorClassifier.fromException(e));
                } catch (Exception e) {                                      // last safety net
                    callback.onResult(ApiResult.error(ApiResult.Status.UNKNOWN, "Something went wrong."));
                }
            }
        });
    }
}