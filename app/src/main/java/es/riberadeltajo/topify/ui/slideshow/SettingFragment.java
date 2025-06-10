package es.riberadeltajo.topify.ui.slideshow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Importar Button
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Importar FirebaseUser

import java.util.Locale;

import es.riberadeltajo.topify.LoginActivity;
import es.riberadeltajo.topify.MainActivity;
import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.models.DarkModeHelper;

public class SettingFragment extends Fragment {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANGUAGE = "language";

    private Switch switchDarkMode;
    private Spinner spinnerLanguage; // Aunque no lo usas, lo mantengo por si es parte de tu plan futuro.
    private Button btnDeleteAccount; // Declarar el botón

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN);
        auth = FirebaseAuth.getInstance();

        SharedPreferences preferences = requireActivity().getSharedPreferences(PREFS_NAME, 0);

        switchDarkMode = root.findViewById(R.id.switch_dark_mode);
        boolean isDarkMode = DarkModeHelper.isDarkModeEnabled(requireContext());
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DarkModeHelper.setDarkMode(requireContext(), isChecked);
            requireActivity().recreate();
        });

        // Inicializar el botón de eliminar cuenta
        btnDeleteAccount = root.findViewById(R.id.btn_delete_account);

        // Configurar el OnClickListener para el botón de eliminar cuenta
        btnDeleteAccount.setOnClickListener(v -> {
            deleteUserAccount();
        });

        return root;
    }

    private void deleteUserAccount() {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            user.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(requireContext(), "Cuenta eliminada exitosamente.", Toast.LENGTH_SHORT).show();
                                // Cerrar sesión de Google si está autenticado con Google
                                googleSignInClient.signOut();
                                // Redirigir al usuario a la pantalla de inicio de sesión
                                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                requireActivity().finish(); // Cierra la actividad actual
                            } else {
                                // Si la eliminación falla, podría ser necesario volver a autenticar al usuario.
                                // Esto sucede si la sesión del usuario ha caducado.
                                Toast.makeText(requireContext(), "Error al eliminar la cuenta. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
                                // Puedes forzar al usuario a iniciar sesión de nuevo aquí si quieres
                                auth.signOut();
                                googleSignInClient.signOut();
                                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                            }
                        }
                    });
        } else {
            Toast.makeText(requireContext(), "No hay usuario autenticado para eliminar.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources resources = requireActivity().getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}