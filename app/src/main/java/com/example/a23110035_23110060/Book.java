package com.example.a23110035_23110060;

public class Book {
    private String title;
    private String author;
    private int coverResId;
    private boolean isPremium;

    public Book(String title, String author, int coverResId, boolean isPremium) {
        this.title = title;
        this.author = author;
        this.coverResId = coverResId;
        this.isPremium = isPremium;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getCoverResId() { return coverResId; }
    public boolean isPremium() { return isPremium; }
}