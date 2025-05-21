package es.riberadeltajo.topify.database;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    public static void guardarUsuarioFirestore(String uid, String nombre, String email, String fotoUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("email", email);
        usuario.put("foto", fotoUrl);

        db.collection("usuarios").document(uid)
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario guardado correctamente"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al guardar usuario", e));
    }
}