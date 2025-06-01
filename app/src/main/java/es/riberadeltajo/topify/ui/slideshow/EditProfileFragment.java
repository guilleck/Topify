package es.riberadeltajo.topify.ui.slideshow;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.hbb20.CountryCodePicker;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import es.riberadeltajo.topify.LocationActivity; // Asegúrate de que esta clase exista en este paquete
import es.riberadeltajo.topify.R;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private EditText editTextName;
    private EditText editTextPhone;
    private EditText editTextAddress;
    private Button buttonChooseLocation;
    private Button buttonSaveProfile;
    private CountryCodePicker countryCodePicker;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> locationLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            FirebaseApp.initializeApp(requireContext());
        }
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        editTextName = root.findViewById(R.id.editTextName);
        editTextPhone = root.findViewById(R.id.editTextPhone);
        editTextAddress = root.findViewById(R.id.editTextAddress);
        buttonChooseLocation = root.findViewById(R.id.buttonChooseLocation);
        buttonSaveProfile = root.findViewById(R.id.buttonSaveProfile);
        countryCodePicker = root.findViewById(R.id.countryCodePicker);

        countryCodePicker.registerCarrierNumberEditText(editTextPhone);

        loadExistingProfileData();
        loadSelectedCountryCode(countryCodePicker);

        locationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        String selectedAddress = result.getData().getStringExtra("SELECTED_ADDRESS");
                        if (selectedAddress != null) {
                            editTextAddress.setText(selectedAddress);
                        }
                    }
                }
        );

        buttonChooseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 201);
                } else {
                    Intent intent = new Intent(requireContext(), LocationActivity.class);
                    locationLauncher.launch(intent);
                }
            }
        });

        countryCodePicker.setOnCountryChangeListener(() -> {
            String selectedCountry = countryCodePicker.getSelectedCountryName();
            String selectedCode = countryCodePicker.getSelectedCountryCodeWithPlus();
            Toast.makeText(requireContext(), "Seleccionado: " + selectedCountry + " (" + selectedCode + ")", Toast.LENGTH_SHORT).show();
            saveSelectedCountryCode(selectedCode, selectedCountry);
        });

        buttonSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });

        return root;
    }

    private void loadExistingProfileData() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);

        // Cargar el número de teléfono completo y establecerlo directamente en CountryCodePicker
        String savedFullPhone = sharedPreferences.getString("user_full_phone", "");
        if (!savedFullPhone.isEmpty()) {
            countryCodePicker.setFullNumber(savedFullPhone); // Esto parseará el número y establecerá el código de país y el número
        } else {
            // Si el número completo no está guardado, cargar solo la parte local si está disponible
            String savedPhone = sharedPreferences.getString("user_phone", "");
            editTextPhone.setText(savedPhone);
        }

        String savedAddress = sharedPreferences.getString("user_address", "Ubicación Actual");
        editTextAddress.setText(savedAddress);

        // Cargar nombre desde Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("usuarios").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firebaseName = documentSnapshot.getString("nombre");
                            if (firebaseName != null) {
                                editTextName.setText(firebaseName);
                            } else {
                                Log.d(TAG, "El campo 'nombre' no existe en el documento del usuario en Firestore.");
                                String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
                                editTextName.setText(savedName);
                            }
                        } else {
                            Log.d(TAG, "No se encontró el documento para el UID: " + uid + " en Firestore.");
                            String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
                            editTextName.setText(savedName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error al cargar el nombre de Firebase Firestore", e);
                        String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
                        editTextName.setText(savedName);
                        Toast.makeText(getContext(), "Error al cargar el nombre de Firebase.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.d(TAG, "Usuario no autenticado, cargando nombre de SharedPreferences.");
            String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
            editTextName.setText(savedName);
        }
    }

    private void saveProfileData() {
        String newName = editTextName.getText().toString().trim();
        String newAddress = editTextAddress.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newAddress.isEmpty()) {
            Toast.makeText(getContext(), "La ubicación no puede estar vacía", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullPhoneNumber = countryCodePicker.getFullNumberWithPlus();
        String countryRegionCode = countryCodePicker.getSelectedCountryNameCode();

        if (TextUtils.isEmpty(countryRegionCode)) {
            Toast.makeText(getContext(), "Por favor, seleccione un código de país.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!countryCodePicker.isValidFullNumber()) {
            Toast.makeText(getContext(), "Número de teléfono no válido para la región seleccionada", Toast.LENGTH_LONG).show();
            return;
        }

        String newPhone = editTextPhone.getText().toString().trim();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("usuarios").document(uid)
                    .update("nombre", newName, "telefono", fullPhoneNumber, "ubicacion", newAddress)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Datos de usuario actualizados en Firestore.");
                        saveDataToSharedPreferences(newName, newPhone, newAddress, fullPhoneNumber);
                        Toast.makeText(getContext(), "Perfil guardado con Firebase.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error al actualizar datos en Firestore", e);
                        Toast.makeText(getContext(), "Error al guardar el perfil en Firebase.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.d(TAG, "Usuario no autenticado, guardando solo en SharedPreferences.");
            saveDataToSharedPreferences(newName, newPhone, newAddress, fullPhoneNumber);
            Toast.makeText(getContext(), "Perfil guardado (solo localmente).", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDataToSharedPreferences(String name, String phone, String address, String fullPhone) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_name", name);
        editor.putString("user_phone", phone);
        editor.putString("user_full_phone", fullPhone);
        editor.putString("user_address", address);
        editor.apply();
    }

    private void saveSelectedCountryCode(String code, String country) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("country_code", code)
                .putString("country_name", country)
                .apply();
    }

    private void loadSelectedCountryCode(CountryCodePicker ccp) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE);
        String savedCode = prefs.getString("country_code", null);
        if (savedCode != null) {
            ccp.setCountryForPhoneCode(Integer.parseInt(savedCode.replace("+", "")));
        }
    }

    private boolean isValidPhoneNumber(String fullPhone, String countryRegionCode) {
        if (TextUtils.isEmpty(fullPhone) || TextUtils.isEmpty(countryRegionCode)) {
            return false;
        }

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(fullPhone, countryRegionCode);
            return phoneNumberUtil.isValidNumberForRegion(phoneNumber, countryRegionCode);
        } catch (NumberParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(requireContext(), LocationActivity.class);
                locationLauncher.launch(intent);
            } else {
                Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}