package es.riberadeltajo.topify.models;

import java.util.List;

public class User {
    private String id;
    private String nombre;
    private String email;
    private String foto;
    private List<String> friends; // Nueva propiedad para almacenar IDs de amigos

    public User() {
        // Constructor vacío necesario para Firestore
    }

    public User(String id, String nombre, String email, String foto) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.foto = foto;
        // La lista de amigos se inicializará cuando se cargue desde Firestore
    }

    public User(String id, String nombre, String email, String foto, List<String> friends) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.foto = foto;
        this.friends = friends;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getFoto() {
        return foto;
    }

    public List<String> getFriends() {
        return friends;
    }

    // Setters (si son necesarios)
    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }
}