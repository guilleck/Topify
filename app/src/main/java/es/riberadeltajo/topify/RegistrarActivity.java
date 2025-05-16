package es.riberadeltajo.topify;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.riberadeltajo.topify.database.DatabaseHelper;

public class RegistrarActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText editTextNombre, editTextCorreo, editTextPass;
    private Button buttonRegistrar;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrar);

        editTextCorreo = findViewById(R.id.editTextCorreoRegistrar);
        editTextPass = findViewById(R.id.editTextPassRegistrar);
        editTextNombre = findViewById(R.id.editTextNombreRegistrar);
        buttonRegistrar = findViewById(R.id.buttonRegistrar);
        auth = FirebaseAuth.getInstance();

        buttonRegistrar.setOnClickListener(v -> {
            registrarUsuario();
        });
    }

    private void registrarUsuario() {
        String email = editTextCorreo.getText().toString().trim();
        String password = editTextPass.getText().toString().trim();
        String nombre = editTextNombre.getText().toString().trim();

        if (email.isEmpty() ||password.isEmpty() ||nombre.isEmpty()) {
            Toast.makeText(RegistrarActivity.this,"No puede haber ningun campo vacio",Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(RegistrarActivity.this,"El formato de correo es invalido",Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(RegistrarActivity.this,"La constraseña debe tener mas de 6 digitos",Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String uid = usuario.getUid();
                            String emailUser = usuario.getEmail();

                            registrarUsuarioEnBaseDatos(uid, nombre, emailUser);

                            navegarLoginActivity();
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String existingEmail = email;

                            if (existingEmail != null && !existingEmail.isEmpty()) {
                                auth.fetchSignInMethodsForEmail(existingEmail)
                                        .addOnCompleteListener(fetchTask -> {
                                            if (fetchTask.isSuccessful()) {
                                                List<String> signInMethods = fetchTask.getResult().getSignInMethods();
                                                boolean isGoogle = signInMethods != null && signInMethods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD);
                                                boolean isFacebook = signInMethods != null && signInMethods.contains(FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD);

                                                if (isGoogle || isFacebook) {
                                                    Toast.makeText(RegistrarActivity.this, "ESE CORREO YA ESTA REGISTRADO", Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(RegistrarActivity.this, "ESE CORREO YA ESTA REGISTRADO", Toast.LENGTH_LONG).show();
                                                }
                                            } else {
                                                Toast.makeText(RegistrarActivity.this, "ERROR AL VERIFICAR EL CORREO", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(RegistrarActivity.this, "Ese correo ya está registrado. Intenta iniciar sesión.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(RegistrarActivity.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void registrarUsuarioEnBaseDatos(String userId, String name, String email) {
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        dbHelper.insertOrUpdateUser(
                userId,
                name,
                email
        );
    }

    private void navegarLoginActivity() {
        Intent intent = new Intent(RegistrarActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}