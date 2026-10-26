package com.example.unilib.ui.student;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.unilib.data.DocumentRestApi;
import com.example.unilib.model.Document;
import com.example.unilib.network.ApiResult;
import java.util.List;

public class LibraryViewModel extends AndroidViewModel {
    private final DocumentRestApi api;
    private final MutableLiveData<ApiResult<List<Document>>> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LibraryViewModel(@NonNull Application app) {
        super(app);
        api = new DocumentRestApi(app);
    }

    public LiveData<ApiResult<List<Document>>> getResult() { return result; }
    public LiveData<Boolean> getLoading() { return loading; }

    public void load() {
        loading.setValue(true);
        api.fetchApprovedDocuments(r -> {
            loading.postValue(false);     // callbacks arrive on a background thread: use postValue
            result.postValue(r);
        });
    }
}