package com.example.a23110035_23110060.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "books")
public class BookEntity {
    @PrimaryKey
    @NonNull
    public String id;
    
    public String title;
    public String description;
    public String coverUrl;
    public String authorName;
    public String categoryNameVi;
    public String audioUrl;
    public String epubUrl;
    
    public long cachedAt; // To know when we fetched it last time
    public boolean isFeatured; // To quickly query featured books
    public boolean isAudiobook;
    public boolean isEbook;

    public BookEntity(@NonNull String id) {
        this.id = id;
    }
}
