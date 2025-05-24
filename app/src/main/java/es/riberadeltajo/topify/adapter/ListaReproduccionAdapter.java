package es.riberadeltajo.topify.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.riberadeltajo.topify.R;

public class ListaReproduccionAdapter extends RecyclerView.Adapter<ListaReproduccionAdapter.ViewHolder> {

    private List<String> listaReproducciones;
    private Map<String, String> listaFotos;
    private OnListaClickListener listener;
    private OnListaLongClickListener longClickListener;

    public interface OnListaClickListener {
        void onListaClick(String nombreLista);
    }

    public interface OnListaLongClickListener { // Nueva interfaz
        void onListaLongClick(String nombreLista);
    }


    public ListaReproduccionAdapter(List<String> listaReproducciones, OnListaClickListener listener, OnListaLongClickListener longClickListener) {
        this.listaReproducciones = listaReproducciones;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.listaFotos = new HashMap<>(); // Inicializar aquí o pasarlo en el constructor
    }

    public ListaReproduccionAdapter(List<String> listaReproducciones, Map<String, String> listaFotos, OnListaClickListener listener, OnListaLongClickListener longClickListener) {
        this.listaReproducciones = listaReproducciones;
        this.listaFotos = listaFotos;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista_reproduccion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String nombreLista = listaReproducciones.get(position);
        holder.textViewNombreLista.setText(nombreLista);

        // Cargar la imagen si existe una URL
        String fotoUrl = listaFotos.get(nombreLista);
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(fotoUrl)

                    .into(holder.imageViewListaCover);
        } else {
            holder.imageViewListaCover.setImageResource(R.drawable.musica); // Mostrar imagen por defecto
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onListaClick(nombreLista);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onListaLongClick(nombreLista);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return listaReproducciones.size();
    }

    public void setListaFotos(Map<String, String> listaFotos) {
        this.listaFotos = listaFotos;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewNombreLista;
        ImageView imageViewListaCover; // Añadir ImageView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombreLista = itemView.findViewById(R.id.textViewNombreLista);
            imageViewListaCover = itemView.findViewById(R.id.imageViewListaCover); // Asignar ImageView
        }
    }
}