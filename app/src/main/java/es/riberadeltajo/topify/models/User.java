package es.riberadeltajo.topify.models;

import com.google.firebase.firestore.Exclude;

public class User {
    private String id, nombre, email, fotoUrl;
    public User(String id, String nombre, String email, String fotoUrl) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.fotoUrl = fotoUrl;
    }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getFotoUrl() { return fotoUrl; }
}
