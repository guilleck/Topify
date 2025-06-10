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
import android.util.Base64;
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
import com.google.firebase.firestore.FieldValue; // Importar FieldValue para arrayRemove
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.UserAdapter; // Asegúrate de importar UserAdapter
import es.riberadeltajo.topify.models.User; // Asegúrate de que tu modelo User esté en esta ruta

public class PerfilFragment extends Fragment {

    // 1. Interfaz de Callback para comunicar con MainActivity
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
    private UserAdapter friendAdapter; // Se sigue usando UserAdapter
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

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean storageGranted = result.getOrDefault(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE,
                            false
                    );

                    if (cameraGranted != null && cameraGranted && storageGranted != null && storageGranted) {
                        // Asegurarse de que el diálogo se muestre si no está ya visible (para evitar problemas de ciclo de vida)
                        if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                            currentEditPhotoDialog.show();
                        }
                        mostrarDialogoSeleccionarFotoOrigen();
                    } else {
                        Toast.makeText(getContext(), "Permisos de cámara y almacenamiento son necesarios para esta función.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success) {
                        if (currentPhotoUri != null) {
                            String photoUriString = saveImageToInternalStorageAndGetUri(currentPhotoUri);
                            if(photoUriString != null) {
                                updateProfilePhotoInFirestore(photoUriString);
                            } else {
                                Toast.makeText(getContext(), "Error al guardar la foto tomada.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Error al obtener la URI de la foto.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Foto no tomada o cancelada.", Toast.LENGTH_SHORT).show();
                    }
                    // Volver a mostrar el diálogo si estaba oculto por el launcher
                    if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                        currentEditPhotoDialog.show();
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        String photoUriString = saveImageToInternalStorageAndGetUri(uri);
                        if (photoUriString != null) {
                            updateProfilePhotoInFirestore(photoUriString);
                        } else {
                            Toast.makeText(getContext(), "Error al procesar la imagen de la galería.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Selección de imagen cancelada.", Toast.LENGTH_SHORT).show();
                    }
                    // Volver a mostrar el diálogo si estaba oculto por el launcher
                    if (currentEditPhotoDialog != null && !currentEditPhotoDialog.isShowing()) {
                        currentEditPhotoDialog.show();
                    }
                }
        );
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
            return encodedString; // En caso de error, devuelve la cadena original
        } catch (UnsupportedEncodingException e) {
            Log.e("Base64Decoder", "Error de codificación de caracteres: " + e.getMessage());
            return encodedString; // En caso de error, devuelve la cadena original
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);

        imageViewProfile = root.findViewById(R.id.imageViewProfile);
        textViewChangeDeletePhoto = root.findViewById(R.id.textViewChangeDeletePhoto);
        textViewUserName = root.findViewById(R.id.textViewUserName);
        textViewUserEmail = root.findViewById(R.id.textViewUserEmail); // Asegúrate de que este ID sea correcto en tu layout fragment_perfil.xml
        buttonEditProfile = root.findViewById(R.id.buttonEditProfile);

        rvFriends = root.findViewById(R.id.rvFriends); // Asegúrate que este ID exista en fragment_perfil.xml
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        // ** Importante: Instanciar UserAdapter para amigos con el onDeleteClickListener **
        // Esto hará que UserAdapter infle R.layout.item_user_profile, que contiene ivDeleteFriend.
        friendAdapter = new UserAdapter(friendList,
                // OnItemClickListener para navegar al perfil del amigo
                friend -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", friend.getId());
                    Navigation.findNavController(root).navigate(R.id.userProfileFragment, bundle);
                },
                // OnFriendDeleteListener para la lógica de eliminación
                friendToDelete -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Eliminar Amigo")
                            .setMessage("¿Estás seguro de que quieres eliminar a " + friendToDelete.getNombre() + " de tus amigos?")
                            .setPositiveButton("Sí", (dialog, which) -> {
                                deleteFriend(friendToDelete);
                            })
                            .setNegativeButton("No", null)
                            .show();
                });
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
                        List<String> friendIds = (List<String>) documentSnapshot.get("friends");
                        friendList.clear(); // Limpiar la lista antes de añadir nuevos amigos

                        if (friendIds != null && !friendIds.isEmpty()) {
                            // Para cada ID de amigo, obtener sus datos completos
                            for (String friendId : friendIds) {
                                db.collection("usuarios").document(friendId)
                                        .get()
                                        .addOnSuccessListener(friendDoc -> {
                                            if (friendDoc.exists()) {
                                                String encodedNombre = friendDoc.getString("nombre");
                                                String encodedEmail = friendDoc.getString("email");
                                                String foto = friendDoc.getString("foto");

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
                            Log.d("PerfilFragment", "El usuario no tiene amigos.");
                            friendAdapter.notifyDataSetChanged(); // Notificar si la lista está vacía
                        }
                    })
                    .addOnFailureListener(e -> Log.e("PerfilFragment", "Error al cargar la lista de amigos: " + e.getMessage()));
        }
    }

    private void deleteFriend(User friendToDelete) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            String friendId = friendToDelete.getId();

            // Eliminar el amigo de la lista de "friends" del usuario actual
            db.collection("usuarios").document(currentUserId)
                    .update("friends", FieldValue.arrayRemove(friendId))
                    .addOnSuccessListener(aVoid -> {
                        // También eliminar el usuario actual de la lista de "friends" del amigo (relación bidireccional)
                        db.collection("usuarios").document(friendId)
                                .update("friends", FieldValue.arrayRemove(currentUserId))
                                .addOnSuccessListener(aVoid2 -> {
                                    // Actualizar la lista local y notificar al adaptador
                                    friendList.remove(friendToDelete);
                                    friendAdapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(), friendToDelete.getNombre() + " ha sido eliminado de tus amigos.", Toast.LENGTH_SHORT).show();
                                    Log.d("PerfilFragment", "Amigo eliminado de Firestore y localmente: " + friendToDelete.getNombre());
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Error al eliminar la relación de amistad del amigo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("PerfilFragment", "Error al eliminar la relación de amistad del amigo", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al eliminar amigo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PerfilFragment", "Error al eliminar amigo de la lista del usuario", e);
                    });
        } else {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------- Métodos de Gestión de Perfil y Foto (sin cambios) -------------------

    // 1. Cargar datos del perfil del usuario actual (sin cambios)
    private void loadUserProfileData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String encodedUserName = documentSnapshot.getString("nombre");
                            String encodedUserEmail = documentSnapshot.getString("email");
                            String photoUrl = documentSnapshot.getString("foto");

                            textViewUserName.setText(decodeBase64(encodedUserName));
                            textViewUserEmail.setText(decodeBase64(encodedUserEmail));
                            tempProfilePhotoUrl = photoUrl; // Guardar la URL de la foto para el diálogo

                            loadProfileImage(photoUrl);
                        } else {
                            Toast.makeText(getContext(), "Datos de usuario no encontrados.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al cargar datos del usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PerfilFragment", "Error al cargar datos del usuario", e);
                    });
        }
    }

    // 2. Carga la imagen de perfil usando Glide (sin cambios)
    private void loadProfileImage(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.usuario) // Placeholder de imagen
                    .error(R.drawable.usuario)     // Imagen de error
                    .into(imageViewProfile);
        } else {
            imageViewProfile.setImageResource(R.drawable.usuario); // Imagen por defecto
        }
    }

    // 3. Muestra el diálogo para cambiar/eliminar la foto de perfil (sin cambios)
    private void showChangeDeletePhotoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Cambiar o Eliminar Foto");

        // Crear un LinearLayout para los botones
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        Button btnChangePhoto = new Button(getContext());
        btnChangePhoto.setText("Cambiar foto");
        layout.addView(btnChangePhoto);

        Button btnDeletePhoto = new Button(getContext());
        btnDeletePhoto.setText("Eliminar foto");
        layout.addView(btnDeletePhoto);

        builder.setView(layout);

        currentEditPhotoDialog = builder.create(); // Guardar la referencia al diálogo
        currentEditPhotoDialog.show();

        btnChangePhoto.setOnClickListener(v -> {
            currentEditPhotoDialog.dismiss(); // Ocultar el diálogo actual
            verificarYPedirPermisosParaFoto();
        });

        btnDeletePhoto.setOnClickListener(v -> {
            currentEditPhotoDialog.dismiss(); // Ocultar el diálogo actual
            new AlertDialog.Builder(getContext())
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que quieres eliminar tu foto de perfil?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        deleteProfilePhotoFromFirestore();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    // 4. Verifica y pide permisos para acceder a la cámara y almacenamiento (sin cambios)
    private void verificarYPedirPermisosParaFoto() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            mostrarDialogoSeleccionarFotoOrigen();
        }
    }

    // 5. Muestra el diálogo para seleccionar el origen de la foto (cámara, galería, URL) (sin cambios)
    private void mostrarDialogoSeleccionarFotoOrigen() {
        final CharSequence[] options = {"Tomar foto", "Elegir de la galería", "Introducir URL"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccionar Origen de la Foto");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Tomar foto")) {
                    dispatchTakePictureIntent();
                } else if (options[item].equals("Elegir de la galería")) {
                    pickImageLauncher.launch("image/*");
                } else if (options[item].equals("Introducir URL")) {
                    mostrarDialogoIntroducirUrl();
                }
            }
        });
        builder.show();
    }

    // 6. Muestra un diálogo para que el usuario introduzca una URL de imagen (sin cambios)
    private void mostrarDialogoIntroducirUrl() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Introducir URL de la Foto");

        final EditText input = new EditText(getContext());
        input.setHint("http://example.com/imagen.jpg");
        builder.setView(input);

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String imageUrl = input.getText().toString().trim();
                if (!imageUrl.isEmpty()) {
                    updateProfilePhotoInFirestore(imageUrl);
                } else {
                    Toast.makeText(getContext(), "URL no puede estar vacía.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // 7. Crea un archivo temporal para guardar la foto tomada con la cámara (sin cambios)
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // 8. Lanza la intent para tomar una foto con la cámara (sin cambios)
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("PerfilFragment", "Error al crear archivo de foto: " + ex.getMessage());
            }
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "es.riberadeltajo.topify.fileprovider",
                        photoFile
                );
                takePictureLauncher.launch(currentPhotoUri);
            }
        }
    }

    // 9. Guarda la imagen de la URI en el almacenamiento interno y devuelve su URI (sin cambios)
    private String saveImageToInternalStorageAndGetUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File directory = new File(requireContext().getFilesDir(), "profile_photos");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "profile_" + timeStamp + ".jpg";
            File file = new File(directory, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();

            // Retorna la URI interna como String
            return file.toURI().toString();

        } catch (IOException e) {
            Log.e("PerfilFragment", "Error al guardar la imagen en el almacenamiento interno: " + e.getMessage());
            Toast.makeText(getContext(), "Error al guardar la imagen.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // 10. Actualiza la URL de la foto en el documento del usuario en Firestore (sin cambios)
    private void updateProfilePhotoInFirestore(String photoUrl) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            Log.e("PerfilFragment", "Usuario no autenticado, no se puede actualizar la foto en Firestore.");
            return;
        }

        Map<String, Object> photoUpdate = new HashMap<>();
        photoUpdate.put("foto", photoUrl);

        db.collection("usuarios").document(user.getUid())
                .update(photoUpdate)
                .addOnSuccessListener(aVoid -> {
                    tempProfilePhotoUrl = photoUrl; // Actualizar la URL temporal
                    loadProfileImage(photoUrl);
                    if (listener != null) {
                        listener.onProfilePhotoChanged(photoUrl);
                    }
                    Toast.makeText(getContext(), "Foto de perfil actualizada.", Toast.LENGTH_SHORT).show();
                    Log.d("PerfilFragment", "Foto de perfil actualizada en Firestore: " + photoUrl);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al actualizar la foto en Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PerfilFragment", "Error al actualizar la foto en Firestore", e);
                });
    }

    // 11. Elimina la URL de la foto en el documento del usuario en Firestore (sin cambios)
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