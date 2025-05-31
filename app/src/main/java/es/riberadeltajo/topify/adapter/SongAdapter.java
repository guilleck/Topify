package es.riberadeltajo.topify.adapter;

import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.Song;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<DeezerTrackResponse.Track> songs;
    private OnItemClickListener listener;
    private OnAddButtonClickListener addButtonClickListener;
    private boolean isDarkMode = false;

    public interface OnItemClickListener {
        void onItemClick(DeezerTrackResponse.Track song);
    }

    public interface OnAddButtonClickListener {
        void onAddButtonClick(DeezerTrackResponse.Track song);
    }


    public SongAdapter(List<DeezerTrackResponse.Track> songs, android.content.Context context) {
        this.songs = songs;


        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public void updateSongs(List<DeezerTrackResponse.Track> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnAddButtonClickListener(OnAddButtonClickListener listener) {
        this.addButtonClickListener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        DeezerTrackResponse.Track song = songs.get(position);
        holder.bind(song, isDarkMode);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textArtist;
        ImageView imageCover;
        ImageButton buttonAdd;
        DeezerTrackResponse.Track currentSong;
        View rootView;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textArtist = itemView.findViewById(R.id.textArtist);
            imageCover = itemView.findViewById(R.id.imageCover);
            buttonAdd = itemView.findViewById(R.id.buttonAdd);
            rootView = itemView;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(currentSong);
                }
            });

            buttonAdd.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (addButtonClickListener != null && position != RecyclerView.NO_POSITION) {
                    addButtonClickListener.onAddButtonClick(currentSong);
                }
            });
        }

        public void bind(DeezerTrackResponse.Track song, boolean isDarkMode) {
            currentSong = song;
            textTitle.setText(song.title);
            textArtist.setText(song.artist.name);
            Glide.with(itemView.getContext()).load(song.album.cover_big).into(imageCover);

            // Colores dinámicos según el modo
            if (isDarkMode) {
                rootView.setBackgroundColor(Color.parseColor("#1E1E1E"));
                textTitle.setTextColor(Color.WHITE);
                textArtist.setTextColor(Color.LTGRAY);
            } else {
                rootView.setBackgroundColor(Color.WHITE);
                textTitle.setTextColor(Color.BLACK);
                textArtist.setTextColor(Color.DKGRAY);
            }
        }
    }
}