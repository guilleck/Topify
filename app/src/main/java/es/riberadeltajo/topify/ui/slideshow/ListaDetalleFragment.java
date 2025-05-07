package es.riberadeltajo.topify.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.CancionListaAdapter;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;


public class ListaDetalleFragment extends Fragment {

    private TextView textViewNombreListaDetalle;
    private RecyclerView recyclerViewCancionesLista;
    private CancionListaAdapter adapter;
    private ListaReproduccionViewModel viewModel;
    private String nombreLista;

    public static ListaDetalleFragment newInstance(String nombreLista) {
        ListaDetalleFragment fragment = new ListaDetalleFragment();
        Bundle args = new Bundle();
        args.putString("nombreLista", nombreLista);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nombreLista = getArguments().getString("nombreLista");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textViewNombreListaDetalle = view.findViewById(R.id.textViewNombreListaDetalle);
        recyclerViewCancionesLista = view.findViewById(R.id.recyclerViewCancionesLista);
        recyclerViewCancionesLista.setLayoutManager(new LinearLayoutManager(getContext()));

        textViewNombreListaDetalle.setText(nombreLista);

        viewModel = new ViewModelProvider(requireActivity()).get(ListaReproduccionViewModel.class);

        viewModel.getCancionesDeLista(nombreLista).observe(getViewLifecycleOwner(), canciones -> {
            adapter = new CancionListaAdapter(canciones); // Asegúrate de que tienes un Adapter para las canciones
            recyclerViewCancionesLista.setAdapter(adapter);
            adapter.notifyDataSetChanged(); // Notifica al Adapter que los datos han cambiado
        });
    }
}