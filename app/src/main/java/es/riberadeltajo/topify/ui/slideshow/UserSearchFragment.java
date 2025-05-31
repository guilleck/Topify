package es.riberadeltajo.topify.ui.slideshow;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        adapter = new UserAdapter(userList, user -> openUserProfile(user.getId()));
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);



        btnSearch.setOnClickListener(v -> {
            String searchText = etSearchName.getText().toString().trim();
            loadUsers(searchText);
        });

        return view;
    }

    // Cambia loadUsers para aceptar un filtro (nombre)
    private void loadUsers(@Nullable String filterName) {
        db.collection("usuarios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String nombre = doc.getString("nombre");
                        String email = doc.getString("email");
                        String foto = doc.getString("foto");

                        if (filterName == null || filterName.isEmpty() || (nombre != null && nombre.toLowerCase().contains(filterName.toLowerCase()))) {
                            userList.add(new User(id, nombre, email, foto));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void openUserProfile(String userId) {
        // Obtiene el NavController desde la vista del fragmento
        NavController navController = Navigation.findNavController(requireView());

        // Crea un Bundle para pasar argumentos (en este caso, el ID del usuario)
        Bundle bundle = new Bundle();
        bundle.putString("userId", userId); // Asegúrate de usar la misma clave definida en el XML

        // Navega a UserProfileFragment usando la acción definida en mobile_navigation.xml
        navController.navigate(R.id.action_searchUsersFragment_to_userProfileFragment, bundle);
    }
}