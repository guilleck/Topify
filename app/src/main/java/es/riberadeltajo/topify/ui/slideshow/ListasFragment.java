package es.riberadeltajo.topify.ui.slideshow;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.adapter.ListaReproduccionAdapter;
import es.riberadeltajo.topify.models.ListaReproduccion; // Importar tu modelo
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

// ¡IMPORTANTE: Asegúrate de que implementa ambas interfaces!
public class ListasFragment extends Fragment implements ListaReproduccionAdapter.OnListaClickListener, ListaReproduccionAdapter.OnListaLongClickListener {

    private RecyclerView recyclerViewListas;
    private ListaReproduccionAdapter adapter;
    private FloatingActionButton fabNuevaLista;
    private ListaReproduccionViewModel viewModel;

    // Variables para la selección de imagen
    private Uri selectedImageUri;
    private ImageView imageViewDialogCover;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar el ActivityResultLauncher para seleccionar imágenes
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (imageViewDialogCover != null) {
                            Glide.with(this).load(selectedImageUri).into(imageViewDialogCover);
                        }
                    }
                });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_listas, container, false);

        recyclerViewListas = root.findViewById(R.id.recyclerViewListas);
        recyclerViewListas.setLayoutManager(new LinearLayoutManager(getContext()));

        fabNuevaLista = root.findViewById(R.id.fabNuevaLista);

        viewModel = new ViewModelProvider(requireActivity()).get(ListaReproduccionViewModel.class);

        // Observar las listas de reproducción del ViewModel
        viewModel.getListasReproduccion().observe(getViewLifecycleOwner(), listas -> {
            if (adapter == null) {
                // Línea 75: Asegúrate de pasar 'this' para ambos listeners,
                // ya que ListasFragment ahora implementa ambos.
                adapter = new ListaReproduccionAdapter(listas, this, this);
                recyclerViewListas.setAdapter(adapter);
            } else {
                adapter.setListas(listas); // Actualizar los datos existentes en el adaptador
            }
        });

        fabNuevaLista.setOnClickListener(v -> mostrarDialogoNuevaLista());

        return root;
    }

    private void mostrarDialogoNuevaLista() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Crear nueva lista");

        // Layout para el diálogo
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setHint("Nombre de la lista");
        layout.addView(inputNombre);

        // Botón y ImageView para seleccionar la imagen
        Button btnSeleccionarImagen = new Button(getContext());
        btnSeleccionarImagen.setText("Seleccionar Portada");
        layout.addView(btnSeleccionarImagen);

        imageViewDialogCover = new ImageView(getContext());
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(200, 200);
        imageViewDialogCover.setLayoutParams(imageLp);
        imageViewDialogCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageViewDialogCover.setImageResource(R.drawable.musica); // Imagen por defecto
        layout.addView(imageViewDialogCover);

        builder.setView(layout);

        selectedImageUri = null; // Resetear la URI seleccionada

        btnSeleccionarImagen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String nombreLista = inputNombre.getText().toString().trim();
            if (!nombreLista.isEmpty()) {
                viewModel.agregarNuevaLista(nombreLista, selectedImageUri); // Pasar la URI de la imagen
            } else {
                Toast.makeText(getContext(), "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void mostrarDialogoEditarEliminarLista(ListaReproduccion lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Editar o Eliminar Lista");

        // Layout para el diálogo
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        final EditText inputNombre = new EditText(getContext());
        inputNombre.setText(lista.getName());
        layout.addView(inputNombre);

        // Botón y ImageView para seleccionar la imagen
        Button btnSeleccionarImagen = new Button(getContext());
        btnSeleccionarImagen.setText("Cambiar Portada");
        layout.addView(btnSeleccionarImagen);

        imageViewDialogCover = new ImageView(getContext());
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(200, 200);
        imageViewDialogCover.setLayoutParams(imageLp);
        imageViewDialogCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // Cargar la imagen actual de la lista
        if (lista.getImageUrl() != null && !lista.getImageUrl().isEmpty()) {
            Glide.with(this).load(lista.getImageUrl()).into(imageViewDialogCover);
            selectedImageUri = Uri.parse(lista.getImageUrl());
        } else {
            imageViewDialogCover.setImageResource(R.drawable.musica);
            selectedImageUri = null;
        }
        layout.addView(imageViewDialogCover);

        builder.setView(layout);

        btnSeleccionarImagen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        builder.setPositiveButton("Guardar Cambios", (dialog, which) -> {
            String nuevoNombre = inputNombre.getText().toString().trim();
            if (!nuevoNombre.isEmpty()) {
                viewModel.actualizarLista(lista.getId(), nuevoNombre, selectedImageUri); // Pasar la URI de la imagen
            } else {
                Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNeutralButton("Eliminar", (dialog, which) -> {
            // Confirmación antes de eliminar
            new AlertDialog.Builder(getContext())
                    .setTitle("Confirmar Eliminación")
                    .setMessage("¿Estás seguro de que quieres eliminar la lista '" + lista.getName() + "'?")
                    .setPositiveButton("Sí", (dialogConfirm, whichConfirm) -> {
                        viewModel.eliminarLista(lista.getId());
                        Toast.makeText(getContext(), "Lista eliminada.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onListaClick(String nombreLista) {
        // Navegar al fragmento de detalles de la lista
        Bundle bundle = new Bundle();
        bundle.putString("nombreLista", nombreLista);
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_slideshow_to_listaDetalleFragment, bundle);
    }

    @Override
    public void onListaLongClick(ListaReproduccion lista) {
        mostrarDialogoEditarEliminarLista(lista);
    }
}