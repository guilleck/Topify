package es.riberadeltajo.topify.ui.slideshow; // Ajusta el paquete si es necesario

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.PlaylistAdapter;
import es.riberadeltajo.topify.models.Playlist;
import es.riberadeltajo.topify.models.User;
import es.riberadeltajo.topify.ui.slideshow.PlaylistDetailFragment;


public class UserProfileFragment extends Fragment {
    private String userId;
    private TextView tvUserName;
    private ImageView ivUserPhoto;
    private RecyclerView rvPlaylists;
    private FirebaseFirestore db;
    private List<Playlist> playlistList = new ArrayList<>();
    private PlaylistAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        tvUserName = view.findViewById(R.id.tvUserName);
        ivUserPhoto = view.findViewById(R.id.ivUserPhoto);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        db = FirebaseFirestore.getInstance();

        userId = getArguments().getString("userId");

        adapter = new PlaylistAdapter(playlistList, playlist -> openPlaylistDetail(playlist.getId()));
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPlaylists.setAdapter(adapter);

        loadUserInfo();
        loadUserPlaylists();

        return view;
    }

    private void loadUserInfo() {
        db.collection("usuarios").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            String nombre = documentSnapshot.getString("nombre");
            String foto = documentSnapshot.getString("foto"); // Asegúrate de que este campo existe y tiene una URL válida
            tvUserName.setText(nombre);
            // Si 'foto' es null o vacío, Glide podría no hacer nada. Considera una imagen por defecto.
            if (foto != null && !foto.isEmpty()) {
                Glide.with(this).load(foto).into(ivUserPhoto);
            } else {
                // Opcional: Mostrar una imagen por defecto si no hay foto de perfil
                ivUserPhoto.setImageResource(R.drawable.usuario); // Reemplaza con tu drawable por defecto
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
        // Agrega el filtro por userId aquí, asumiendo que tienes un campo 'ownerId' en tus documentos de lista
        db.collection("listas")
                .whereEqualTo("userId", userId) // <--- ¡Importante! Filtra por el ID del propietario
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    playlistList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String foto = doc.getString("fotoUrl");
                        playlistList.add(new Playlist(doc.getId(), name, foto));
                    }
                    adapter.notifyDataSetChanged();
                    if (playlistList.isEmpty()) {
                        Toast.makeText(getContext(), "No se encontraron listas para este usuario.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfileFragment", "Error cargando listas de usuario: " + e.getMessage());
                    Toast.makeText(getContext(), "Error al cargar las listas del usuario", Toast.LENGTH_SHORT).show();
                });
    }


    private void openPlaylistDetail(String playlistId) {
        Bundle bundle = new Bundle();
        bundle.putString("PLAYLIST_ID", playlistId); // Usa la misma clave que en tu argumento

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_userProfileFragment_to_playlistDetailFragment, bundle);
    }
}
