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
import es.riberadeltajo.topify.models.DeezerTrackResponse;

public class CancionListaAdapter extends RecyclerView.Adapter<CancionListaAdapter.ViewHolder> {

    private List<DeezerTrackResponse.Track> canciones;

    public CancionListaAdapter(List<DeezerTrackResponse.Track> canciones) {
        this.canciones = canciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cancion_lista, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeezerTrackResponse.Track cancion = canciones.get(position);
        holder.textViewTituloCancionLista.setText(cancion.title);
        Glide.with(holder.itemView.getContext())
                .load(cancion.album.cover_big)
                .into(holder.imageViewCoverCancionLista);
    }

    @Override
    public int getItemCount() {
        return canciones == null ? 0 : canciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCoverCancionLista;
        TextView textViewTituloCancionLista;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCoverCancionLista = itemView.findViewById(R.id.imageViewCoverCancionLista);
            textViewTituloCancionLista = itemView.findViewById(R.id.textViewTituloCancionLista);
        }
    }
}