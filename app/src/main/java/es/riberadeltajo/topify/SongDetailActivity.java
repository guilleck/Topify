package es.riberadeltajo.topify;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import es.riberadeltajo.topify.adapter.CommentAdapter;
import es.riberadeltajo.topify.models.Comment;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;

public class SongDetailActivity extends AppCompatActivity {

    private ImageView imageViewCoverDetail;
    private TextView textViewTitleDetail;
    private TextView textViewArtistDetail;
    private TextView textViewDurationDetail;
    private Button buttonPlayPreview;
    private Button buttonAddToPlaylist;
    private TextView textViewPreviewUrl;

    private SeekBar seekBarProgress;
    private TextView textViewCurrentTime;

    private EditText editTextComment;
    private Button buttonPostComment;
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentsList;
    private ImageView buttonReloadComments; // Declaración del nuevo botón

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    private DeezerTrackResponse.Track currentSong;
    private ListaReproduccionViewModel viewModel;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration commentsListenerRegistration;

    private boolean isDarkMode = false;
    private long songDeezerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        viewModel = new ViewModelProvider(this).get(ListaReproduccionViewModel.class);
        if (viewModel == null) {
            Log.e("SongDetailActivity", "ERROR: ViewModel es NULL después de la inicialización en onCreate.");
        } else {
            Log.d("SongDetailActivity", "ViewModel inicializado correctamente en onCreate.");
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        imageViewCoverDetail = findViewById(R.id.imageViewCoverDetail);
        textViewTitleDetail = findViewById(R.id.textViewTitleDetail);
        textViewArtistDetail = findViewById(R.id.textViewArtistDetail);
        textViewDurationDetail = findViewById(R.id.textViewDurationDetail);
        buttonPlayPreview = findViewById(R.id.buttonPlayPreview);
        buttonAddToPlaylist = findViewById(R.id.buttonAddToPlaylist);
        textViewPreviewUrl = findViewById(R.id.textViewPreviewUrl);

        seekBarProgress = findViewById(R.id.seekBarProgress);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);


        editTextComment = findViewById(R.id.editTextComment);
        buttonPostComment = findViewById(R.id.buttonPostComment);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        buttonReloadComments = findViewById(R.id.buttonReloadComments); // Inicialización del botón

        commentsList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentsList, isDarkMode);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);

        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("title");
            String artistName = intent.getStringExtra("artist");
            String coverUrl = intent.getStringExtra("coverUrl");
            int duration = intent.getIntExtra("duration", 0);
            String previewUrl = intent.getStringExtra("previewUrl");
            songDeezerId = intent.getLongExtra("deezerId", 0);

            currentSong = new DeezerTrackResponse.Track();
            currentSong.title = title;
            currentSong.duration = duration;
            currentSong.preview = previewUrl;
            currentSong.deezer_id = songDeezerId;

            currentSong.artist = new DeezerTrackResponse.Track.Artist();
            currentSong.artist.name = artistName;

            currentSong.album = new DeezerTrackResponse.Track.Album();
            currentSong.album.cover_big = coverUrl;

            Log.d("SongDetailDebug", "onCreate: Song Deezer ID recibido: " + currentSong.deezer_id);


            textViewTitleDetail.setText(title);
            textViewArtistDetail.setText(artistName);
            textViewDurationDetail.setText(formatDuration(duration));
            textViewPreviewUrl.setText(previewUrl);
            Glide.with(this).load(coverUrl).into(imageViewCoverDetail);

            textViewCurrentTime.setText("0:00");

            buttonPlayPreview.setOnClickListener(v -> playPreview(previewUrl));

            buttonAddToPlaylist.setOnClickListener(v -> {
                if (currentSong != null) {
                    showAddToPlaylistDialog(currentSong);
                } else {
                    Toast.makeText(this, "No se pudo obtener la información de la canción.", Toast.LENGTH_SHORT).show();
                    Log.e("SongDetailActivity", "currentSong es null al intentar añadir a la lista.");
                }
            });

            buttonPostComment.setOnClickListener(v -> postComment());
            // Asignar OnClickListener al nuevo botón de recarga
            buttonReloadComments.setOnClickListener(v -> {
                if (currentSong != null) {
                    // Si ya hay un listener, lo removemos antes de añadir uno nuevo
                    if (commentsListenerRegistration != null) {
                        commentsListenerRegistration.remove();
                        Log.d("SongDetailDebug", "onCreate: Listener de comentarios existente desregistrado antes de recargar.");
                    }
                    loadComments(currentSong.deezer_id);
                    Toast.makeText(this, "Comentarios recargados", Toast.LENGTH_SHORT).show();
                    Log.d("SongDetailDebug", "onCreate: Recargando comentarios por petición del usuario.");
                } else {
                    Toast.makeText(this, "No se puede recargar, información de la canción no disponible.", Toast.LENGTH_SHORT).show();
                    Log.w("SongDetailDebug", "onCreate: currentSong es null, no se pueden recargar comentarios.");
                }
            });


            seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    }
                    textViewCurrentTime.setText(formatDuration(progress / 1000));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    handler.removeCallbacks(updateSeekBar);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mediaPlayer != null && isPlaying) {
                        handler.post(updateSeekBar);
                    }
                }
            });

            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && isPlaying) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        seekBarProgress.setProgress(currentPosition);
                        textViewCurrentTime.setText(formatDuration(currentPosition / 1000));
                        handler.postDelayed(this, 1000);
                    }
                }
            };

            if (isDarkMode) {
                applyDarkModeStyleToButton(buttonPlayPreview);
                applyDarkModeStyleToButton(buttonAddToPlaylist);
                applyDarkModeStyleToEditText(editTextComment);
                // Si quieres aplicar un estilo al ImageButton para Dark Mode, hazlo aquí
                // Por ejemplo, buttonReloadComments.setColorFilter(Color.WHITE);
            }
        }
    }

    private void applyDarkModeStyleToButton(Button button) {
        button.setTextColor(Color.WHITE);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(4, Color.WHITE);
        drawable.setCornerRadius(12);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            button.setBackground(drawable);
        } else {
            button.setBackgroundDrawable(drawable);
        }
    }

    private void applyDarkModeStyleToEditText(EditText editText) {
        editText.setTextColor(Color.WHITE);
        editText.setHintTextColor(Color.LTGRAY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentSong != null) {
            // LOG DEPURACIÓN: ID usado para la escucha
            Log.d("SongDetailDebug", "onStart: Iniciando escucha de comentarios para Song Deezer ID: " + currentSong.deezer_id);
            loadComments(currentSong.deezer_id);
        } else {
            Log.w("SongDetailDebug", "onStart: currentSong es null, no se pueden cargar comentarios.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (commentsListenerRegistration != null) {
            commentsListenerRegistration.remove();
            commentsListenerRegistration = null;
        }
        stopPreview();
    }

    private String formatDuration(int durationSeconds) {
        long minutes = TimeUnit.SECONDS.toMinutes(durationSeconds);
        long seconds = durationSeconds - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    private void playPreview(String previewUrl) {
        if (previewUrl != null && !previewUrl.isEmpty()) {
            Log.d("SongDetailActivity", "Attempting to play preview: " + previewUrl);
            if (!isPlaying) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    Log.d("SongDetailActivity", "Setting data source: " + previewUrl);
                    mediaPlayer.setDataSource(previewUrl);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(mp -> {
                        Log.d("SongDetailActivity", "MediaPlayer prepared. Starting playback.");
                        mp.start();
                        isPlaying = true;
                        buttonPlayPreview.setText("Parar");

                        seekBarProgress.setMax(mediaPlayer.getDuration());
                        handler.post(updateSeekBar);
                    });
                    mediaPlayer.setOnCompletionListener(mp -> {
                        Log.d("SongDetailActivity", "MediaPlayer completed playback.");
                        stopPreview();
                    });
                    mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                        Log.e("SongDetailActivity", "MediaPlayer error: what=" + what + ", extra=" + extra);
                        Toast.makeText(this, "Error playing preview", Toast.LENGTH_SHORT).show();
                        stopPreview();
                        return true;
                    });
                } catch (IOException e) {
                    Log.e("SongDetailActivity", "IOException setting data source: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading preview", Toast.LENGTH_SHORT).show();
                    stopPreview();
                }
            } else {
                Log.d("SongDetailActivity", "Stopping preview.");
                stopPreview();
            }
        } else {
            Toast.makeText(this, "Preview not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPreview() {
        if (mediaPlayer != null) {
            Log.d("SongDetailActivity", "Stopping and releasing MediaPlayer.");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            buttonPlayPreview.setText("Escuchar");

            handler.removeCallbacks(updateSeekBar);
            seekBarProgress.setProgress(0);
            textViewCurrentTime.setText("0:00");
        } else {
            Log.d("SongDetailActivity", "MediaPlayer is null, nothing to stop.");
        }
    }

    private void showAddToPlaylistDialog(DeezerTrackResponse.Track song) {
        if (viewModel == null) {
            Log.e("SongDetailActivity", "ERROR: ViewModel es NULL en showAddToPlaylistDialog. No se puede mostrar el diálogo.");
            Toast.makeText(this, "Error: No se pudo cargar la funcionalidad de listas de reproducción.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir a lista de reproducción");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);

        List<String> nombresListas = viewModel.getListaNombres().getValue();
        if (nombresListas != null) {
            arrayAdapter.addAll(nombresListas);
        } else {
            Toast.makeText(this, "No hay listas de reproducción disponibles. Crea una primero.", Toast.LENGTH_LONG).show();
            return;
        }

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            String listaSeleccionada = nombresListas.get(which);
            viewModel.agregarCancionALista(listaSeleccionada, song);
            Toast.makeText(this, "Añadido a " + listaSeleccionada, Toast.LENGTH_SHORT).show();
            Log.d("AñadirCancion", "Canción '" + song.title + "' añadida a '" + listaSeleccionada + "'");
        });

        builder.show();
    }

    private void postComment() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Debes iniciar sesión para comentar.", Toast.LENGTH_SHORT).show();
            Log.w("SongDetailDebug", "postComment: Usuario no autenticado.");
            return;
        }

        if (currentSong == null) {
            Toast.makeText(this, "No se puede comentar sin información de la canción.", Toast.LENGTH_SHORT).show();
            Log.w("SongDetailDebug", "postComment: currentSong es null.");
            return;
        }

        String commentText = editTextComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "El comentario no puede estar vacío.", Toast.LENGTH_SHORT).show();
            Log.w("SongDetailDebug", "postComment: El comentario está vacío.");
            return;
        }

        String userId = user.getUid();
        String userName = user.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = user.getEmail();
            if (userName != null && userName.contains("@")) {
                userName = userName.substring(0, userName.indexOf("@"));
            } else {
                userName = "Usuario Anónimo";
            }
        }


        Log.d("SongDetailDebug", "postComment: Intentando guardar comentario para Song Deezer ID: " + songDeezerId);
        Log.d("SongDetailDebug", "postComment: Comentario: \"" + commentText + "\" por: " + userName + " (UserID: " + userId + ")");


        Comment newComment = new Comment(null, userId, userName, songDeezerId, commentText, new Date());

        db.collection("comments")
                .add(newComment)
                .addOnSuccessListener(documentReference -> {
                    Log.d("SongDetailDebug", "postComment: Comentario añadido exitosamente con ID: " + documentReference.getId());
                    editTextComment.setText("");
                    Toast.makeText(this, "Comentario publicado.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("SongDetailDebug", "postComment: Error al añadir comentario", e);
                    Toast.makeText(this, "Error al publicar comentario.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComments(long songDeezerId) {
        Log.d("SongDetailDebug", "loadComments: Cargando comentarios para la canción ID: " + songDeezerId);

        if (commentsListenerRegistration != null) {
            commentsListenerRegistration.remove();
            Log.d("SongDetailDebug", "loadComments: Listener anterior desregistrado.");
        }

        if (commentAdapter != null) {
            commentAdapter.setComments(new ArrayList<>());
            Log.d("SongDetailDebug", "loadComments: Adaptador de comentarios limpiado.");
        }

        commentsListenerRegistration = db.collection("comments")
                .whereEqualTo("songDeezerId", songDeezerId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("SongDetailDebug", "loadComments: Error en el listener de comentarios.", e);
                            return;
                        }

                        List<Comment> updatedComments = new ArrayList<>();
                        if (snapshots != null) {
                            Log.d("SongDetailDebug", "loadComments: Documentos en el snapshot: " + snapshots.size());

                            for (QueryDocumentSnapshot doc : snapshots) {
                                try {
                                    Comment comment = doc.toObject(Comment.class);
                                    comment.setId(doc.getId());
                                    updatedComments.add(comment);
                                    Log.d("SongDetailDebug", "loadComments: Comentario cargado: " + comment.getText() + " | ID: " + comment.getSongDeezerId());
                                } catch (Exception ex) {
                                    Log.e("SongDetailDebug", "loadComments: Error al convertir documento a objeto Comment: " + doc.getId(), ex);
                                }
                            }

                            if (updatedComments.isEmpty()) {
                                Log.d("SongDetailDebug", "loadComments: No hay comentarios para esta canción.");
                            } else {
                                Log.d("SongDetailDebug", "loadComments: Comentarios actualizados. Total: " + updatedComments.size());
                            }

                            commentAdapter.setComments(updatedComments);

                            if (updatedComments.size() > 0) {
                                recyclerViewComments.scrollToPosition(updatedComments.size() - 1);
                            }
                        } else {
                            Log.d("SongDetailDebug", "loadComments: Snapshots es null.");
                        }
                    }
                });
    }
}