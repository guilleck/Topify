package es.riberadeltajo.topify.ui.slideshow;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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

import java.io.UnsupportedEncodingException; // Importar para manejo de errores de codificación
import java.util.HashMap;
import java.util.Map;

import es.riberadeltajo.topify.LocationActivity;
import es.riberadeltajo.topify.R;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private EditText editTextName;
    private EditText editTextPhone;
    private EditText editTextAddress;
    private Button buttonChooseLocation;
    private Button buttonSaveProfile;
    private CountryCodePicker countryCodePicker;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> locationLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            FirebaseApp.initializeApp(requireContext());
        }
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
                saveProfileChanges();
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

        final String[] savedAddress = {sharedPreferences.getString("user_address", "Ubicación Actual")};
        editTextAddress.setText(savedAddress[0]);

        // Cargar nombre, teléfono y ubicación desde Firebase
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("usuarios").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Obtener valores codificados de Firebase
                            String firebaseNameEncoded = documentSnapshot.getString("nombre");
                            String firebasePhoneEncoded = documentSnapshot.getString("telefono"); // Asumiendo que 'telefono' está en Firestore
                            String firebaseAddressEncoded = documentSnapshot.getString("ubicacion"); // Asumiendo que 'ubicacion' está en Firestore

                            // Decodificar los valores
                            String firebaseNameDecoded = decodeBase64(firebaseNameEncoded);
                            String firebasePhoneDecoded = decodeBase64(firebasePhoneEncoded);
                            String firebaseAddressDecoded = decodeBase64(firebaseAddressEncoded);


                            // Establecer los valores decodificados en los EditText
                            if (firebaseNameDecoded != null && !firebaseNameDecoded.isEmpty()) {
                                editTextName.setText(firebaseNameDecoded);
                            } else {
                                Log.d(TAG, "El campo 'nombre' decodificado es nulo o vacío en Firestore.");
                                String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
                                editTextName.setText(savedName);
                            }

                            if (firebasePhoneDecoded != null && !firebasePhoneDecoded.isEmpty()) {
                                // Intenta establecer el número decodificado en el CountryCodePicker
                                try {
                                    countryCodePicker.setFullNumber(firebasePhoneDecoded);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error al establecer el número de teléfono decodificado en CountryCodePicker: " + e.getMessage());
                                    editTextPhone.setText(firebasePhoneDecoded); // Si falla, solo establece el texto
                                }
                            }

                            if (firebaseAddressDecoded != null && !firebaseAddressDecoded.isEmpty()) {
                                editTextAddress.setText(firebaseAddressDecoded);
                            }

                        } else {
                            Log.d(TAG, "No se encontró el documento para el UID: " + uid + " en Firestore.");
                            String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
                            editTextName.setText(savedName);
                            savedAddress[0] = sharedPreferences.getString("user_address", "Ubicación Actual");
                            editTextAddress.setText(savedAddress[0]);
                            // También para el teléfono si no se encuentra en Firebase
                            String savedPhone = sharedPreferences.getString("user_phone", "");
                            editTextPhone.setText(savedPhone);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error al cargar datos de Firebase Firestore", e);
                        String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
                        editTextName.setText(savedName);
                        savedAddress[0] = sharedPreferences.getString("user_address", "Ubicación Actual");
                        editTextAddress.setText(savedAddress[0]);
                        String savedPhone = sharedPreferences.getString("user_phone", "");
                        editTextPhone.setText(savedPhone);
                        Toast.makeText(getContext(), "Error al cargar datos de Firebase.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.d(TAG, "Usuario no autenticado, cargando datos de SharedPreferences.");
            String savedName = sharedPreferences.getString("user_name", "Nombre de Usuario Actual");
            editTextName.setText(savedName);
            String savedPhone = sharedPreferences.getString("user_phone", "");
            editTextPhone.setText(savedPhone);
            savedAddress[0] = sharedPreferences.getString("user_address", "Ubicación Actual");
            editTextAddress.setText(savedAddress[0]);
        }
    }


    private void saveProfileChanges() {
        String newUserName = editTextName.getText().toString().trim();
        // Obtener el número de teléfono completo del CountryCodePicker
        String newPhoneNumber = countryCodePicker.getFullNumberWithPlus();
        String newLocation = editTextAddress.getText().toString().trim();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Codificar los datos a Base64 antes de guardar
            String encodedUserName = Base64.encodeToString(newUserName.getBytes(), Base64.DEFAULT);
            String encodedPhoneNumber = Base64.encodeToString(newPhoneNumber.getBytes(), Base64.DEFAULT);
            String encodedLocation = Base64.encodeToString(newLocation.getBytes(), Base64.DEFAULT);

            Map<String, Object> updates = new HashMap<>();
            updates.put("nombre", encodedUserName);
            updates.put("telefono", encodedPhoneNumber);
            updates.put("ubicacion", encodedLocation);

            db.collection("usuarios").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Perfil actualizado con éxito.", Toast.LENGTH_SHORT).show();
                        // Guardar también en SharedPreferences por si acaso para la siguiente carga rápida
                        saveDataToSharedPreferences(newUserName, newPhoneNumber, newLocation, countryCodePicker.getFullNumberWithPlus());
                        Navigation.findNavController(requireView()).popBackStack(); // Vuelve al fragmento anterior
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al actualizar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("EditProfileFragment", "Error al actualizar perfil", e);
                    });
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