package com.example.unilib.data;

import android.content.Context;

import com.example.unilib.R;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.example.unilib.network.ErrorClassifier;
import com.example.unilib.network.HttpClientProvider;
import com.example.unilib.network.NetworkUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reads and updates the members list. Only an admin uses this, and the admin
 * dashboard checks the role again before it calls anything here.
 */
public class UserRepository {

    private final Context ctx;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public UserRepository(Context context) {
        this.ctx = context.getApplicationContext();
    }

    private String friendly(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case PERMISSION_DENIED:
                    return ctx.getString(R.string.err_not_allowed);
                case UNAVAILABLE:
                    return ctx.getString(R.string.err_network);
                default:
                    break;
            }
        }
        return ctx.getString(R.string.err_generic);
    }

    /** Every registered member, newest last. */
    public void listUsers(ResultCallback<List<User>> callback) {
        db.collection("users").get()
                .addOnSuccessListener(snap -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        try {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                u.setId(doc.getId());
                                users.add(u);
                            }
                        } catch (RuntimeException e) {
                            // Skip a member we cannot read rather than losing the list.
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Gives a member a new role. The value stored is the plain enum name. */
    public void updateRole(String uid, Role role, ResultCallback<Void> callback) {
        db.collection("users").document(uid)
                .update("role", role.name())
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Switches a member account on or off. */
    public void setActive(String uid, boolean active, ResultCallback<Void> callback) {
        db.collection("users").document(uid)
                .update("active", active)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Deletes a member profile document from Firestore. */
    public void deleteUser(String uid, ResultCallback<Void> callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onError(ctx.getString(R.string.err_generic));
            return;
        }
        db.collection("users").document(uid).delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Updates a member's profile details. */
    public void updateProfile(String uid, String name, String email, List<String> teachingPrograms, ResultCallback<Void> callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onError(ctx.getString(R.string.err_generic));
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        if (teachingPrograms != null) {
            updates.put("teachingPrograms", teachingPrograms);
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(v -> {
                    FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
                    if (current != null && email != null && !email.trim().isEmpty() && !email.equalsIgnoreCase(current.getEmail())) {
                        try {
                            current.updateEmail(email);
                        } catch (Exception ignored) { }
                    }
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /**
     * Creates an account for a lecturer or a class president.
     *
     * <p>The account is made through the Firebase Auth web API with
     * {@code returnSecureToken} switched off, so the admin who is doing this
     * stays signed in. The profile is then written to the {@code users}
     * collection under the new account's id.
     */
    public void createAccount(String name, String email, String password, Role role,
                              String programId, String programName,
                              String courseCode, String courseTitle,
                              ResultCallback<User> callback) {
        if (role == null) {
            callback.onError(ctx.getString(R.string.error_role_required));
            return;
        }
        // Only an admin may make accounts. The role is read from Firestore again
        // rather than trusted from the screen.
        FirebaseUser caller = FirebaseAuth.getInstance().getCurrentUser();
        if (caller == null) {
            callback.onError(ctx.getString(R.string.err_session));
            return;
        }
        db.collection("users").document(caller.getUid()).get()
                .addOnSuccessListener(snap -> {
                    User me = snap.toObject(User.class);
                    if (me == null || !me.isActive() || me.parsedRole() != Role.ADMIN) {
                        callback.onError(ctx.getString(R.string.err_not_allowed));
                        return;
                    }
                    makeAccount(email, password, role, programId, programName,
                            courseCode, courseTitle, name, callback);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    /** Makes the account and then writes its profile. */
    private void makeAccount(String email, String password, Role role,
                             String programId, String programName,
                             String courseCode, String courseTitle, String name,
                             ResultCallback<User> callback) {
        String apiKey = FirebaseApp.getInstance().getOptions().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError(ctx.getString(R.string.err_generic));
            return;
        }
        createAuthAccount(apiKey, email, password, new ResultCallback<String>() {
            @Override public void onSuccess(String uid) {
                writeProfile(uid, name, email, role, programId, programName, courseCode, courseTitle, callback);
            }
            @Override public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /** Signs the new account up without touching the session of the device. */
    private void createAuthAccount(String apiKey, String email, String password,
                                   ResultCallback<String> callback) {
        if (!NetworkUtils.isOnline(ctx)) {
            callback.onError(ctx.getString(R.string.err_network));
            return;
        }
        String body;
        try {
            body = new JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("returnSecureToken", false)
                    .toString();
        } catch (JSONException e) {
            callback.onError(ctx.getString(R.string.err_generic));
            return;
        }

        Request request = new Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey)
                .post(RequestBody.create(body, MediaType.get("application/json")))
                .build();

        HttpClientProvider.get().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onError(ErrorClassifier.fromException(e).message);
            }

            @Override public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    String text = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) {
                        callback.onError(readAuthError(text));
                        return;
                    }
                    String uid = new JSONObject(text).optString("localId", "");
                    if (uid.isEmpty()) {
                        callback.onError(ctx.getString(R.string.err_generic));
                        return;
                    }
                    callback.onSuccess(uid);
                } catch (JSONException e) {
                    callback.onError(ctx.getString(R.string.err_generic));
                } catch (IOException e) {
                    callback.onError(ErrorClassifier.fromException(e).message);
                }
            }
        });
    }

    /** Turns the error the web API sends into a message the admin can act on. */
    private String readAuthError(String text) {
        try {
            String message = new JSONObject(text).getJSONObject("error")
                    .getString("message");
            if (message.contains("EMAIL_EXISTS")) return ctx.getString(R.string.err_account_exists);
            if (message.contains("WEAK_PASSWORD")) return ctx.getString(R.string.err_weak);
            if (message.contains("INVALID_PASSWORD") || message.contains("INVALID_EMAIL")) {
                return ctx.getString(R.string.error_password_required);
            }
            return ctx.getString(R.string.err_generic);
        } catch (JSONException e) {
            return ctx.getString(R.string.err_generic);
        }
    }

    /** Writes the profile of the new account next to the other members. */
    private void writeProfile(String uid, String name, String email, Role role,
                              String programId, String programName,
                              String courseCode, String courseTitle,
                              ResultCallback<User> callback) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role.name());
        user.setProgramId(programId);
        user.setProgramName(programName);
        user.setCourseCode(courseCode);
        user.setCourseTitle(courseTitle);
        user.setActive(true);
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(v -> {
                    user.setId(uid);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }
}
