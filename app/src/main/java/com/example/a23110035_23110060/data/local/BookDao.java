package com.example.a23110035_23110060.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookDao {
    
    @Query("SELECT * FROM books WHERE isFeatured = 1 ORDER BY cachedAt DESC")
    LiveData<List<BookEntity>> getFeaturedBooks();
    
    @Query("SELECT * FROM books WHERE id = :id")
    LiveData<BookEntity> getBookById(String id);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBooks(List<BookEntity> books);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBook(BookEntity book);
    
    @Query("DELETE FROM books WHERE isFeatured = 1")
    void clearFeaturedBooks();
}
