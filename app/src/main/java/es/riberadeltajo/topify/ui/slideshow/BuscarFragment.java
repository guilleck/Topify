package es.riberadeltajo.topify.ui.slideshow;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.SearchResultsAdapter;
import es.riberadeltajo.topify.models.BuscarViewModel;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;
import es.riberadeltajo.topify.models.SearchResult;

public class BuscarFragment extends Fragment implements SearchResultsAdapter.OnSearchResultLongClickListener{

    private BuscarViewModel buscarViewModel;
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView resultsRecyclerView;
    private SearchResultsAdapter adapter;
    private ListaReproduccionViewModel listaReproduccionViewModel;
    private AlertDialog currentDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buscar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buscarViewModel = new ViewModelProvider(this).get(BuscarViewModel.class);
        listaReproduccionViewModel = new ViewModelProvider(this).get(ListaReproduccionViewModel.class);

        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchResultsAdapter(getContext(), getViewLifecycleOwner(), (SearchResultsAdapter.OnSearchResultLongClickListener) this);
        resultsRecyclerView.setAdapter(adapter);

        buscarViewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (results != null) {
                adapter.submitList(results);
            } else {
                Toast.makeText(getContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show();
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                buscarViewModel.buscar(query, "all");
            } else {
                Toast.makeText(getContext(), "Por favor, introduce un término de búsqueda", Toast.LENGTH_SHORT).show();
            }
        });


    }

    public void onSearchResultLongClick(DeezerTrackResponse.Track song) {
        final Observer<List<String>> listaNombresObserver = new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> nombresListas) {
                listaReproduccionViewModel.getListaNombres().removeObserver(this);

                if (nombresListas != null && !nombresListas.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Añadir a lista de reproducción");

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_singlechoice);
                    arrayAdapter.addAll(nombresListas);

                    builder.setAdapter(arrayAdapter, (dialog, which) -> {
                        String listaSeleccionada = nombresListas.get(which);
                        listaReproduccionViewModel.agregarCancionALista(listaSeleccionada, song);
                        Toast.makeText(getContext(), "Añadido a " + listaSeleccionada, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        currentDialog = null;
                    });

                    builder.setNegativeButton("Cancelar", (dialog, which) -> {
                        dialog.dismiss();
                        currentDialog = null;
                    });

                    currentDialog = builder.create();
                    currentDialog.show();
                } else {
                    Toast.makeText(getContext(), "No hay listas de reproducción creadas", Toast.LENGTH_SHORT).show();
                }
            }
        };


        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }


        listaReproduccionViewModel.getListaNombres().observe(getViewLifecycleOwner(), listaNombresObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }
}