package es.riberadeltajo.topify.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private boolean isDarkMode;

    public CommentAdapter(List<Comment> comments, boolean isDarkMode) {
        this.comments = comments;
        this.isDarkMode = isDarkMode;
    }

    public void setComments(List<Comment> newComments) {
        this.comments.clear();
        this.comments.addAll(newComments);
        notifyDataSetChanged(); // Notifica al RecyclerView que los datos han cambiado
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, isDarkMode);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCommentUser;
        TextView textViewCommentText;
        TextView textViewCommentTimestamp;
        View rootView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView;
            textViewCommentUser = itemView.findViewById(R.id.textViewCommentUser);
            textViewCommentText = itemView.findViewById(R.id.textViewCommentText);
            textViewCommentTimestamp = itemView.findViewById(R.id.textViewCommentTimestamp);
        }

        public void bind(Comment comment, boolean isDarkMode) {
            textViewCommentUser.setText(comment.getUserName());
            textViewCommentText.setText(comment.getText());

            if (comment.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                textViewCommentTimestamp.setText(sdf.format(comment.getTimestamp()));
            } else {
                textViewCommentTimestamp.setText("Fecha desconocida");
            }

            if (isDarkMode) {
                rootView.setBackgroundColor(Color.parseColor("#333333"));
                textViewCommentUser.setTextColor(Color.WHITE);
                textViewCommentText.setTextColor(Color.LTGRAY);
                textViewCommentTimestamp.setTextColor(Color.GRAY);
            } else {
                rootView.setBackgroundColor(Color.WHITE);
                textViewCommentUser.setTextColor(Color.BLACK);
                textViewCommentText.setTextColor(Color.DKGRAY);
                textViewCommentTimestamp.setTextColor(Color.GRAY);
            }
        }
    }
}