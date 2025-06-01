package es.riberadeltajo.topify.ui.slideshow;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Importaciones necesarias para Base64
import android.util.Base64;
import java.io.UnsupportedEncodingException;


import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.PlaylistAdapter;
import es.riberadeltajo.topify.adapter.UserAdapter;
import es.riberadeltajo.topify.models.Playlist;
import es.riberadeltajo.topify.models.User;
import es.riberadeltajo.topify.ui.slideshow.PlaylistDetailFragment;


public class UserProfileFragment extends Fragment {
    private String userId; // ID del usuario cuyo perfil estamos viendo
    private String currentUserId; // ID del usuario actualmente logueado
    private TextView tvUserName;
    private ImageView ivUserPhoto;
    private RecyclerView rvPlaylists;
    private RecyclerView rvFriends; // Nuevo RecyclerView para amigos
    private FirebaseFirestore db;
    private List<Playlist> playlistList = new ArrayList<>();
    private List<User> friendList = new ArrayList<>(); // Nueva lista para amigos
    private PlaylistAdapter playlistAdapter; // Renombrado para claridad
    private UserAdapter friendAdapter; // Nuevo adaptador para amigos
    private Button btnAddFriend; // Nuevo botón
    private TextView tvFriendsTitle; // Nuevo TextView para el título de amigos


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        tvUserName = view.findViewById(R.id.tvUserName);
        ivUserPhoto = view.findViewById(R.id.ivUserPhoto);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        rvFriends = view.findViewById(R.id.rvFriends); // Inicializar RecyclerView de amigos
        btnAddFriend = view.findViewById(R.id.btnAddFriend); // Inicializar botón
        tvFriendsTitle = view.findViewById(R.id.tvFriendsTitle); // Inicializar título de amigos

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance(); // Inicializar FirebaseAuth
        FirebaseUser currentUser = auth.getCurrentUser(); // Obtener usuario actual
        if (currentUser != null) { //
            currentUserId = currentUser.getUid(); // Obtener UID del usuario actual
        }

        userId = getArguments().getString("userId"); // ID del perfil que estamos visitando

        playlistAdapter = new PlaylistAdapter(playlistList, playlist -> openPlaylistDetail(playlist.getId()));
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPlaylists.setAdapter(playlistAdapter);


        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriends.setAdapter(friendAdapter);

        setupUI();
        loadUserInfo();
        loadUserPlaylists();

        return view;
    }

    // Método para decodificar una cadena Base64
    private String decodeBase64(String encodedString) {
        if (encodedString == null || encodedString.isEmpty()) {
            return "";
        }
        try {
            byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
            return new String(decodedBytes, "UTF-8");
        } catch (IllegalArgumentException e) {
            Log.e("Base64Decoder", "Error decodificando Base64: " + e.getMessage() + " para la cadena: " + encodedString);
            return encodedString; // Devuelve la cadena original si hay un error de formato Base64
        } catch (UnsupportedEncodingException e) {
            Log.e("Base64Decoder", "Error de codificación de caracteres: " + e.getMessage());
            return encodedString; // Devuelve la cadena original si hay un error de codificación
        }
    }

    private void setupUI() { // Nuevo método para configurar la visibilidad de los elementos
        if (currentUserId != null && currentUserId.equals(userId)) { // Si es el perfil del usuario actual
            btnAddFriend.setVisibility(View.GONE); // Ocultar botón "Añadir amigo"
            tvFriendsTitle.setVisibility(View.VISIBLE); // Mostrar título de amigos
            rvFriends.setVisibility(View.VISIBLE); // Mostrar RecyclerView de amigos
            loadFriends(); // Cargar la lista de amigos del usuario actual
        } else { // Si es el perfil de otro usuario
            btnAddFriend.setVisibility(View.VISIBLE); // Mostrar botón "Añadir amigo"
            tvFriendsTitle.setVisibility(View.GONE); // Ocultar título de amigos
            rvFriends.setVisibility(View.GONE); // Ocultar RecyclerView de amigos
            checkIfAlreadyFriend(); // Verificar si ya son amigos para deshabilitar el botón
            btnAddFriend.setOnClickListener(v -> addFriend()); // Establecer listener para añadir amigo
        }
    }

    private void loadUserInfo() {
        db.collection("usuarios").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            String encodedNombre = documentSnapshot.getString("nombre");
            String encodedEmail = documentSnapshot.getString("email");
            String foto = documentSnapshot.getString("foto");

            String nombreDecodificado = decodeBase64(encodedNombre);
            String emailDecodificado = decodeBase64(encodedEmail);

            tvUserName.setText(nombreDecodificado);
            if (foto != null && !foto.isEmpty()) {
                Glide.with(this).load(foto).into(ivUserPhoto);
            } else {
                ivUserPhoto.setImageResource(R.drawable.usuario);
            }
        }).addOnFailureListener(e -> {
            Log.e("UserProfileFragment", "Error cargando info de usuario: " + e.getMessage());
            Toast.makeText(getContext(), "Error al cargar información del usuario", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserPlaylists() {
        if (userId == null || userId.isEmpty()) {
            Log.e("UserProfileFragment", "userId es nulo o vacío, no se pueden cargar las listas.");
            return;
        }
        db.collection("listas")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    playlistList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String foto = doc.getString("fotoUrl");
                        // Las listas de reproducción no parecen estar codificadas, si lo estuvieran, decodificar aquí también
                        playlistList.add(new Playlist(doc.getId(), name, foto));
                    }
                    playlistAdapter.notifyDataSetChanged();
                    if (playlistList.isEmpty()) {
                        Toast.makeText(getContext(), "No se encontraron listas para este usuario.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfileFragment", "Error cargando listas de usuario: " + e.getMessage());
                    Toast.makeText(getContext(), "Error al cargar las listas del usuario", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriends() { // Nuevo método para cargar amigos del usuario actual
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e("UserProfileFragment", "currentUserId es nulo o vacío, no se pueden cargar los amigos.");
            return;
        }

        db.collection("usuarios").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> friendIds = (List<String>) documentSnapshot.get("friends");
                    friendList.clear();
                    if (friendIds != null && !friendIds.isEmpty()) {
                        for (String friendId : friendIds) {
                            db.collection("usuarios").document(friendId).get()
                                    .addOnSuccessListener(friendDoc -> {
                                        if (friendDoc.exists()) {
                                            String id = friendDoc.getId();
                                            String encodedNombre = friendDoc.getString("nombre"); // Obtener nombre codificado
                                            String encodedEmail = friendDoc.getString("email");   // Obtener email codificado
                                            String foto = friendDoc.getString("foto");

                                            // Decodificar el nombre y el email del amigo
                                            String nombreDecodificado = decodeBase64(encodedNombre);
                                            String emailDecodificado = decodeBase64(encodedEmail);

                                            friendList.add(new User(id, nombreDecodificado, emailDecodificado, foto)); // Añadir a la lista de amigos con datos decodificados
                                            friendAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("UserProfileFragment", "Error cargando datos de amigo: " + e.getMessage()));
                        }
                    } else {
                        Toast.makeText(getContext(), "Aún no tienes amigos.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfileFragment", "Error cargando la lista de amigos: " + e.getMessage());
                    Toast.makeText(getContext(), "Error al cargar amigos.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addFriend() {
        if (currentUserId == null || userId == null || currentUserId.equals(userId)) {
            Toast.makeText(getContext(), "Operación no válida para añadir amigo.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference currentUserRef = db.collection("usuarios").document(currentUserId);
        DocumentReference targetUserRef = db.collection("usuarios").document(userId);

        currentUserRef.update("friends", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    targetUserRef.update("friends", FieldValue.arrayUnion(currentUserId))
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), "¡Amigo añadido!", Toast.LENGTH_SHORT).show();
                                btnAddFriend.setEnabled(false);
                                btnAddFriend.setText("Amigo Añadido");
                                Log.d("UserProfileFragment", "Amigo añadido con éxito en ambas direcciones.");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error al añadir amigo al otro usuario.", Toast.LENGTH_SHORT).show();
                                Log.e("UserProfileFragment", "Error al añadir amigo al otro usuario", e);
                                // Considerar revertir la primera actualización si esta falla
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al añadir amigo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UserProfileFragment", "Error al añadir amigo al usuario actual", e);
                });
    }

    private void checkIfAlreadyFriend() {
        if (currentUserId == null || userId == null || currentUserId.equals(userId)) {
            return;
        }

        db.collection("usuarios").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> friends = (List<String>) documentSnapshot.get("friends");
                    if (friends != null && friends.contains(userId)) {
                        btnAddFriend.setEnabled(false);
                        btnAddFriend.setText("Ya son Amigos");
                    }
                })
                .addOnFailureListener(e -> Log.e("UserProfileFragment", "Error al verificar amistad: " + e.getMessage()));
    }

    private void openPlaylistDetail(String playlistId) {
        Bundle bundle = new Bundle();
        bundle.putString("PLAYLIST_ID", playlistId);

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_userProfileFragment_to_playlistDetailFragment, bundle);
    }
}