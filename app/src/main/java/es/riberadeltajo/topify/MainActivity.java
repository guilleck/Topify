package es.riberadeltajo.topify;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import es.riberadeltajo.topify.databinding.ActivityMainBinding;
import es.riberadeltajo.topify.models.DarkModeHelper;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;
import es.riberadeltajo.topify.ui.slideshow.PerfilFragment; // Importar PerfilFragment para la interfaz

public class MainActivity extends AppCompatActivity implements PerfilFragment.OnProfilePhotoChangeListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private TextView textViewNombre, textViewEmail;
    private com.google.android.material.imageview.ShapeableImageView imageViewPhoto;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private Toolbar toolbar;
    private Button logoutButton;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarkModeHelper.applySavedTheme(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = binding.appBarMain.toolbar;
        setSupportActionBar(toolbar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        View headerView = navigationView.getHeaderView(0);
        textViewNombre = headerView.findViewById(R.id.textViewNombre);
        textViewEmail = headerView.findViewById(R.id.textViewCorreo);
        imageViewPhoto = headerView.findViewById(R.id.imageViewFoto);
        logoutButton = headerView.findViewById(R.id.buttonLogout);

        loadUserDataInitial();

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cerrarSesion();
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Cargar datos iniciales del usuario
        loadUserDataInitial();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_topCanciones,R.id.nav_buscarCanciones,R.id.nav_listasReproduccion)
                .setOpenableLayout(drawer)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_top) {
                navController.navigate(R.id.nav_topCanciones);
            } else if (id == R.id.nav_buscar) {
                navController.navigate(R.id.nav_buscarCanciones);
            } else if (id == R.id.nav_lista) {
                navController.navigate(R.id.nav_listasReproduccion);
            }else if(id == R.id.nav_usuario){
                navController.navigate(R.id.nav_searchUsersFragment);
            }

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        updateToolbarTitleWithCountry();


    }

    private String decodeBase64(String encodedString) {
        if (encodedString == null || encodedString.isEmpty()) {
            return "";
        }
        try {
            byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
            return new String(decodedBytes, "UTF-8");
        } catch (IllegalArgumentException e) {
            Log.e("Base64", "Error decodificando Base64: " + e.getMessage());
            return encodedString; // Devuelve la cadena original si hay un error de formato Base64
        } catch (UnsupportedEncodingException e) {
            Log.e("Base64", "Error de codificación de caracteres: " + e.getMessage());
            return encodedString; // Devuelve la cadena original si hay un error de codificación
        }
    }

    private void loadUserDataInitial() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String fotoUrl = document.getString("foto"); // Carga la URL de la foto de Firestore
                            String encodedNombre = document.getString("nombre"); // Carga el nombre codificado
                            String encodedEmail = document.getString("email");   // Carga el email codificado

                            // Decodificar el nombre y el email
                            String nombreDecodificado = decodeBase64(encodedNombre);
                            String emailDecodificado = decodeBase64(encodedEmail);

                            textViewNombre.setText(nombreDecodificado != null && !nombreDecodificado.isEmpty() ? nombreDecodificado : "Usuario"); // Establece el nombre decodificado
                            textViewEmail.setText(emailDecodificado != null && !emailDecodificado.isEmpty() ? emailDecodificado : user.getEmail()); // Establece el email decodificado

                            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(fotoUrl) // Glide cargará la URL (local o externa)
                                        .placeholder(R.drawable.usuario)
                                        .error(R.drawable.usuario)
                                        .into(imageViewPhoto);
                            } else {
                                imageViewPhoto.setImageResource(R.drawable.usuario); // Imagen por defecto si no hay foto
                            }

                        } else {
                            textViewNombre.setText("Usuario"); // Establece un nombre por defecto
                            textViewEmail.setText(user.getEmail()); // Establece el email del usuario de Firebase
                            imageViewPhoto.setImageResource(R.drawable.usuario); // Si el documento no existe
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Error al cargar datos iniciales del usuario: " + e.getMessage());
                        textViewNombre.setText("Usuario"); // Establece un nombre por defecto en caso de error
                        textViewEmail.setText(user.getEmail()); // Establece el email del usuario de Firebase en caso de error
                        imageViewPhoto.setImageResource(R.drawable.usuario); // Error, muestra por defecto
                    });
        } else {
            textViewNombre.setText("Invitado"); // Establece "Invitado" si no hay usuario logueado
            textViewEmail.setText(""); // Establece un email vacío si no hay usuario logueado
            imageViewPhoto.setImageResource(R.drawable.usuario); // No hay usuario logeado
        }
    }

    @Override
    public void onProfilePhotoChanged(String newPhotoUrl) {
        if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(newPhotoUrl)
                    .placeholder(R.drawable.usuario)
                    .error(R.drawable.usuario)
                    .into(imageViewPhoto);
        } else {
            imageViewPhoto.setImageResource(R.drawable.usuario);
        }
    }

    @Override
    public void onProfilePhotoDeleted() {
        imageViewPhoto.setImageResource(R.drawable.usuario);
    }


    private void cerrarSesion() {
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Sesión cerrada de Google", Toast.LENGTH_SHORT).show();
                    if (auth != null) {
                        auth.signOut();
                        Toast.makeText(this, "Sesión cerrada de Firebase", Toast.LENGTH_SHORT).show();
                    }
                    irAlLogin();
                } else {
                    Toast.makeText(this, "Error al cerrar sesión de Google.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (auth != null) {
                auth.signOut();
                Toast.makeText(this, "Sesión cerrada de Firebase", Toast.LENGTH_SHORT).show();
            }
            irAlLogin();
        }
    }

    private void irAlLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        this.finish();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        if (id == R.id.action_settings) {
            navController.navigate(R.id.nav_setting);
            return true;
        }else if(id == R.id.action_perfil){
            navController.navigate(R.id.nav_perfil);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private String getCountryName(String countryCode) {
        Locale locale = new Locale("", countryCode);
        return locale.getDisplayCountry(Locale.getDefault());
    }

    public void updateToolbarTitleWithCountry() {
        String countryCode = Locale.getDefault().getCountry();
        String countryName = getCountryName(countryCode);
        if (toolbar != null) {
            toolbar.setTitle("Top Canciones (" + countryName + ")");
        }
    }
}