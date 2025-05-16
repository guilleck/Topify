package es.riberadeltajo.topify.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.riberadeltajo.topify.R;

public class ListaReproduccionAdapter extends RecyclerView.Adapter<ListaReproduccionAdapter.ViewHolder> {

    private List<String> listaReproducciones;
    private OnListaClickListener listener;

    public interface OnListaClickListener {
        void onListaClick(String nombreLista);
    }

    public ListaReproduccionAdapter(List<String> listaReproducciones, OnListaClickListener listener) {
        this.listaReproducciones = listaReproducciones;
        this.listener = listener;
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
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onListaClick(nombreLista);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaReproducciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewNombreLista;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombreLista = itemView.findViewById(R.id.textViewNombreLista);
        }
    }
}