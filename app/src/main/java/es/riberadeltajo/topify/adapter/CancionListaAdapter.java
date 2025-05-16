package es.riberadeltajo.topify.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

public class CancionListaAdapter extends RecyclerView.Adapter<CancionListaAdapter.ViewHolder> {

    private List<DeezerTrackResponse.Track> canciones;
    private OnCancionClickListener listener;
    private final Context context;
    private ListaReproduccionViewModel listaReproduccionViewModel;

    public interface OnCancionClickListener {
        void onCancionClick(DeezerTrackResponse.Track cancion);
    }

    public CancionListaAdapter(Context context, List<DeezerTrackResponse.Track> canciones, OnCancionClickListener listener) {
        this.context = context;
        this.canciones = canciones;
        this.listener = listener;
    }

    public void setCanciones(List<DeezerTrackResponse.Track> nuevasCanciones) {
        this.canciones = nuevasCanciones;
        notifyDataSetChanged();
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
        holder.itemView.setOnClickListener(v -> {
            long trackId = cancion.deezer_id;
            if (listaReproduccionViewModel == null) {

                if (context instanceof ViewModelStoreOwner) {
                    listaReproduccionViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(ListaReproduccionViewModel.class);
                    listaReproduccionViewModel.obtenerDetallesCancion(trackId, context);
                }
            } else {
                listaReproduccionViewModel.obtenerDetallesCancion(trackId, context);
            }
            if (listener != null) {
                listener.onCancionClick(cancion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return canciones == null ? 0 : canciones.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCoverCancionLista;
        TextView textViewTituloCancionLista;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCoverCancionLista = itemView.findViewById(R.id.imageViewCoverCancionLista);
            textViewTituloCancionLista = itemView.findViewById(R.id.textViewTituloCancionLista);

        }
    }
}
