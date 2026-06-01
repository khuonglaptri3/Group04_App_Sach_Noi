package com.example.a23110035_23110060.model;

public class Chapter {
    private String id;
    private String bookId;
    private String title;
    private int chapterNumber;
    private String audioUrl;
    private int startTime;
    private int endTime;
    
    // For local ebook progress/rendering
    private String textContent;

    public Chapter() {}

    public Chapter(String id, String bookId, String title, int chapterNumber, String audioUrl) {
        this.id = id;
        this.bookId = bookId;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.audioUrl = audioUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getEndTime() { return endTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
}
