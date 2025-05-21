package es.riberadeltajo.topify.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import es.riberadeltajo.topify.SongDetailActivity;
import es.riberadeltajo.topify.adapter.SongAdapter;
import es.riberadeltajo.topify.api.ApiService;
import es.riberadeltajo.topify.databinding.FragmentHomeBinding;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;
import es.riberadeltajo.topify.models.ListaReproduccion;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment implements SongAdapter.OnItemClickListener, SongAdapter.OnItemLongClickListener {

    private FragmentHomeBinding binding;
    private SongAdapter adapter;
    private ApiService apiService;
    private ListaReproduccionViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Configurar RecyclerView
        RecyclerView recyclerView = binding.recyclerSongs;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new SongAdapter(new ArrayList<>(),requireContext());
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        recyclerView.setAdapter(adapter);

        // Obtener el ViewModel compartido
        viewModel = new ViewModelProvider(requireActivity()).get(ListaReproduccionViewModel.class);

        // Obtener país del usuario
        String countryCode = Locale.getDefault().getCountry();

        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Obtener canciones populares del país
        loadTopTracks(countryCode);

        return root;
    }

    private void loadTopTracks(String countryCode) {
        Call<DeezerTrackResponse> call = apiService.getTopTracksByCountry(countryCode);
        call.enqueue(new Callback<DeezerTrackResponse>() {
            @Override
            public void onResponse(Call<DeezerTrackResponse> call, Response<DeezerTrackResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<DeezerTrackResponse.Track> allTracks = response.body().data;
                    int limit = Math.min(allTracks.size(), 10);
                    List<DeezerTrackResponse.Track> topTracks = allTracks.subList(0, limit);
                    Log.d("API_RESPONSE", "Number of top tracks received: " + topTracks.size());
                    adapter.updateSongs(topTracks);
                } else {
                    Log.e("API_RESPONSE", "Error getting top tracks: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<DeezerTrackResponse> call, Throwable t) {
                Log.e("API_RESPONSE", "API call failed: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemClick(DeezerTrackResponse.Track song) {
        viewModel.obtenerDetallesCancion(song.deezer_id, requireContext());
    }

    @Override
    public void onItemLongClick(DeezerTrackResponse.Track song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Añadir a lista de reproducción");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_singlechoice);

        // Observar las listas de reproducción del ViewModel
        viewModel.getListasReproduccion().observe(getViewLifecycleOwner(), listas -> { //
            arrayAdapter.clear();
            if (listas != null) {

                List<String> nombresListas = listas.stream()
                        .map(ListaReproduccion::getName)
                        .collect(Collectors.toList());
                arrayAdapter.addAll(nombresListas);
            }
            arrayAdapter.notifyDataSetChanged();
        });


        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            // Es importante obtener la lista actualizada de nombres directamente del adaptador
            // para evitar problemas si el LiveData se ha actualizado después de crear el arrayAdapter inicial.
            String listaSeleccionada = arrayAdapter.getItem(which);
            if (listaSeleccionada != null) {
                viewModel.agregarCancionALista(listaSeleccionada, song); //
                Toast.makeText(getContext(), "Añadido a " + listaSeleccionada, Toast.LENGTH_SHORT).show();
                Log.d("AñadirCancion", "Canción '" + song.title + "' añadida a '" + listaSeleccionada + "'");
            }
        });

        builder.show();
    }
}