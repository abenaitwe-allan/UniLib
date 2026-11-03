package com.example.unilib.data;

import android.content.Context;
import com.example.unilib.R;
import com.example.unilib.model.Role;
import com.example.unilib.model.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private final Context ctx;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AuthRepository(Context context) { this.ctx = context.getApplicationContext(); }

    private String friendly(Exception e) {
        if (e instanceof FirebaseNetworkException) return ctx.getString(R.string.err_network);
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException)
            return ctx.getString(R.string.err_credentials);
        if (e instanceof FirebaseAuthUserCollisionException) return ctx.getString(R.string.err_exists);
        if (e instanceof FirebaseAuthWeakPasswordException) return ctx.getString(R.string.err_weak);
        if (e instanceof FirebaseTooManyRequestsException) return ctx.getString(R.string.err_too_many);
        return ctx.getString(R.string.err_generic);
    }

    public void register(String name, String email, String password,
                         String programId, String programName,
                         ResultCallback<User> cb) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                User u = new User();
                u.setName(name);
                u.setEmail(email);
                u.setRole(Role.STUDENT.name());
                    u.setProgramId(programId);
                    u.setProgramName(programName);
                    u.setActive(true);
                    db.collection("users").document(uid).set(u)
                            .addOnSuccessListener(v -> { u.setId(uid); cb.onSuccess(u); })
                            .addOnFailureListener(e -> cb.onError(friendly(e)));
                })
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    public void login(String email, String password, ResultCallback<User> cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> fetchProfile(result.getUser().getUid(), cb))
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    public void restoreSession(ResultCallback<User> cb) {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) { cb.onError(ctx.getString(R.string.err_generic)); return; }
        fetchProfile(current.getUid(), cb);
    }

    public void fetchProfile(String uid, ResultCallback<User> cb) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    User u;
                    try {
                        u = snap.toObject(User.class);
                    } catch (RuntimeException e) {
                        // The document holds a value Firestore cannot map. Sign out
                        // instead of letting the exception kill the app.
                        auth.signOut();
                        cb.onError(ctx.getString(R.string.err_profile_broken));
                        return;
                    }
                    if (u == null) { auth.signOut(); cb.onError(ctx.getString(R.string.err_profile_missing)); return; }
                    if (!u.isActive()) { auth.signOut(); cb.onError(ctx.getString(R.string.err_deactivated)); return; }
                    if (u.parsedRole() == null) {
                        // The role was never set, or it was typed wrongly in the console.
                        auth.signOut();
                        String raw = u.getRole();
                        cb.onError((raw == null || raw.trim().isEmpty())
                                ? ctx.getString(R.string.err_unknown_role_blank)
                                : ctx.getString(R.string.err_unknown_role, raw.trim()));
                        return;
                    }
                    cb.onSuccess(u);
                })
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    public void sendPasswordReset(String email, ResultCallback<String> cb) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> cb.onSuccess(email))
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    public void logout() { auth.signOut(); }
}