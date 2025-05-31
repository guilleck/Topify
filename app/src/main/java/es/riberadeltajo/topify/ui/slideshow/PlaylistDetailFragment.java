package es.riberadeltajo.topify.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.SongDetailActivity;
import es.riberadeltajo.topify.adapter.SongAdapter;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

public class PlaylistDetailFragment extends Fragment implements SongAdapter.OnItemClickListener, SongAdapter.OnAddButtonClickListener { // Implementar interfaces
    private String playlistId;
    private RecyclerView rvSongs;
    private FirebaseFirestore db;
    private List<DeezerTrackResponse.Track> songList = new ArrayList<>();

    private SongAdapter adapter;
    private ListaReproduccionViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);
        rvSongs = view.findViewById(R.id.rvSongs);
        db = FirebaseFirestore.getInstance();

        playlistId = getArguments().getString("PLAYLIST_ID");

        adapter = new SongAdapter(songList, getContext());
        adapter.setOnItemClickListener(this); // Establecer el listener para clics en ítems
        adapter.setOnAddButtonClickListener(this); // Establecer el listener para clics en el botón de añadir
        rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSongs.setAdapter(adapter);

        // Obtener el ViewModel compartido
        viewModel = new ViewModelProvider(requireActivity()).get(ListaReproduccionViewModel.class);

        loadSongs();
        return view;
    }

    private void loadSongs() {
        db.collection("listas")
                .document(playlistId)
                .get().addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> songs = (List<Map<String, Object>>) documentSnapshot.get("songs");
                    songList.clear();
                    if (songs != null) {
                        for (Map<String, Object> songData : songs) {
                            String title = (String) songData.get("title");
                            String artistName = (String) songData.get("artist");
                            String albumTitle = (String) songData.get("album");
                            String coverUrl = (String) songData.get("albumCover");
                            // Asegúrate de recuperar todos los campos necesarios para SongDetailActivity
                            // Si no están en Firestore, deberás ajustar tu modelo o la forma en que los obtienes
                            long duration = songData.containsKey("duration") ? (long) songData.get("duration") : 0; // Ejemplo: Si tienes la duración en Firestore
                            String previewUrl = songData.containsKey("previewUrl") ? (String) songData.get("previewUrl") : null; // Ejemplo: Si tienes el previewUrl
                            long deezerId = songData.containsKey("deezerId") ? (long) songData.get("deezerId") : 0; // Ejemplo: Si tienes el deezerId

                            DeezerTrackResponse.Track track = new DeezerTrackResponse.Track();
                            track.title = title;

                            DeezerTrackResponse.Track.Artist artist = new DeezerTrackResponse.Track.Artist();
                            artist.name = artistName;
                            track.artist = artist;

                            DeezerTrackResponse.Track.Album album = new DeezerTrackResponse.Track.Album();
                            album.title = albumTitle;
                            album.cover_big = coverUrl;
                            track.album = album;

                            track.duration = (int) duration;
                            track.preview = previewUrl;
                            track.deezer_id = deezerId;

                            songList.add(track);
                        }
                        adapter.notifyDataSetChanged();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("PlaylistDetailFragment", "Error al cargar canciones de la lista: " + e.getMessage());
                    Toast.makeText(getContext(), "Error al cargar canciones.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(DeezerTrackResponse.Track song) {
        // Lógica similar a HomeFragment para abrir SongDetailActivity
        Intent intent = new Intent(getContext(), SongDetailActivity.class);
        intent.putExtra("title", song.title);
        intent.putExtra("artist", song.artist.name);
        intent.putExtra("coverUrl", song.album.cover_big);
        intent.putExtra("duration", song.duration);
        intent.putExtra("previewUrl", song.preview);
        intent.putExtra("deezerId", song.deezer_id);
        startActivity(intent);
    }

    @Override
    public void onAddButtonClick(DeezerTrackResponse.Track song) {
        // Lógica similar a HomeFragment para añadir a una lista de reproducción
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Añadir a lista de reproducción");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_singlechoice);

        List<String> nombresListas = viewModel.getListaNombres().getValue();
        if (nombresListas != null) {
            arrayAdapter.addAll(nombresListas);
        }

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            String listaSeleccionada = nombresListas.get(which);
            viewModel.agregarCancionALista(listaSeleccionada, song);
            Toast.makeText(getContext(), "Añadido a " + listaSeleccionada, Toast.LENGTH_SHORT).show();
            Log.d("AñadirCancion", "Canción '" + song.title + "' añadida a '" + listaSeleccionada + "'");
        });

        builder.show();
    }
}