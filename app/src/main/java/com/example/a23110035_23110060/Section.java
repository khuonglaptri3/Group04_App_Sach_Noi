package com.example.a23110035_23110060;

import java.util.List;

public class Section {
    public static final int TYPE_CAROUSEL = 0;
    public static final int TYPE_BANNER = 1;
    public static final int TYPE_CATEGORIES = 2;

    private String title;
    private List<Book> books;
    private List<Category> categories;
    private int type;

    public Section(String title, List<Book> books, int type) {
        this.title = title;
        this.books = books;
        this.type = type;
    }

    public Section(String title, List<Category> categories, int type, boolean dummy) {
        this.title = title;
        this.categories = categories;
        this.type = type;
    }

    public String getTitle() { return title; }
    public List<Book> getBooks() { return books; }
    public List<Category> getCategories() { return categories; }
    public int getType() { return type; }
}