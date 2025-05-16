package es.riberadeltajo.topify.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.DeezerTrackResponse;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<DeezerTrackResponse.Track> songs;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(DeezerTrackResponse.Track song);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(DeezerTrackResponse.Track song);
    }


    public SongAdapter(List<DeezerTrackResponse.Track> songs) {
        this.songs = songs;
    }

    public void updateSongs(List<DeezerTrackResponse.Track> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
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
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textArtist;
        ImageView imageCover;
        DeezerTrackResponse.Track currentSong;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textArtist = itemView.findViewById(R.id.textArtist);
            imageCover = itemView.findViewById(R.id.imageCover);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(currentSong);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(currentSong);
                }
                return true;
            });
        }

        public void bind(DeezerTrackResponse.Track song) {
            currentSong = song;
            String title = song.title;
            String artist = song.artist.name;
            String imageUrl = song.album.cover_big;

            textTitle.setText(title);
            textArtist.setText(artist);
            Glide.with(itemView.getContext()).load(imageUrl).into(imageCover);
        }
    }
}