package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;

    public interface ReviewInteractionListener {
        void onLikeClick(Review review, int position);
    }

    private ReviewInteractionListener listener;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    public void setListener(ReviewInteractionListener listener) {
        this.listener = listener;
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
        holder.tvLikeCount.setText(String.valueOf(review.likeCount));

        if (review.avatarUrl != null && !review.avatarUrl.isEmpty() && !review.avatarUrl.equals("null")) {
            Glide.with(holder.itemView.getContext()).load(review.avatarUrl).placeholder(R.drawable.bacl).into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bacl);
        }

        int activeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.heart_active);
        int inactiveColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_light_onSurfaceVariant);

        if (review.isLiked) {
            holder.ivLikeHeart.setColorFilter(activeColor);
        } else {
            holder.ivLikeHeart.setColorFilter(inactiveColor);
        }

        holder.llLikeReview.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(review, position);
            }
        });

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
        TextView tvReviewerName, tvReviewDate, tvReviewComment, tvReviewRating, tvLikeCount;
        ImageView ivLikeHeart;
        View llLikeReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_reviewer_avatar);
            tvReviewerName = itemView.findViewById(R.id.tv_reviewer_name);
            tvReviewDate = itemView.findViewById(R.id.tv_review_date);
            tvReviewComment = itemView.findViewById(R.id.tv_review_comment);
            tvReviewRating = itemView.findViewById(R.id.tv_review_rating);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            ivLikeHeart = itemView.findViewById(R.id.iv_like_heart);
            llLikeReview = itemView.findViewById(R.id.ll_like_review);
        }
    }

    public static class Review {
        public String id, reviewerName, avatarUrl, comment, createdAt;
        public int rating, likeCount;
        public boolean isLiked;

        public Review(String id, String reviewerName, String avatarUrl, String comment, int rating, String createdAt) {
            this.id = id;
            this.reviewerName = reviewerName;
            this.avatarUrl = avatarUrl;
            this.comment = comment;
            this.rating = rating;
            this.createdAt = createdAt;
            this.likeCount = 0; // Default
            this.isLiked = false;
        }
    }
}