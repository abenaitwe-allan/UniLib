package com.example.unilib.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.unilib.data.AuthRepository;
import com.example.unilib.data.ResultCallback;
import com.example.unilib.model.User;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repo;
    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> info = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository(app);
    }

    public LiveData<User> getUser() { return user; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getInfo() { return info; }
    public LiveData<Boolean> getLoading() { return loading; }

    /** Call after showing a message, so it is not shown again after rotation. */
    public void clearError() { error.setValue(null); }
    public void clearInfo() { info.setValue(null); }

    private final ResultCallback<User> callback = new ResultCallback<User>() {
        @Override public void onSuccess(User u) { loading.setValue(false); user.setValue(u); }
        @Override public void onError(String message) { loading.setValue(false); error.setValue(message); }
    };

    public void login(String email, String password) { loading.setValue(true); repo.login(email, password, callback); }
    public void register(String name, String email, String password,
                         String programId, String programName) {
        loading.setValue(true);
        repo.register(name, email, password, programId, programName, callback);
    }
    public void restoreSession() { loading.setValue(true); repo.restoreSession(callback); }

    public void sendPasswordReset(String email) {
        loading.setValue(true);
        repo.sendPasswordReset(email, new ResultCallback<String>() {
            @Override public void onSuccess(String sentTo) { loading.setValue(false); info.setValue(sentTo); }
            @Override public void onError(String message) { loading.setValue(false); error.setValue(message); }
        });
    }
}