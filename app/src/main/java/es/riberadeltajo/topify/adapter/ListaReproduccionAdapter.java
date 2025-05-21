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
import es.riberadeltajo.topify.models.ListaReproduccion;

public class ListaReproduccionAdapter extends RecyclerView.Adapter<ListaReproduccionAdapter.ViewHolder> {

    private List<ListaReproduccion> listaReproducciones;
    private OnListaClickListener listener;
    private OnListaLongClickListener longClickListener;

    public interface OnListaClickListener {
        void onListaClick(String nombreLista);
    }

    public interface OnListaLongClickListener { // Nueva interfaz para clic largo
        void onListaLongClick(ListaReproduccion lista);
    }

    public ListaReproduccionAdapter(List<ListaReproduccion> listaReproducciones, OnListaClickListener listener, OnListaLongClickListener longClickListener) {
        this.listaReproducciones = listaReproducciones;
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
        ListaReproduccion lista = listaReproducciones.get(position); // Obtener el objeto ListaReproduccion
        holder.textViewNombreLista.setText(lista.getName()); // Usar getName() del modelo


        if (lista.getImageUrl() != null && !lista.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(lista.getImageUrl())

                    .into(holder.imageViewCover);
        } else {
            holder.imageViewCover.setImageResource(R.drawable.musica); // Si no hay URL, usa la por defecto
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onListaClick(lista.getName());
            }
        });

        // Manejar el clic largo
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onListaLongClick(lista);
                return true; // Consumir el evento
            }
            return false;
        });
    }


    @Override
    public int getItemCount() {
        return listaReproducciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewNombreLista;
        ImageView imageViewCover; // Añadir ImageView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombreLista = itemView.findViewById(R.id.textViewNombreLista);
            imageViewCover = itemView.findViewById(R.id.imageViewCover); // Inicializar ImageView
        }
    }

    // Método para actualizar los datos del adaptador
    public void setListas(List<ListaReproduccion> nuevasListas) {
        this.listaReproducciones = nuevasListas;
        notifyDataSetChanged();
    }
}