package es.riberadeltajo.topify.ui.slideshow;

import android.Manifest;
import android.content.ContentResolver;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
import java.util.Objects;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.ListaReproduccionAdapter;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

public class ListasFragment extends Fragment implements ListaReproduccionAdapter.OnListaClickListener, ListaReproduccionAdapter.OnListaLongClickListener {

    private RecyclerView recyclerViewListas;
    private ListaReproduccionAdapter adapter;
    private FloatingActionButton fabNuevaLista;
    private ListaReproduccionViewModel viewModel;

    private String currentPhotoPath;
    private Uri currentPhotoUri;

    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private String tempPlaylistName;
    private String tempPlaylistPhotoUrl;
    private boolean isEditing = false;

    private AlertDialog currentEditOrCreateDialog;
    private EditText inputFotoUrlInDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean storageGranted = result.getOrDefault(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE,
                            false
                    );

                    if (cameraGranted != null && cameraGranted && storageGranted != null && storageGranted) {
                        if (currentEditOrCreateDialog != null && !currentEditOrCreateDialog.isShowing()) {
                            currentEditOrCreateDialog.show();
                        }
                        // Luego lanzamos el selector de origen de foto si es necesario
                        if (isEditing) {
                            mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, tempPlaylistPhotoUrl, true);
                        } else {
                            mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, tempPlaylistPhotoUrl, false);
                        }
                    } else {
                        Toast.makeText(getContext(), "Permisos de cámara y almacenamiento son necesarios para esta función.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (success) {
                        if (currentPhotoUri != null) {
                            tempPlaylistPhotoUrl = currentPhotoUri.toString();
                            updatePhotoUrlInDialog(tempPlaylistPhotoUrl);
                        } else {
                            Toast.makeText(getContext(), "Error al obtener la URI de la foto.", Toast.LENGTH_SHORT).show();
                            tempPlaylistPhotoUrl = "";
                            updatePhotoUrlInDialog(tempPlaylistPhotoUrl);
                        }
                    } else {
                        Toast.makeText(getContext(), "Foto no tomada o cancelada.", Toast.LENGTH_SHORT).show();
                    }
                    if (currentEditOrCreateDialog != null && !currentEditOrCreateDialog.isShowing()) {
                        currentEditOrCreateDialog.show();
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        String photoUriString = saveImageToInternalStorageAndGetUri(uri);
                        if (photoUriString != null) {
                            tempPlaylistPhotoUrl = photoUriString;
                            updatePhotoUrlInDialog(tempPlaylistPhotoUrl);
                        } else {
                            Toast.makeText(getContext(), "Error al procesar la imagen de la galería.", Toast.LENGTH_SHORT).show();
                            tempPlaylistPhotoUrl = "";
                            updatePhotoUrlInDialog(tempPlaylistPhotoUrl);
                        }
                    } else {
                        Toast.makeText(getContext(), "Selección de imagen cancelada.", Toast.LENGTH_SHORT).show();
                    }
                    if (currentEditOrCreateDialog != null && !currentEditOrCreateDialog.isShowing()) {
                        currentEditOrCreateDialog.show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_listas, container, false);

        recyclerViewListas = root.findViewById(R.id.recyclerViewListas);
        recyclerViewListas.setLayoutManager(new LinearLayoutManager(getContext()));

        fabNuevaLista = root.findViewById(R.id.fabNuevaLista);

        viewModel = new ViewModelProvider(requireActivity()).get(ListaReproduccionViewModel.class);

        viewModel.getListaNombres().observe(getViewLifecycleOwner(), nombres -> {
            Map<String, String> fotosMap = viewModel.getListaFotos().getValue();
            if (fotosMap == null) {
                fotosMap = new HashMap<>();
            }
            adapter = new ListaReproduccionAdapter(nombres, fotosMap, this, this);
            recyclerViewListas.setAdapter(adapter);
        });

        viewModel.getListaFotos().observe(getViewLifecycleOwner(), fotos -> {
            if (adapter != null) {
                adapter.setListaFotos(fotos);
            }
        });

        fabNuevaLista.setOnClickListener(v -> {
            isEditing = false;
            tempPlaylistPhotoUrl = "";
            mostrarDialogoPedirNombreLista();
        });

        return root;
    }

    private void mostrarDialogoPedirNombreLista() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Crear nueva lista");

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setHint("Nombre de la lista");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        inputNombre.setLayoutParams(lp);
        inputNombre.setPadding(50, 20, 50, 20);
        builder.setView(inputNombre);

        builder.setPositiveButton("Siguiente", (dialog, which) -> {
            String nombreLista = inputNombre.getText().toString().trim();
            if (!nombreLista.isEmpty()) {
                tempPlaylistName = nombreLista;
                mostrarDialogoNuevaListaConFoto(tempPlaylistName, tempPlaylistPhotoUrl);
            } else {
                Toast.makeText(getContext(), "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void mostrarDialogoSeleccionarFotoOrigen(String nombreLista, String fotoUrlActual, boolean isEditingMode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccionar foto para '" + nombreLista + "'");
        String[] opciones = {"Tomar foto", "Seleccionar de galería", "Introducir URL de foto", "Sin foto"};

        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0:
                    if (currentEditOrCreateDialog != null && currentEditOrCreateDialog.isShowing()) {
                        currentEditOrCreateDialog.hide();
                    }
                    dispatchTakePictureIntent();
                    break;
                case 1:
                    if (currentEditOrCreateDialog != null && currentEditOrCreateDialog.isShowing()) {
                        currentEditOrCreateDialog.hide();
                    }
                    pickImageLauncher.launch("image/*");
                    break;
                case 2:
                    mostrarDialogoIntroducirUrl(nombreLista, fotoUrlActual, isEditingMode);
                    break;
                case 3:
                    tempPlaylistPhotoUrl = "";
                    updatePhotoUrlInDialog("");
                    break;
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {

        });
        builder.show();
    }

    private void mostrarDialogoIntroducirUrl(String nombreLista, String fotoUrlActual, boolean isEditingMode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("URL de la foto");

        final EditText inputFotoUrl = new EditText(getContext());
        inputFotoUrl.setHint("URL de la foto");
        inputFotoUrl.setText(fotoUrlActual);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        inputFotoUrl.setLayoutParams(lp);
        inputFotoUrl.setPadding(50, 20, 50, 20);
        builder.setView(inputFotoUrl);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String fotoUrl = inputFotoUrl.getText().toString().trim();
            tempPlaylistPhotoUrl = fotoUrl;
            updatePhotoUrlInDialog(tempPlaylistPhotoUrl);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {

        });
        builder.show();
    }

    private void mostrarDialogoNuevaListaConFoto(String nombreLista, String fotoUrl) {
        tempPlaylistPhotoUrl = fotoUrl; // Sincroniza la variable temporal

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirmar nueva lista");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView nombreTv = new TextView(getContext());
        nombreTv.setText("Nombre: " + nombreLista);
        layout.addView(nombreTv);

        TextView fotoTv = new TextView(getContext());
        fotoTv.setText("Foto URL: ");
        layout.addView(fotoTv);

        inputFotoUrlInDialog = new EditText(getContext());
        inputFotoUrlInDialog.setHint("URL de la foto");
        inputFotoUrlInDialog.setText(tempPlaylistPhotoUrl);
        inputFotoUrlInDialog.setEnabled(false);
        layout.addView(inputFotoUrlInDialog);

        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLp.topMargin = 20;
        android.widget.Button cambiarFotoBtn = new android.widget.Button(getContext());
        cambiarFotoBtn.setText("Cambiar Foto");
        cambiarFotoBtn.setLayoutParams(buttonLp);
        cambiarFotoBtn.setOnClickListener(v -> {
            verificarYPedirPermisosParaFoto(false);
        });
        layout.addView(cambiarFotoBtn);

        builder.setView(layout);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            viewModel.agregarNuevaLista(nombreLista, tempPlaylistPhotoUrl);
            Toast.makeText(getContext(), "Lista '" + nombreLista + "' creada.", Toast.LENGTH_SHORT).show();
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            currentEditOrCreateDialog = null;
            inputFotoUrlInDialog = null;
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            currentEditOrCreateDialog = null;
            inputFotoUrlInDialog = null;
        });

        currentEditOrCreateDialog = builder.create();
        currentEditOrCreateDialog.show();
    }


    private void mostrarDialogoEditarLista(String nombreActual, String fotoUrl) {
        tempPlaylistPhotoUrl = fotoUrl;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar lista");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setHint("Nuevo nombre de la lista");
        inputNombre.setText(nombreActual);
        layout.addView(inputNombre);

        inputFotoUrlInDialog = new EditText(getContext());
        inputFotoUrlInDialog.setHint("URL de la foto (opcional)");
        inputFotoUrlInDialog.setText(tempPlaylistPhotoUrl); // Usa la URI temporal
        inputFotoUrlInDialog.setEnabled(false); // No editable
        layout.addView(inputFotoUrlInDialog);

        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLp.topMargin = 20;
        android.widget.Button cambiarFotoBtn = new android.widget.Button(getContext());
        cambiarFotoBtn.setText("Cambiar Foto");
        cambiarFotoBtn.setLayoutParams(buttonLp);
        cambiarFotoBtn.setOnClickListener(v -> {
            isEditing = true;
            verificarYPedirPermisosParaFoto(true);
        });
        layout.addView(cambiarFotoBtn);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = inputNombre.getText().toString().trim();
            String nuevaFotoUrl = tempPlaylistPhotoUrl;

            if (!nuevoNombre.isEmpty()) {
                viewModel.editarLista(nombreActual, nuevoNombre, nuevaFotoUrl);
                Toast.makeText(getContext(), "Lista '" + nuevoNombre + "' actualizada.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            currentEditOrCreateDialog = null;
            inputFotoUrlInDialog = null;
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            currentEditOrCreateDialog = null;
            inputFotoUrlInDialog = null;
        });

        currentEditOrCreateDialog = builder.create();
        currentEditOrCreateDialog.show();
    }

    private void updatePhotoUrlInDialog(String newPhotoUrl) {
        if (inputFotoUrlInDialog != null) {
            inputFotoUrlInDialog.setText(newPhotoUrl);
        }
    }

    @Override
    public void onListaClick(String nombreLista) {
        Bundle bundle = new Bundle();
        bundle.putString("nombreLista", nombreLista);
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_slideshow_to_listaDetalleFragment, bundle);
    }

    @Override
    public void onListaLongClick(String nombreLista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Opciones de la lista");
        String[] opciones = {"Editar", "Eliminar"};
        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0:
                    viewModel.getListaInfo(nombreLista).observe(getViewLifecycleOwner(), listaInfo -> {
                        if (listaInfo != null) {
                            String fotoUrl = listaInfo.get("fotoUrl");
                            tempPlaylistName = nombreLista;
                            tempPlaylistPhotoUrl = fotoUrl;
                            isEditing = true;
                            mostrarDialogoEditarLista(nombreLista, fotoUrl);
                        }
                    });
                    break;
                case 1:
                    mostrarDialogoConfirmarEliminar(nombreLista);
                    break;
            }
        });
        builder.show();
    }

    private void mostrarDialogoConfirmarEliminar(String nombreLista) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar lista")
                .setMessage("¿Estás seguro de que quieres eliminar la lista '" + nombreLista + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.eliminarLista(nombreLista);
                    Toast.makeText(getContext(), "Lista '" + nombreLista + "' eliminada.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void verificarYPedirPermisosParaFoto(boolean isEditingMode) {
        isEditing = isEditingMode;

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
            if (isEditingMode) {
                mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, tempPlaylistPhotoUrl, true);
            } else {
                mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, tempPlaylistPhotoUrl, false);
            }
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

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

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "Error al crear el archivo de imagen: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
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
            Log.e("ListasFragment", "Error al guardar la imagen de la galería: " + e.getMessage());
            return null;
        }
        return null;
    }
}