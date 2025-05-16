package es.riberadeltajo.topify.ui.slideshow;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.SearchResultsAdapter;
import es.riberadeltajo.topify.models.BuscarViewModel;
import es.riberadeltajo.topify.models.SearchResult;

public class BuscarFragment extends Fragment {

    private BuscarViewModel buscarViewModel;
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView resultsRecyclerView;
    private SearchResultsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buscar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buscarViewModel = new ViewModelProvider(this).get(BuscarViewModel.class);

        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchResultsAdapter(getContext(), getViewLifecycleOwner());
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
}