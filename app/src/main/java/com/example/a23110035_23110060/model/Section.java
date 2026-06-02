package com.example.a23110035_23110060.model;

import java.util.List;

public class Section {
    public static final int TYPE_CAROUSEL = 0;
    public static final int TYPE_BANNER = 1;
    public static final int TYPE_CATEGORIES = 2;
    public static final int TYPE_FEATURED_REVIEWS = 3;

    private final String title;
    private List<Book> books;
    private List<Category> categories;
    private List<FeaturedReview> featuredReviews;
    private final int type;
    private String filter; // e.g., "is_audiobook=eq.true"

    public Section(String title, List<Book> books, int type) {
        this.title = title;
        this.books = books;
        this.type = type;
    }

    public Section(String title, List<Book> books, int type, String filter) {
        this.title = title;
        this.books = books;
        this.type = type;
        this.filter = filter;
    }

    public Section(String title, List<Category> categories, int type, boolean dummy) {
        this.title = title;
        this.categories = categories;
        this.type = type;
    }

    public Section(String title, List<FeaturedReview> featuredReviews, int type, int dummy) {
        this.title = title;
        this.featuredReviews = featuredReviews;
        this.type = type;
    }

    public String getTitle() { return title; }
    public List<Book> getBooks() { return books; }
    public List<Category> getCategories() { return categories; }
    public List<FeaturedReview> getFeaturedReviews() { return featuredReviews; }
    public int getType() { return type; }
    public String getFilter() { return filter; }
}