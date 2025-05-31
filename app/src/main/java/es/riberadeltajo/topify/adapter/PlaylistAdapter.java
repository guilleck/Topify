package es.riberadeltajo.topify.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.Playlist;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<Playlist> playlistList;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(List<Playlist> playlistList, OnPlaylistClickListener listener) {
        this.playlistList = playlistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.playlistName.setText(playlist.getName()); // Ahora playlistName existe en ViewHolder

        if (playlist.getFotoUrl() != null && !playlist.getFotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(playlist.getFotoUrl())
                    .into(holder.playlistImage); // Ahora playlistImage existe en ViewHolder
        } else {
            holder.playlistImage.setImageResource(R.drawable.musica); // Imagen por defecto
        }

        // Configurar el listener para el clic en el elemento
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        // Renombradas para coincidir con el uso en onBindViewHolder
        TextView playlistName; // Antes tvName
        ImageView playlistImage; // Antes ivCover

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            // Asegúrate de que estos IDs (tvPlaylistItemName, ivPlaylistItemCover)
            // existen en tu archivo de layout item_playlist.xml
            playlistName = itemView.findViewById(R.id.tvPlaylistItemName);
            playlistImage = itemView.findViewById(R.id.ivPlaylistItemCover);
        }
    }
}