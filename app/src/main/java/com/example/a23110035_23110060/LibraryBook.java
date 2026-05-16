package com.example.a23110035_23110060;

public class LibraryBook {
    private String title;
    private String author;
    private int coverResId;
    private boolean isPremium;
    private int progress; // 0 to 100
    private String timeRemaining;

    public LibraryBook(String title, String author, int coverResId, boolean isPremium, int progress, String timeRemaining) {
        this.title = title;
        this.author = author;
        this.coverResId = coverResId;
        this.isPremium = isPremium;
        this.progress = progress;
        this.timeRemaining = timeRemaining;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getCoverResId() { return coverResId; }
    public boolean isPremium() { return isPremium; }
    public int getProgress() { return progress; }
    public String getTimeRemaining() { return timeRemaining; }
}