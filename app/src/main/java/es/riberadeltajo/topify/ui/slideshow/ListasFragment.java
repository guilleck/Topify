package es.riberadeltajo.topify.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.ListaReproduccionAdapter;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

public class ListasFragment extends Fragment implements ListaReproduccionAdapter.OnListaClickListener {

    private RecyclerView recyclerViewListas;
    private ListaReproduccionAdapter adapter;
    private FloatingActionButton fabNuevaLista;
    private ListaReproduccionViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_listas, container, false);

        recyclerViewListas = root.findViewById(R.id.recyclerViewListas);
        recyclerViewListas.setLayoutManager(new LinearLayoutManager(getContext()));

        fabNuevaLista = root.findViewById(R.id.fabNuevaLista);

        // Obtener el ViewModel compartido
        viewModel = new ViewModelProvider(requireActivity()).get(ListaReproduccionViewModel.class);


        viewModel.getListaNombres().observe(getViewLifecycleOwner(), nombres -> {
            adapter = new ListaReproduccionAdapter(nombres, this); // Asegúrate de pasar 'this' si ListasFragment implementa OnListaClickListener
            recyclerViewListas.setAdapter(adapter);
            adapter.notifyDataSetChanged(); // Opcional, pero útil para asegurar la actualización
        });

        fabNuevaLista.setOnClickListener(v -> mostrarDialogoNuevaLista());

        return root;
    }

    private void mostrarDialogoNuevaLista() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Crear nueva lista");

        final EditText input = new EditText(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String nombreLista = input.getText().toString().trim();
            if (!nombreLista.isEmpty()) {
                viewModel.agregarNuevaLista(nombreLista);
            } else {
                Toast.makeText(getContext(), "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onListaClick(String nombreLista) {
        // Navigate to the list details fragment from the Fragment
        Bundle bundle = new Bundle();
        bundle.putString("nombreLista", nombreLista);
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_slideshow_to_listaDetalleFragment, bundle);
    }
}