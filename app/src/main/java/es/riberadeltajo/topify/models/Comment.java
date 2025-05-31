package es.riberadeltajo.topify.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {
    private String id; // ID del documento de Firestore
    private String userId;
    private String userName;
    private long songDeezerId; // Usamos long para el ID de la canción
    private String text;
    private Date timestamp; // Usaremos @ServerTimestamp para la fecha de creación

    public Comment() {
        // Constructor público vacío requerido por Firestore
    }

    public Comment(String id, String userId, String userName, long songDeezerId, String text, Date timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.songDeezerId = songDeezerId;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public long getSongDeezerId() {
        return songDeezerId;
    }

    public String getText() {
        return text;
    }

    @ServerTimestamp // Anotación para que Firestore maneje la fecha de creación en el servidor
    public Date getTimestamp() {
        return timestamp;
    }

    // Setters (Firestore puede necesitar setters si no pasas el objeto completo)
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setSongDeezerId(long songDeezerId) {
        this.songDeezerId = songDeezerId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}