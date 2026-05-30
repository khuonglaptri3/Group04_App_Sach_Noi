package com.example.a23110035_23110060;

public class FeaturedReview {
    public String id, reviewerName, avatarUrl, comment, createdAt;
    public int rating;
    public String bookId, bookTitle, bookAuthor, bookCoverUrl, bookType;
    public double bookRating;

    public FeaturedReview(String id, String reviewerName, String avatarUrl, String comment, int rating, String createdAt,
                          String bookId, String bookTitle, String bookAuthor, String bookCoverUrl, String bookType, double bookRating) {
        this.id = id;
        this.reviewerName = reviewerName;
        this.avatarUrl = avatarUrl;
        this.comment = comment;
        this.rating = rating;
        this.createdAt = createdAt;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.bookCoverUrl = bookCoverUrl;
        this.bookType = bookType;
        this.bookRating = bookRating;
    }
}
