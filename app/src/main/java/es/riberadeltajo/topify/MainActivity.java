package es.riberadeltajo.topify;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.Locale;

import es.riberadeltajo.topify.databinding.ActivityMainBinding;
import es.riberadeltajo.topify.models.DarkModeHelper;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private TextView textViewNombre, textViewEmail;
    private com.google.android.material.imageview.ShapeableImageView imageViewPhoto;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private Toolbar toolbar;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarkModeHelper.applySavedTheme(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = binding.appBarMain.toolbar; // Inicializar Toolbar
        setSupportActionBar(toolbar);

        ListaReproduccionViewModel viewModel = new ViewModelProvider(this).get(ListaReproduccionViewModel.class);


        auth = FirebaseAuth.getInstance();


        FirebaseUser user = auth.getCurrentUser();


        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        View headerView = navigationView.getHeaderView(0);
        textViewNombre = headerView.findViewById(R.id.textViewNombre);
        textViewEmail = headerView.findViewById(R.id.textViewCorreo);
        imageViewPhoto = headerView.findViewById(R.id.imageViewFoto);



        if (user != null) {
            boolean esGoogle = false;
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    esGoogle = true;
                    break;
                }
            }

            if (esGoogle) {
                textViewNombre.setText(user.getDisplayName());
                textViewEmail.setText(user.getEmail());

                if (user.getPhotoUrl() != null) {
                    Glide.with(this)
                            .load(user.getPhotoUrl())
                            .into(imageViewPhoto);
                } else {
                    imageViewPhoto.setImageResource(R.drawable.usuario);
                }
            } else {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("usuarios").document(user.getUid())
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                String nombre = document.getString("nombre");
                                String email = document.getString("email");
                                String fotoUrl = document.getString("foto");

                                textViewNombre.setText(nombre != null ? nombre : "Usuario");
                                textViewEmail.setText(email != null ? email : user.getEmail());

                                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                    Glide.with(this)
                                            .load(fotoUrl)
                                            .into(imageViewPhoto);
                                } else {
                                    imageViewPhoto.setImageResource(R.drawable.usuario);
                                }
                            } else {
                                textViewNombre.setText("Usuario");
                                textViewEmail.setText(user.getEmail());
                                imageViewPhoto.setImageResource(R.drawable.usuario);
                            }
                        })
                        .addOnFailureListener(e -> {
                            textViewNombre.setText("Usuario");
                            textViewEmail.setText(user.getEmail());
                            imageViewPhoto.setImageResource(R.drawable.usuario);
                        });
            }
        }




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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        if (id == R.id.action_settings) {
            navController.navigate(R.id.nav_setting);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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