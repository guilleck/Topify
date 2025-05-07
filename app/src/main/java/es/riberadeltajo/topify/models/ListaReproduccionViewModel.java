package es.riberadeltajo.topify.models;

import android.app.Application;
import android.util.Log;

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

import es.riberadeltajo.topify.models.DeezerTrackResponse;

public class ListaReproduccionViewModel extends AndroidViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private MutableLiveData<List<String>> listaNombres = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<Map<String, List<DeezerTrackResponse.Track>>> listasConCanciones = new MutableLiveData<>(new HashMap<>());    private ListenerRegistration playlistsListener;
    private Map<String, ListenerRegistration> cancionesListeners = new HashMap<>();

    public ListaReproduccionViewModel(Application application) {
        super(application);
        loadUserPlaylistsFromFirebase();
    }

    private CollectionReference getPlaylistsCollection() {
        return db.collection("listas");
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
                                Map<String, List<DeezerTrackResponse.Track>> nuevasListasConCancionesEnMemoria = new HashMap<>();
                                if (value != null) {
                                    for (QueryDocumentSnapshot doc : value) {
                                        String name = doc.getString("name");
                                        String playlistId = doc.getId();
                                        if (name != null) {
                                            nombresFirebase.add(name);
                                            nuevasListasConCancionesEnMemoria.put(name, new ArrayList<>()); // Inicializar en memoria
                                            loadCancionesDeListaFromFirebase(playlistId, name);
                                        }
                                    }
                                }
                                listaNombres.setValue(nombresFirebase);
                                listasConCanciones.setValue(nuevasListasConCancionesEnMemoria);
                            });
        }
    }

    private void loadCancionesDeListaFromFirebase(String playlistId, String nombreLista) {
        if (playlistId != null) {
            CollectionReference cancionesRef = getPlaylistsCollection().document(playlistId).collection("canciones");
            ListenerRegistration listener = cancionesRef.addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e("Firebase", "Listen for songs in " + nombreLista + " failed.", error);
                    return;
                }

                List<DeezerTrackResponse.Track> canciones = new ArrayList<>();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        DeezerTrackResponse.Track cancion = new DeezerTrackResponse.Track();
                        cancion.deezer_id = doc.getLong("idDeezer"); // Asegúrate de que este campo exista en Firestore
                        cancion.title = doc.getString("title");
                        if (doc.contains("artistName")) {
                            cancion.artist = new DeezerTrackResponse.Track.Artist();
                            cancion.artist.name = doc.getString("artistName");
                        }
                        if (doc.contains("albumTitle")) {
                            cancion.album = new DeezerTrackResponse.Track.Album();
                            cancion.album.title = doc.getString("albumTitle");
                        }
                        canciones.add(cancion);
                    }
                    Map<String, List<DeezerTrackResponse.Track>> currentMap = listasConCanciones.getValue();
                    if (currentMap != null) {
                        currentMap.put(nombreLista, canciones);
                        listasConCanciones.setValue(new HashMap<>(currentMap));
                    }
                } else {
                    Map<String, List<DeezerTrackResponse.Track>> currentMap = listasConCanciones.getValue();
                    if (currentMap != null) {
                        currentMap.put(nombreLista, new ArrayList<>());
                        listasConCanciones.setValue(new HashMap<>(currentMap));
                    }
                }
            });
            cancionesListeners.put(nombreLista, listener);
        }
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

        Map<String, List<DeezerTrackResponse.Track>> cancionesActuales = listasConCanciones.getValue();

        if (cancionesActuales != null && cancionesActuales.containsKey(nombreLista)) {

            List<DeezerTrackResponse.Track> lista = cancionesActuales.get(nombreLista);

            if (lista != null && !lista.contains(cancion)) {

                List<DeezerTrackResponse.Track> nuevaLista = new ArrayList<>(lista);

                nuevaLista.add(cancion);

                cancionesActuales.put(nombreLista, nuevaLista);

                listasConCanciones.setValue(new HashMap<>(cancionesActuales));

            }

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