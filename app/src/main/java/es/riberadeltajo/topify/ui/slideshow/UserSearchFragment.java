package es.riberadeltajo.topify.ui.slideshow;

import android.os.Bundle;
import android.util.Base64; // Importar Base64
import java.io.UnsupportedEncodingException; // Importar para manejo de errores de codificación
import android.util.Log; // Importar para logs de errores

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.UserAdapter;
import es.riberadeltajo.topify.models.User;

public class UserSearchFragment extends Fragment {
    private RecyclerView rvUsers;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<User> userList = new ArrayList<>();
    private UserAdapter adapter;

    private EditText etSearchName;
    private Button btnSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_search, container, false);
        rvUsers = view.findViewById(R.id.rvUsers);
        etSearchName = view.findViewById(R.id.etSearchName);
        btnSearch = view.findViewById(R.id.btnSearch);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new UserAdapter(userList, user -> openUserProfile(user.getId()));
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String searchText = etSearchName.getText().toString().trim();
            loadUsers(searchText);
        });

        return view;
    }

    private String decodeBase64(String encodedString) {
        if (encodedString == null || encodedString.isEmpty()) {
            return "";
        }
        try {
            byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
            return new String(decodedBytes, "UTF-8");
        } catch (IllegalArgumentException e) {
            Log.e("Base64Decoder", "Error decodificando Base64: " + e.getMessage() + " para la cadena: " + encodedString);
            return encodedString;
        } catch (UnsupportedEncodingException e) {
            Log.e("Base64Decoder", "Error de codificación de caracteres: " + e.getMessage());
            return encodedString;
        }
    }


    private void loadUsers(@Nullable String filterName) {
        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String encodedNombre = doc.getString("nombre");
                        String email = doc.getString("email");
                        String foto = doc.getString("foto");


                        String nombreDecodificado = decodeBase64(encodedNombre);

                        String emailDecodificado = decodeBase64(email);

                        if (filterName == null || filterName.isEmpty() || (nombreDecodificado != null && nombreDecodificado.toLowerCase().contains(filterName.toLowerCase()))) {
                            userList.add(new User(id, nombreDecodificado, emailDecodificado, foto)); // Usa 'email' o 'emailDecodificado'
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserSearchFragment", "Error al cargar usuarios: " + e.getMessage());
                    // Manejar el error, por ejemplo, mostrar un Toast al usuario
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al cargar usuarios.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openUserProfile(String userId) {
        NavController navController = Navigation.findNavController(requireView());

        FirebaseUser currentUser = auth.getCurrentUser();
        String currentUserId = null;
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        if (currentUserId != null && currentUserId.equals(userId)) {
            navController.navigate(R.id.nav_perfil);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("userId", userId);

            navController.navigate(R.id.action_searchUsersFragment_to_userProfileFragment, bundle);
        }
    }
}