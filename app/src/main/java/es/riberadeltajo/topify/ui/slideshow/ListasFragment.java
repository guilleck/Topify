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
import androidx.lifecycle.Observer;
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

    private String currentPhotoPath; // Para almacenar la ruta de la foto tomada con la cámara
    private Uri currentPhotoUri;    // Para almacenar la URI de la foto tomada con la cámara

    // ActivityResultLauncher para permisos
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    // ActivityResultLauncher para tomar foto con cámara
    private ActivityResultLauncher<Uri> takePictureLauncher;

    // ActivityResultLauncher para seleccionar de galería
    private ActivityResultLauncher<String> pickImageLauncher;

    // Variables para el diálogo de edición/creación
    private String tempPlaylistName;
    private String tempPlaylistPhotoUrl;
    private boolean isEditing = false; // Indica si estamos en modo edición o creación

    private AlertDialog currentAlertDialog; // Añadir esta línea para gestionar los diálogos

    // Método para cerrar el diálogo actual
    private void dismissCurrentDialog() {
        if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
            currentAlertDialog.dismiss();
            currentAlertDialog = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar ActivityResultLauncher para permisos
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean storageGranted = result.getOrDefault(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE,
                            false
                    );

                    if (cameraGranted != null && cameraGranted && storageGranted != null && storageGranted) {
                        // Permisos concedidos, proceder con la acción deseada (ej. abrir selector de origen de foto)
                        if (isEditing) {
                            // Si estamos editando y los permisos se conceden,
                            // volvemos a mostrar el diálogo de selección de origen
                            mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, tempPlaylistPhotoUrl, true);
                        } else {
                            mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, "", false);
                        }
                    } else {
                        Toast.makeText(getContext(), "Permisos de cámara y almacenamiento son necesarios para esta función.", Toast.LENGTH_LONG).show();
                        // Si los permisos no se conceden, no se debería volver a abrir el diálogo de edición/creación
                        // Simplemente informar al usuario y esperar a que intente de nuevo.
                        // Limpiar el estado si no se pueden obtener los permisos para evitar un bucle
                        tempPlaylistName = null;
                        tempPlaylistPhotoUrl = null;
                        isEditing = false;
                    }
                }
        );

        // Inicializar ActivityResultLauncher para tomar foto
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    dismissCurrentDialog(); // Cerrar cualquier diálogo existente antes de mostrar el nuevo
                    if (success && currentPhotoUri != null) {
                        // La foto se ha tomado correctamente, ahora podemos mostrar el diálogo con la URI
                        if (isEditing) {
                            mostrarDialogoEditarLista(tempPlaylistName, currentPhotoUri.toString());
                        } else {
                            mostrarDialogoNuevaListaConFoto(tempPlaylistName, currentPhotoUri.toString());
                        }
                    } else {
                        Toast.makeText(getContext(), "Foto no tomada o error al obtener URI.", Toast.LENGTH_SHORT).show();
                        // Si la foto no se tomó o hubo un error, no vuelvas a abrir el diálogo.
                        // Limpiar el estado para evitar un bucle
                        tempPlaylistName = null;
                        tempPlaylistPhotoUrl = null;
                        isEditing = false;
                    }
                }
        );

        // Inicializar ActivityResultLauncher para seleccionar de galería
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    dismissCurrentDialog(); // Cerrar cualquier diálogo existente antes de mostrar el nuevo
                    if (uri != null) {
                        String photoUriString = saveImageToInternalStorageAndGetUri(uri);
                        if (photoUriString != null) {
                            // Imagen seleccionada de la galería y procesada correctamente
                            if (isEditing) {
                                mostrarDialogoEditarLista(tempPlaylistName, photoUriString);
                            } else {
                                mostrarDialogoNuevaListaConFoto(tempPlaylistName, photoUriString);
                            }
                        } else {
                            Toast.makeText(getContext(), "Error al procesar la imagen de la galería.", Toast.LENGTH_SHORT).show();
                            // Si hay un error al procesar, no vuelvas a abrir el diálogo.
                            // Limpiar el estado para evitar un bucle
                            tempPlaylistName = null;
                            tempPlaylistPhotoUrl = null;
                            isEditing = false;
                        }
                    } else {
                        Toast.makeText(getContext(), "Selección de imagen cancelada.", Toast.LENGTH_SHORT).show();
                        // Si la selección se cancela, no vuelvas a abrir el diálogo.
                        // Limpiar el estado para evitar un bucle
                        tempPlaylistName = null;
                        tempPlaylistPhotoUrl = null;
                        isEditing = false;
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

        // Observar nombres de lista y fotos
        viewModel.getListaNombres().observe(getViewLifecycleOwner(), nombres -> {
            // Se necesita el mapa de fotos para el adaptador
            Map<String, String> fotosMap = viewModel.getListaFotos().getValue();
            if (fotosMap == null) {
                fotosMap = new HashMap<>(); // Asegurarse de que no sea null
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
            tempPlaylistName = null; // Limpiar el estado temporal para una nueva creación
            tempPlaylistPhotoUrl = null;
            mostrarDialogoPedirNombreLista();
        });

        return root;
    }

    private void mostrarDialogoPedirNombreLista() {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Crear nueva lista");

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setHint("Nombre de la lista");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        inputNombre.setLayoutParams(lp);
        inputNombre.setPadding(50, 20, 50, 20); // Añadir padding
        builder.setView(inputNombre);

        builder.setPositiveButton("Siguiente", (dialog, which) -> {
            String nombreLista = inputNombre.getText().toString().trim();
            if (!nombreLista.isEmpty()) {
                tempPlaylistName = nombreLista;
                // tempPlaylistPhotoUrl se mantiene vacío ya que es una nueva lista
                verificarYPedirPermisosParaFoto(false); // Es para crear nueva lista
            } else {
                Toast.makeText(getContext(), "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
            // Limpiar variables temporales al cancelar la creación
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            isEditing = false;
        });

        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }


    private void mostrarDialogoSeleccionarFotoOrigen(String nombreLista, String fotoUrlActual, boolean isEditingMode) {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccionar foto para '" + nombreLista + "'");
        String[] opciones = {"Tomar foto", "Seleccionar de galería", "Introducir URL de foto", "Sin foto"};

        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0: // Tomar foto
                    dispatchTakePictureIntent();
                    break;
                case 1: // Seleccionar de galería
                    pickImageLauncher.launch("image/*");
                    break;
                case 2: // Introducir URL de foto
                    mostrarDialogoIntroducirUrl(nombreLista, fotoUrlActual, isEditingMode);
                    break;
                case 3: // Sin foto
                    if (isEditingMode) {
                        mostrarDialogoEditarLista(nombreLista, "");
                    } else {
                        mostrarDialogoNuevaListaConFoto(nombreLista, "");
                    }
                    break;
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            // Si se cancela aquí, volvemos al diálogo de nombre original o edición
            dialog.cancel(); // Cerrar este diálogo
            if (isEditingMode) {
                // Volver al diálogo de edición con la foto original si es una edición
                mostrarDialogoEditarLista(nombreLista, fotoUrlActual);
            } else {
                // Si es modo creación, al cancelar la selección de foto, volvemos al diálogo de confirmación de nueva lista
                // para que el usuario pueda crearla sin foto o intentar seleccionar una foto de nuevo.
                // Limpiar el estado si el usuario cancela completamente la creación
                tempPlaylistName = null;
                tempPlaylistPhotoUrl = null;
                isEditing = false;
            }
        });
        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }

    private void mostrarDialogoIntroducirUrl(String nombreLista, String fotoUrlActual, boolean isEditingMode) {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("URL de la foto");

        final EditText inputFotoUrl = new EditText(getContext());
        inputFotoUrl.setHint("URL de la foto");
        inputFotoUrl.setText(fotoUrlActual);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        inputFotoUrl.setLayoutParams(lp);
        inputFotoUrl.setPadding(50, 20, 50, 20); // Añadir padding
        builder.setView(inputFotoUrl);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String fotoUrl = inputFotoUrl.getText().toString().trim();
            if (isEditingMode) {
                mostrarDialogoEditarLista(nombreLista, fotoUrl);
            } else {
                mostrarDialogoNuevaListaConFoto(nombreLista, fotoUrl);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel(); // Cerrar este diálogo
            // Volver al diálogo de selección de origen
            mostrarDialogoSeleccionarFotoOrigen(nombreLista, fotoUrlActual, isEditingMode);
        });
        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }

    private void mostrarDialogoNuevaListaConFoto(String nombreLista, String fotoUrl) {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirmar nueva lista");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView nombreTv = new TextView(getContext());
        nombreTv.setText("Nombre: " + nombreLista);
        layout.addView(nombreTv);

        TextView fotoTv = new TextView(getContext());
        fotoTv.setText("Foto URL: " + (fotoUrl.isEmpty() ? "Ninguna" : fotoUrl));
        layout.addView(fotoTv);

        builder.setView(layout);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            viewModel.agregarNuevaLista(nombreLista, fotoUrl);
            Toast.makeText(getContext(), "Lista '" + nombreLista + "' creada.", Toast.LENGTH_SHORT).show();
            // Limpiar variables temporales y asegurar el cierre del diálogo
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            isEditing = false;
            dialog.dismiss(); // Asegura que este diálogo se cierre
        });
        builder.setNegativeButton("Volver", (dialog, which) -> {
            dialog.cancel(); // Cerrar este diálogo
            // Si el usuario quiere volver, le damos la opción de cambiar la foto o el nombre
            mostrarDialogoSeleccionarFotoOrigen(nombreLista, fotoUrl, false);
        });
        builder.setNeutralButton("Cancelar", (dialog, which) -> {
            dialog.cancel(); // Cerrar este diálogo
            // Limpiar variables temporales si el usuario cancela completamente la creación
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            isEditing = false;
        });

        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }


    private void mostrarDialogoEditarLista(String nombreActual, String fotoUrl) {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar lista");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setHint("Nuevo nombre de la lista");
        inputNombre.setText(nombreActual);
        layout.addView(inputNombre);

        // Usamos un TextView para mostrar la URL de la foto, ya que no queremos que sea editable directamente
        TextView fotoUrlDisplay = new TextView(getContext());
        fotoUrlDisplay.setText("Foto URL: " + (fotoUrl == null || fotoUrl.isEmpty() ? "Ninguna" : fotoUrl)); // Manejar null
        layout.addView(fotoUrlDisplay);

        // Botón para cambiar la foto
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLp.topMargin = 20;
        android.widget.Button cambiarFotoBtn = new android.widget.Button(getContext());
        cambiarFotoBtn.setText("Cambiar Foto");
        cambiarFotoBtn.setLayoutParams(buttonLp);
        cambiarFotoBtn.setOnClickListener(v -> {
            // Guardamos el estado actual para volver si se cancela la selección de foto
            tempPlaylistName = nombreActual;
            tempPlaylistPhotoUrl = fotoUrl; // La fotoUrl que viene de la selección actual
            isEditing = true;
            mostrarDialogoSeleccionarFotoOrigen(nombreActual, fotoUrl, true);
        });
        layout.addView(cambiarFotoBtn);

        builder.setView(layout);

        final String finalFotoUrl = fotoUrl; // Creamos una variable final para usar en el listener

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = inputNombre.getText().toString().trim();

            if (!nuevoNombre.isEmpty()) {
                // Usamos la URL que llegó al diálogo, que es la correcta
                viewModel.editarLista(nombreActual, nuevoNombre, finalFotoUrl);
                Toast.makeText(getContext(), "Lista '" + nuevoNombre + "' actualizada.", Toast.LENGTH_SHORT).show();
                // Limpiar variables temporales y asegurar el cierre del diálogo
                tempPlaylistName = null;
                tempPlaylistPhotoUrl = null;
                isEditing = false;
                dialog.dismiss(); // Asegura que este diálogo se cierre
            } else {
                Toast.makeText(getContext(), "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel(); // Cerrar este diálogo
            // Limpiar variables temporales al cancelar la edición
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            isEditing = false;
        });

        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }

    @Override
    public void onListaClick(String nombreLista) {
        Bundle bundle = new Bundle();
        bundle.putString("nombreLista", nombreLista);
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_slideshow_to_listaDetalleFragment, bundle);
    }

    @Override
    public void onListaLongClick(String nombreLista) {
        isEditing = true;
        tempPlaylistName = nombreLista;

        // Crear una referencia final al observador para poder eliminarlo después
        final Observer<Map<String, String>> observer = new Observer<Map<String, String>>() {
            @Override
            public void onChanged(Map<String, String> listaInfo) {
                if (listaInfo != null && listaInfo.containsKey("fotoUrl")) {
                    tempPlaylistPhotoUrl = listaInfo.get("fotoUrl");
                } else {
                    tempPlaylistPhotoUrl = null; // O una URL por defecto si no hay foto
                }
                // Una vez que tenemos la info, mostramos el diálogo de opciones.
                mostrarDialogoOpcionesLista(nombreLista);

                // Eliminar el observador inmediatamente después de que se dispare una vez
                viewModel.getListaInfo(nombreLista).removeObserver(this);
            }
        };

        viewModel.getListaInfo(nombreLista).observe(getViewLifecycleOwner(), observer);
    }

    private void mostrarDialogoOpcionesLista(String nombreLista) {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente antes de mostrar el nuevo
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Opciones de la lista");
        String[] opciones = {"Editar", "Eliminar"};
        builder.setItems(opciones, (dialog, which) -> {
            switch (which) {
                case 0: // Editar
                    // Usar las variables temporales establecidas en onListaLongClick
                    mostrarDialogoEditarLista(tempPlaylistName, tempPlaylistPhotoUrl);
                    break;
                case 1: // Eliminar
                    mostrarDialogoConfirmarEliminar(nombreLista);
                    break;
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel(); // Cerrar este diálogo
            // Limpiar el estado al cancelar las opciones de edición
            tempPlaylistName = null;
            tempPlaylistPhotoUrl = null;
            isEditing = false;
        });
        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }

    private void mostrarDialogoConfirmarEliminar(String nombreLista) {
        dismissCurrentDialog(); // Cerrar cualquier diálogo existente
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle("Eliminar lista")
                .setMessage("¿Estás seguro de que quieres eliminar la lista '" + nombreLista + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.eliminarLista(nombreLista);
                    Toast.makeText(getContext(), "Lista '" + nombreLista + "' eliminada.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss(); // Asegura que este diálogo se cierre
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.cancel(); // Cerrar este diálogo
                });
        currentAlertDialog = builder.show(); // Asignar el diálogo actual
    }

    private void verificarYPedirPermisosParaFoto(boolean isEditingMode) {
        // No es necesario llamar a dismissCurrentDialog() aquí,
        // ya que este método prepara para lanzar una actividad (permisos/cámara),
        // y el diálogo actual ya se habrá cerrado en la llamada anterior
        // (ej. desde mostrarDialogoPedirNombreLista o mostrarDialogoSeleccionarFotoOrigen)
        isEditing = isEditingMode; // Establecer el modo antes de solicitar permisos

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
            // Permisos ya concedidos, proceder directamente al diálogo de selección de origen
            if (isEditingMode) {
                mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, tempPlaylistPhotoUrl, true);
            } else {
                mostrarDialogoSeleccionarFotoOrigen(tempPlaylistName, "", false);
            }
        } else {
            // Solicitar permisos
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    private File createImageFile() throws IOException {
        // Crea un nombre de archivo de imagen único
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefijo */
                ".jpg",         /* sufijo */
                storageDir      /* directorio */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Asegúrate de que haya una actividad de cámara para manejar el intent
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
            File destinationFile = createImageFile(); // Reutilizamos el método para crear un archivo temporal
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    // Retornamos la URI del archivo temporal
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