package es.riberadeltajo.topify.ui.slideshow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.firebase.auth.FirebaseAuth;

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
    private Spinner spinnerLanguage;
    private Button logoutButton;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        logoutButton = root.findViewById(R.id.buttonLogout);
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

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cerrarSesion();
            }
        });

        spinnerLanguage = root.findViewById(R.id.spinner_language);
        String[] languages = { getString(R.string.spanish), getString(R.string.english) };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        // Idioma guardado
        String savedLang = preferences.getString(KEY_LANGUAGE, "es");
        spinnerLanguage.setSelection(savedLang.equals("es") ? 0 : 1);

        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = (position == 0) ? "es" : "en";
                if (!selectedLang.equals(savedLang)) {
                    preferences.edit().putString(KEY_LANGUAGE, selectedLang).apply();
                    setLocale(selectedLang);
                    requireActivity().recreate(); // Recarga interfaz
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        return root;
    }

    private void cerrarSesion() {
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Sesión cerrada de Google", Toast.LENGTH_SHORT).show();
                if (auth != null) {
                    auth.signOut();
                    Toast.makeText(getContext(), "Sesión cerrada de Firebase", Toast.LENGTH_SHORT).show();
                }
                irAlLogin();
            } else {
                Toast.makeText(getContext(), "Error al cerrar sesión de Google.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void irAlLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
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