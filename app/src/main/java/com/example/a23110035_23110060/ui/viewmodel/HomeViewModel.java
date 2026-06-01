package com.example.a23110035_23110060.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.a23110035_23110060.NetworkClient;
import com.example.a23110035_23110060.data.local.AppDatabase;
import com.example.a23110035_23110060.data.local.BookDao;
import com.example.a23110035_23110060.data.local.BookEntity;
import com.example.a23110035_23110060.data.repository.BookRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final BookRepository repository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        BookDao bookDao = AppDatabase.getDatabase(application).bookDao();
        repository = new BookRepository(bookDao, NetworkClient.getClient(application));
    }

    public LiveData<List<BookEntity>> getFeaturedBooks(String type) {
        return repository.getFeaturedBooks(type);
    }
}
