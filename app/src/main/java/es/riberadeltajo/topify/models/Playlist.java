package es.riberadeltajo.topify.models;

import java.util.ArrayList;
import java.util.List;

// Importa DeezerTrackResponse.Track si está en otro paquete
// import es.riberadeltajo.topify.models.DeezerTrackResponse;

public class Playlist {
    private String id, name, fotoUrl;
    public Playlist(String id, String name, String fotoUrl) {
        this.id = id;
        this.name = name;
        this.fotoUrl = fotoUrl;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getFotoUrl() { return fotoUrl; }
}
