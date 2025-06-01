package es.riberadeltajo.topify.ui.slideshow;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Importaciones necesarias para Base64
import android.util.Base64;
import java.io.UnsupportedEncodingException;


// NOTA: Ya no necesitamos importar FirebaseStorage ni StorageReference
// import com.google.firebase.storage.FirebaseStorage;
// import com.google.firebase.storage.StorageReference;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.UserAdapter;
import es.riberadeltajo.topify.models.User;

public class PerfilFragment extends Fragment {

    // 1. Interfaz de Callback para comunicar con MainActivity (se mantiene)
    public interface OnProfilePhotoChangeListener {
        void onProfilePhotoChanged(String newPhotoUrl);
        void onProfilePhotoDeleted();
    }

    private OnProfilePhotoChangeListener listener;

    private ImageView imageViewProfile;
    private TextView textViewChangeDeletePhoto;
    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private Button buttonEditProfile;

    private String currentPhotoPath; // Ruta del archivo temporal de la cámara
    private Uri currentPhotoUri; // URI de la imagen tomada con la cámara

    // Launchers para la gestión de resultados de actividades (permisos, cámara, galería)
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private String tempProfilePhotoUrl; // Guarda la URL temporal de la foto para el diálogo de edición
    private AlertDialog currentEditPhotoDialog; // Referencia al diálogo actual para ocultar/mostrar


    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private RecyclerView rvFriends;
    private UserAdapter friendAdapter;
    private List<User> friendList = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnProfilePhotoChangeListener) {
            listener = (OnProfilePhotoChangeListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnProfilePhotoChangeListener");
        }
    }

    // 3. onDetach para limpiar el listener y evitar fugas de memoria
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // 4. onCreate para inicializar Firebase y los ActivityResultLaunchers
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Firebase (Solo Auth y Firestore)
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        // Launcher para solicitar permisos de la cámara y almacenamiento
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean storageGranted = result.getOrDefault(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE,
                            false
                    );

                    if (cameraGranted != null && cameraGranted && storageGranted != null && storageGranted) {
                        // Si todos los permisos son concedidos, muestra el diálogo de selección de origen
                        if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                            currentEditPhotoDialog.show();
                        }
                        mostrarDialogoSeleccionarFotoOrigen();
                    } else {
                        Toast.makeText(getContext(), "Permisos de cámara y almacenamiento son necesarios para esta función.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Launcher para tomar una foto con la cámara
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success) {
                        if (currentPhotoUri != null) {
                            // ***** CAMBIO AQUÍ: Guarda la foto tomada localmente y actualiza Firestore con su URI local *****
                            String photoUriString = saveImageToInternalStorageAndGetUri(currentPhotoUri);
                            if(photoUriString != null) {
                                updateProfilePhotoInFirestore(photoUriString); // Guarda la URI local en Firestore
                            } else {
                                Toast.makeText(getContext(), "Error al guardar la foto tomada.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Error al obtener la URI de la foto.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Foto no tomada o cancelada.", Toast.LENGTH_SHORT).show();
                    }
                    if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                        currentEditPhotoDialog.show();
                    }
                }
        );

        // Launcher para seleccionar una imagen de la galería
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        // ***** CAMBIO AQUÍ: Guarda la imagen de la galería localmente y actualiza Firestore con su URI local *****
                        String photoUriString = saveImageToInternalStorageAndGetUri(uri);
                        if (photoUriString != null) {
                            updateProfilePhotoInFirestore(photoUriString); // Guarda la URI local en Firestore
                        } else {
                            Toast.makeText(getContext(), "Error al procesar la imagen de la galería.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Selección de imagen cancelada.", Toast.LENGTH_SHORT).show();
                    }
                    if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                        currentEditPhotoDialog.show();
                    }
                }
        );
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

    // 5. onCreateView para inflar el layout e inicializar vistas
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);

        imageViewProfile = root.findViewById(R.id.imageViewProfile);
        textViewChangeDeletePhoto = root.findViewById(R.id.textViewChangeDeletePhoto);
        textViewUserName = root.findViewById(R.id.textViewUserName);
        textViewUserEmail = root.findViewById(R.id.textViewUserEmail);
        buttonEditProfile = root.findViewById(R.id.buttonEditProfile);

        rvFriends = root.findViewById(R.id.rvFriends); // Asegúrate que este ID exista en fragment_perfil.xml
        // Cuando haces clic en un amigo, quieres ir a su UserProfileFragment
        friendAdapter = new UserAdapter(friendList, friend -> {
            // Navegar a UserProfileFragment pasando el ID del amigo
            Bundle bundle = new Bundle();
            bundle.putString("userId", friend.getId());
            Navigation.findNavController(root).navigate(R.id.userProfileFragment, bundle);
        });
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriends.setAdapter(friendAdapter);

        loadUserProfileData();
        loadFriends();

        textViewChangeDeletePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeDeletePhotoDialog();
            }
        });

        buttonEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(root).navigate(R.id.action_nav_perfil_to_editProfileFragment);
            }
        });

        return root;
    }

    private void loadFriends() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Obtener la lista de IDs de amigos del usuario actual
                        List<String> friendIds = (List<String>) documentSnapshot.get("friends");
                        friendList.clear(); // Limpiar la lista antes de añadir nuevos amigos

                        if (friendIds != null && !friendIds.isEmpty()) {
                            // Para cada ID de amigo, obtener sus datos completos
                            for (String friendId : friendIds) {
                                db.collection("usuarios").document(friendId)
                                        .get()
                                        .addOnSuccessListener(friendDoc -> {
                                            if (friendDoc.exists()) {
                                                String encodedNombre = friendDoc.getString("nombre"); // Obtener nombre codificado
                                                String encodedEmail = friendDoc.getString("email");   // Obtener email codificado
                                                String foto = friendDoc.getString("foto");

                                                // Decodificar el nombre y el email del amigo
                                                String nombreDecodificado = decodeBase64(encodedNombre);
                                                String emailDecodificado = decodeBase64(encodedEmail);

                                                User friend = new User(friendDoc.getId(), nombreDecodificado, emailDecodificado, foto);
                                                friendList.add(friend);
                                                friendAdapter.notifyDataSetChanged(); // Notificar al adaptador cada vez que se añade un amigo
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e("PerfilFragment", "Error cargando datos de amigo: " + e.getMessage()));
                            }
                        } else {
                            // Opcional: Mostrar un mensaje si no hay amigos (puedes descomentar la línea de Toast)
                            // Toast.makeText(getContext(), "Aún no tienes amigos.", Toast.LENGTH_SHORT).show();
                            Log.d("PerfilFragment", "El usuario no tiene amigos.");
                        }
                        // La notificación final fuera del bucle ya no es tan crítica si se notifica dentro
                        // friendAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Log.e("PerfilFragment", "Error al cargar la lista de amigos: " + e.getMessage()));
        }
    }

    private void loadUserProfileData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String encodedNombre = document.getString("nombre"); // Cargar nombre codificado
                            String encodedEmail = document.getString("email");   // Cargar email codificado
                            String fotoUrl = document.getString("foto");

                            // Decodificar el nombre y el email
                            String nombreDecodificado = decodeBase64(encodedNombre);
                            String emailDecodificado = decodeBase64(encodedEmail);

                            textViewUserName.setText(nombreDecodificado != null ? nombreDecodificado : "Usuario");
                            textViewUserEmail.setText(emailDecodificado != null ? emailDecodificado : user.getEmail());

                            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                tempProfilePhotoUrl = fotoUrl;
                                loadProfileImage(fotoUrl);
                            } else {
                                imageViewProfile.setImageResource(R.drawable.usuario);
                                tempProfilePhotoUrl = "";
                            }
                        } else {
                            textViewUserName.setText("Usuario");
                            textViewUserEmail.setText(user.getEmail());
                            imageViewProfile.setImageResource(R.drawable.usuario);
                            tempProfilePhotoUrl = "";
                        }
                    })
                    .addOnFailureListener(e -> {
                        textViewUserName.setText("Usuario");
                        textViewUserEmail.setText(user.getEmail());
                        imageViewProfile.setImageResource(R.drawable.usuario);
                        tempProfilePhotoUrl = "";
                        Toast.makeText(getContext(), "Error al cargar datos del usuario.", Toast.LENGTH_SHORT).show();
                        Log.e("PerfilFragment", "Error al cargar datos del usuario: " + e.getMessage());
                    });
        } else {
            textViewUserName.setText("Invitado");
            textViewUserEmail.setText("");
            imageViewProfile.setImageResource(R.drawable.usuario);
            tempProfilePhotoUrl = "";
        }
    }

    // 7. Carga de la imagen de perfil con Glide (sin cambios, ya que Glide maneja URIs locales)
    private void loadProfileImage(String imageUrl) {
        if (getContext() != null && imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl)
                    .into(imageViewProfile);
        } else {
            imageViewProfile.setImageResource(R.drawable.usuario);
        }
    }

    // 8. Diálogo para cambiar o eliminar la foto (sin cambios)
    private void showChangeDeletePhotoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Cambiar o Eliminar Foto");
        String[] options = {"Editar Foto", "Eliminar Foto"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Editar Foto
                        verificarYPedirPermisosParaFoto();
                        break;
                    case 1: // Eliminar Foto
                        deleteProfilePhotoFromFirestore();
                        break;
                }
            }
        });
        builder.show();
    }

    // 9. Verifica y pide permisos (sin cambios)
    private void verificarYPedirPermisosParaFoto() {
        List<String> permissionsToRequest = new ArrayList<>();
        permissionsToRequest.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            mostrarDialogoSeleccionarFotoOrigen();
        } else {
            if (currentEditPhotoDialog != null && currentEditPhotoDialog.isShowing()) {
                currentEditPhotoDialog.hide();
            }
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    // 10. Diálogo para seleccionar el origen de la foto (sin cambios)
    private void mostrarDialogoSeleccionarFotoOrigen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccionar origen de la foto");
        String[] opciones = {"Tomar foto", "Seleccionar de galería", "Introducir URL de foto", "Sin foto"};

        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0: // Tomar foto
                    if (currentEditPhotoDialog != null && currentEditPhotoDialog.isShowing()) {
                        currentEditPhotoDialog.hide();
                    }
                    dispatchTakePictureIntent();
                    break;
                case 1: // Seleccionar de galería
                    if (currentEditPhotoDialog != null && currentEditPhotoDialog.isShowing()) {
                        currentEditPhotoDialog.hide();
                    }
                    pickImageLauncher.launch("image/*");
                    break;
                case 2: // Introducir URL de foto
                    mostrarDialogoIntroducirUrl();
                    break;
                case 3: // Sin foto
                    deleteProfilePhotoFromFirestore();
                    break;
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                currentEditPhotoDialog.show();
            }
        });
        currentEditPhotoDialog = builder.create();
        currentEditPhotoDialog.show();
    }

    // 11. Diálogo para introducir una URL de foto manualmente (sin cambios)
    private void mostrarDialogoIntroducirUrl() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ingresar URL de Foto");

        final EditText input = new EditText(getContext());
        input.setHint("URL de la foto");
        input.setText(tempProfilePhotoUrl);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);

        builder.setPositiveButton("Cargar", (dialog, which) -> {
            String imageUrl = input.getText().toString().trim();
            if (!imageUrl.isEmpty()) {
                updateProfilePhotoInFirestore(imageUrl); // Guarda la URL externa directamente
            } else {
                Toast.makeText(getContext(), "La URL no puede estar vacía", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
            if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                currentEditPhotoDialog.show();
            }
        });
        builder.show();
    }

    // 12. Crea un archivo temporal para la imagen tomada por la cámara (sin cambios)
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("No se pudo acceder al directorio de almacenamiento externo de la aplicación.");
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // 13. Inicia la Intent para tomar una foto con la cámara (sin cambios)
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "Error al crear el archivo de imagen: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PerfilFragment", "Error creating image file for camera", ex);
            }
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile
                );
                takePictureLauncher.launch(currentPhotoUri);
            }
        } else {
            Toast.makeText(getContext(), "No se encontró aplicación de cámara.", Toast.LENGTH_SHORT).show();
        }
    }

    // 14. Guarda la imagen de la galería/cámara en el almacenamiento interno de la app
    // y devuelve su URI local. (sin cambios importantes, ya existía en la versión anterior para no-Storage)
    private String saveImageToInternalStorageAndGetUri(Uri sourceUri) {
        try {
            File destinationFile = createImageFile();
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    return Uri.fromFile(destinationFile).toString();
                }
            }
        } catch (IOException e) {
            Log.e("PerfilFragment", "Error al guardar la imagen en almacenamiento interno: " + e.getMessage());
            Toast.makeText(getContext(), "Error al procesar la imagen.", Toast.LENGTH_SHORT).show();
            return null;
        }
        return null;
    }

    // 15. NO SE UTILIZA Firebase Storage. Se guarda la URI local directamente en Firestore.
    // ***** ESTE ES EL MÉTODO CLAVE QUE HA SIDO MODIFICADO PARA NO USAR STORAGE *****
    private void updateProfilePhotoInFirestore(String photoUrl) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e("PerfilFragment", "Usuario no autenticado, no se puede actualizar la foto en Firestore.");
            return;
        }

        Map<String, Object> photoUpdate = new HashMap<>();
        photoUpdate.put("foto", photoUrl); // El campo 'foto' en tu documento de usuario

        db.collection("usuarios").document(user.getUid())
                .update(photoUpdate)
                .addOnSuccessListener(aVoid -> {
                    tempProfilePhotoUrl = photoUrl;
                    loadProfileImage(photoUrl); // Carga la nueva imagen en el ImageView
                    if (listener != null) {
                        listener.onProfilePhotoChanged(photoUrl); // Notifica a MainActivity
                    }
                    Toast.makeText(getContext(), "Foto de perfil actualizada.", Toast.LENGTH_SHORT).show();
                    Log.d("PerfilFragment", "Foto de perfil actualizada en Firestore con URI local/externa.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al guardar URL de la foto en Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PerfilFragment", "Error al actualizar URL de la foto en Firestore", e);
                });
    }

    // 16. Elimina la URL de la foto en el documento del usuario en Firestore (sin cambios)
    private void deleteProfilePhotoFromFirestore() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            Log.e("PerfilFragment", "Usuario no autenticado, no se puede eliminar la foto de Firestore.");
            return;
        }

        Map<String, Object> photoUpdate = new HashMap<>();
        photoUpdate.put("foto", null);

        db.collection("usuarios").document(user.getUid())
                .update(photoUpdate)
                .addOnSuccessListener(aVoid -> {
                    tempProfilePhotoUrl = "";
                    imageViewProfile.setImageResource(R.drawable.usuario);
                    if (listener != null) {
                        listener.onProfilePhotoDeleted();
                    }
                    Toast.makeText(getContext(), "Foto de perfil eliminada.", Toast.LENGTH_SHORT).show();
                    Log.d("PerfilFragment", "Foto de perfil eliminada de Firestore.");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar la foto de Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PerfilFragment", "Error al eliminar la foto de Firestore", e);
                });
    }
}