package com.example.a23110035_23110060;

public class LibraryBook {
    private String bookId;
    private String title;
    private String author;
    private String coverUrl;
    private int coverResId;
    private boolean isPremium;
    private int progress; // 0 to 100
    private String timeRemaining;

    // Flags for filtering
    private boolean isPurchased;
    private boolean isDownloaded;
    private boolean isFavorite;

    public LibraryBook(String title, String author, int coverResId, boolean isPremium, int progress, String timeRemaining) {
        this.title = title;
        this.author = author;
        this.coverResId = coverResId;
        this.isPremium = isPremium;
        this.progress = progress;
        this.timeRemaining = timeRemaining;
    }

    // Constructor for Database
    public LibraryBook() {}

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public int getCoverResId() { return coverResId; }
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(String timeRemaining) { this.timeRemaining = timeRemaining; }

    public boolean isPurchased() { return isPurchased; }
    public void setPurchased(boolean purchased) { isPurchased = purchased; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}