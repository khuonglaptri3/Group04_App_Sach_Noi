package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;



import com.example.a23110035_23110060.model.FeaturedReview;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeaturedReviewAdapter extends RecyclerView.Adapter<FeaturedReviewAdapter.ViewHolder> {

    private final List<FeaturedReview> reviewList;

    public FeaturedReviewAdapter(List<FeaturedReview> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeaturedReview review = reviewList.get(position);

        holder.tvReviewerName.setText(review.reviewerName);
        holder.tvReviewRating.setText(review.rating + "/5");
        holder.tvReviewTitle.setText(review.comment);
        holder.tvReviewComment.setText(review.comment);

        if (review.avatarUrl != null && !review.avatarUrl.isEmpty() && !review.avatarUrl.equals("null")) {
            Glide.with(holder.itemView.getContext()).load(review.avatarUrl).placeholder(R.drawable.bacl).into(holder.ivReviewerAvatar);
        } else {
            holder.ivReviewerAvatar.setImageResource(R.drawable.bacl);
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

        holder.tvBookTitle.setText(review.bookTitle);
        holder.tvBookAuthor.setText(review.bookAuthor);
        holder.tvBookType.setText(review.bookType != null ? review.bookType.toUpperCase() : "SÁCH");
        holder.tvBookRating.setText(String.format(Locale.getDefault(), "%.1f", review.bookRating));

        Glide.with(holder.itemView.getContext()).load(review.bookCoverUrl).placeholder(R.drawable.bacl).into(holder.ivBookCover);

        holder.layoutBookInfo.setOnClickListener(v -> {
            Class<?> activityClass = "SÁCH ĐIỆN TỬ".equalsIgnoreCase(review.bookType) || "EBOOK".equalsIgnoreCase(review.bookType) ? 
                com.example.a23110035_23110060.view.activity.EbookDetailActivity.class : 
                com.example.a23110035_23110060.view.activity.AudiobookDetailActivity.class;
            Intent intent = new Intent(v.getContext(), activityClass);
            intent.putExtra("bookId", review.bookId);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivReviewerAvatar;
        TextView tvReviewerName, tvReviewDate, tvReviewRating, tvReviewTitle, tvReviewComment;
        ImageView ivBookCover;
        TextView tvBookType, tvBookTitle, tvBookAuthor, tvBookRating;
        View layoutBookInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReviewerAvatar = itemView.findViewById(R.id.iv_reviewer_avatar);
            tvReviewerName = itemView.findViewById(R.id.tv_reviewer_name);
            tvReviewDate = itemView.findViewById(R.id.tv_review_date);
            tvReviewRating = itemView.findViewById(R.id.tv_review_rating);
            tvReviewTitle = itemView.findViewById(R.id.tv_review_title);
            tvReviewComment = itemView.findViewById(R.id.tv_review_comment);
            
            ivBookCover = itemView.findViewById(R.id.iv_book_cover);
            tvBookType = itemView.findViewById(R.id.tv_book_type);
            tvBookTitle = itemView.findViewById(R.id.tv_book_title);
            tvBookAuthor = itemView.findViewById(R.id.tv_book_author);
            tvBookRating = itemView.findViewById(R.id.tv_book_rating);
            layoutBookInfo = itemView.findViewById(R.id.layout_book_info);
        }
    }
}
