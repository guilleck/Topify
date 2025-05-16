package es.riberadeltajo.topify.models;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.riberadeltajo.topify.SongDetailActivity;
import es.riberadeltajo.topify.api.ApiService;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ListaReproduccionViewModel extends AndroidViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private MutableLiveData<List<String>> listaNombres = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<Map<String, List<DeezerTrackResponse.Track>>> listasConCanciones = new MutableLiveData<>(new HashMap<>());    private ListenerRegistration playlistsListener;
    private Map<String, ListenerRegistration> cancionesListeners = new HashMap<>();
    private ApiService apiService;
    private MutableLiveData<DeezerTrackResponse.Track> detallesCancionCargados = new MutableLiveData<>();


    public ListaReproduccionViewModel(Application application) {
        super(application);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
        loadUserPlaylistsFromFirebase();

    }

    public void resetDetallesCancionCargados() {
        detallesCancionCargados.setValue(null);
    }

    private CollectionReference getPlaylistsCollection() {
        return db.collection("listas");
    }
    public LiveData<DeezerTrackResponse.Track> getDetallesCancionCargados() {
        return detallesCancionCargados;
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    private void loadUserPlaylistsFromFirebase() {
        String userId = getCurrentUserId();
        if (userId != null) {
            playlistsListener = getPlaylistsCollection()
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e("Firebase", "Listen failed.", error);
                            return;
                        }

                        List<String> nombresFirebase = new ArrayList<>();
                        Map<String, List<DeezerTrackResponse.Track>> nuevasListasConCanciones = new HashMap<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                String name = doc.getString("name");
                                String playlistId = doc.getId();
                                if (name != null) {
                                    nombresFirebase.add(name);
                                    List<Map<String, Object>> cancionesData = (List<Map<String, Object>>) doc.get("songs");
                                    List<DeezerTrackResponse.Track> canciones = new ArrayList<>();
                                    if (cancionesData != null) {
                                        for (Map<String, Object> cancionMap : cancionesData) {
                                            DeezerTrackResponse.Track cancion = new DeezerTrackResponse.Track();
                                            DeezerTrackResponse.Track.Artist artist = new DeezerTrackResponse.Track.Artist();
                                            DeezerTrackResponse.Track.Album album = new DeezerTrackResponse.Track.Album();

                                            if (cancionMap.get("deezer_id") != null) cancion.deezer_id = ((Number) cancionMap.get("deezer_id")).longValue();
                                            if (cancionMap.get("title") != null) cancion.title = (String) cancionMap.get("title");
                                            if (cancionMap.get("artist") != null) artist.name = (String) cancionMap.get("artist");
                                            if (cancionMap.get("album") != null) album.title = (String) cancionMap.get("album");
                                            if (cancionMap.get("duration") != null) cancion.duration = ((Number) cancionMap.get("duration")).intValue();
                                            if (cancionMap.get("preview") != null) cancion.preview = (String) cancionMap.get("preview");
                                            if (cancionMap.get("albumCover") != null) album.cover_big = (String) cancionMap.get("albumCover");
                                            cancion.artist = artist;
                                            cancion.album = album;
                                            canciones.add(cancion);
                                        }
                                    }
                                    nuevasListasConCanciones.put(name, canciones);
                                }
                            }
                        }
                        listaNombres.setValue(nombresFirebase);
                        listasConCanciones.setValue(nuevasListasConCanciones);
                    });
        }
    }

    public void obtenerDetallesCancion(long trackId, Context context) {
        Call<DeezerTrackResponse.Track> call = apiService.getTrackDetails(trackId);
        call.enqueue(new Callback<DeezerTrackResponse.Track>() {
            @Override
            public void onResponse(Call<DeezerTrackResponse.Track> call, Response<DeezerTrackResponse.Track> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeezerTrackResponse.Track trackDetails = response.body();

                    Intent intent = new Intent(context, SongDetailActivity.class);
                    intent.putExtra("title", trackDetails.title);
                    intent.putExtra("artist", trackDetails.artist != null ? trackDetails.artist.name : "");
                    intent.putExtra("coverUrl", trackDetails.album != null ? trackDetails.album.cover_big : "");
                    intent.putExtra("duration", trackDetails.duration);
                    intent.putExtra("previewUrl", trackDetails.preview);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Error al obtener detalles de la canción", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeezerTrackResponse.Track> call, Throwable t) {
                Toast.makeText(context, "Error de red al obtener detalles de la canción", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (playlistsListener != null) {
            playlistsListener.remove();
        }
        for (ListenerRegistration listener : cancionesListeners.values()) {
            listener.remove();
        }
        cancionesListeners.clear();
    }

    public LiveData<List<String>> getListaNombres() {
        return listaNombres;
    }

    public void agregarNuevaLista(String nombreLista) {
        String userId = getCurrentUserId();
        if (userId != null) {
            Map<String, Object> playlist = new HashMap<>();
            playlist.put("userId", userId);
            playlist.put("name", nombreLista);
            getPlaylistsCollection().add(playlist)
                    .addOnSuccessListener(documentReference -> Log.d("Firebase", "Playlist added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.w("Firebase", "Error adding playlist", e));
        }
    }

    public void agregarCancionALista(String nombreLista, DeezerTrackResponse.Track cancion) {
        Log.d("Firebase", "agregarCancionALista llamada para: " + cancion.title + " en la lista: " + nombreLista);

        Map<String, List<DeezerTrackResponse.Track>> cancionesActuales = listasConCanciones.getValue();

        if (cancionesActuales != null && cancionesActuales.containsKey(nombreLista)) {
            List<DeezerTrackResponse.Track> listaEnMemoria = cancionesActuales.get(nombreLista);

            if (listaEnMemoria != null && !listaEnMemoria.contains(cancion)) {
                List<DeezerTrackResponse.Track> nuevaListaEnMemoria = new ArrayList<>(listaEnMemoria);
                nuevaListaEnMemoria.add(cancion);
                cancionesActuales.put(nombreLista, nuevaListaEnMemoria);
                listasConCanciones.setValue(new HashMap<>(cancionesActuales));
                Log.d("Firebase", "Canción añadida a la memoria: " + cancion.title + " en la lista: " + nombreLista);

                String userId = getCurrentUserId();
                if (userId != null) {
                    getPlaylistsCollection()
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("name", nombreLista)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("Firebase", "Búsqueda de la lista exitosa. Resultados: " + task.getResult().size());
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d("Firebase", "ID del documento encontrado: " + document.getId());
                                        List<Map<String, Object>> cancionesFirebase = (List<Map<String, Object>>) document.get("songs");
                                        if (cancionesFirebase == null) {
                                            cancionesFirebase = new ArrayList<>();
                                            Log.d("Firebase", "El array 'songs' no existía, se ha creado uno nuevo.");
                                        }

                                        Map<String, Object> nuevaCancionMap = new HashMap<>();
                                        nuevaCancionMap.put("deezer_id", cancion.deezer_id);
                                        nuevaCancionMap.put("title", cancion.title);
                                        nuevaCancionMap.put("artist", cancion.artist.name);
                                        nuevaCancionMap.put("album", cancion.album.title);
                                        nuevaCancionMap.put("duration", cancion.duration);
                                        nuevaCancionMap.put("preview", cancion.preview);
                                        nuevaCancionMap.put("albumCover", cancion.album.cover_big);

                                        cancionesFirebase.add(nuevaCancionMap);

                                        getPlaylistsCollection().document(document.getId())
                                                .update("songs", cancionesFirebase)
                                                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Canción añadida a Firestore: " + cancion.title + " en la lista: " + nombreLista))
                                                .addOnFailureListener(e -> Log.w("Firebase", "Error al añadir canción a Firestore: " + e.getMessage()));
                                        return; // Importante: salir del bucle después de actualizar el documento correcto
                                    }
                                    if (task.getResult().isEmpty()) {
                                        Log.w("Firebase", "No se encontró el documento de la lista: " + nombreLista + " para el usuario: " + userId);
                                    }
                                } else {
                                    Log.d("Firebase", "Error al buscar la lista en Firestore: ", task.getException());
                                }
                            });
                } else {
                    Log.w("Firebase", "No hay usuario autenticado, no se puede guardar en Firestore.");
                }
            } else {
                Log.d("Firebase", "La canción ya existe en la lista en memoria.");
            }
        } else {
            Log.w("Firebase", "La lista: " + nombreLista + " no existe en la memoria.");
        }
    }
    public LiveData<List<DeezerTrackResponse.Track>> getCancionesDeLista(String nombreLista) {
        MutableLiveData<List<DeezerTrackResponse.Track>> cancionesDeEstaLista = new MutableLiveData<>();
        if (listasConCanciones.getValue() != null && listasConCanciones.getValue().containsKey(nombreLista)) {
            cancionesDeEstaLista.setValue(listasConCanciones.getValue().get(nombreLista));
        } else {
            cancionesDeEstaLista.setValue(new ArrayList<>());
        }
        return cancionesDeEstaLista;
    }
}