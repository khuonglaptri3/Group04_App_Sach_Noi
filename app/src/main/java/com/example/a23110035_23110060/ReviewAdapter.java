package com.example.a23110035_23110060;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.tvReviewerName.setText(review.reviewerName);
        holder.tvReviewComment.setText(review.comment);
        holder.tvReviewRating.setText(review.rating + "/5");

        if (review.avatarUrl != null && !review.avatarUrl.isEmpty() && !review.avatarUrl.equals("null")) {
            Glide.with(holder.itemView.getContext()).load(review.avatarUrl).placeholder(R.drawable.bacl).into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bacl);
        }

        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = parser.parse(review.createdAt.split("\\.")[0]);
            if (date != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                holder.tvReviewDate.setText(formatter.format(date));
            }
        } catch (Exception e) {
            holder.tvReviewDate.setText(review.createdAt);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvReviewerName, tvReviewDate, tvReviewComment, tvReviewRating;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_reviewer_avatar);
            tvReviewerName = itemView.findViewById(R.id.tv_reviewer_name);
            tvReviewDate = itemView.findViewById(R.id.tv_review_date);
            tvReviewComment = itemView.findViewById(R.id.tv_review_comment);
            tvReviewRating = itemView.findViewById(R.id.tv_review_rating);
        }
    }

    public static class Review {
        public String id, reviewerName, avatarUrl, comment, createdAt;
        public int rating;

        public Review(String id, String reviewerName, String avatarUrl, String comment, int rating, String createdAt) {
            this.id = id;
            this.reviewerName = reviewerName;
            this.avatarUrl = avatarUrl;
            this.comment = comment;
            this.rating = rating;
            this.createdAt = createdAt;
        }
    }
}
