package com.example.a23110035_23110060.view.bottomsheet;

import com.example.a23110035_23110060.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class EbookChaptersBottomSheet extends BottomSheetDialogFragment {

    public interface ChapterListener {
        void onChapterSelected(int index);
    }

    private final List<String> chapterTitles;
    private final ChapterListener listener;
    private int currentChapterIndex = -1;

    public EbookChaptersBottomSheet(List<String> chapterTitles, ChapterListener listener) {
        this.chapterTitles = chapterTitles;
        this.listener = listener;
    }

    public void setCurrentChapterIndex(int index) {
        this.currentChapterIndex = index;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chapters_sheet, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_chapters);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new RecyclerView.Adapter<ChapterViewHolder>() {
            @NonNull
            @Override
            public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new ChapterViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
                holder.tvTitle.setText(chapterTitles.get(position));
                
                if (position == currentChapterIndex) {
                    holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    holder.tvTitle.setTextColor(android.graphics.Color.parseColor("#4F46E5")); // Primary color
                } else {
                    holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
                    holder.tvTitle.setTextColor(android.graphics.Color.parseColor("#808080")); // Secondary color
                }

                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onChapterSelected(position);
                    dismiss();
                });
//                holder.itemView.setOnClickListener(
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                if (listener != null)
//                                    listener.onChapterSelected(position);
//                                dismiss();
//                            }
//                        }
//                );
            }

            @Override
            public int getItemCount() {
                return chapterTitles.size();
            }
        });

        return view;
    }

    private static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
        }
    }
}
