package es.riberadeltajo.topify.models;

public class ListaReproduccion {
    private String id; // ID del documento de Firestore
    private String name;
    private String imageUrl;

    public ListaReproduccion() {
        // Constructor vacío requerido para Firestore
    }

    public ListaReproduccion(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}