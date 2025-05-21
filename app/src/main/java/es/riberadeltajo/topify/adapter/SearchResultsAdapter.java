package es.riberadeltajo.topify.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import es.riberadeltajo.topify.R;
import es.riberadeltajo.topify.SongDetailActivity;
import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.ListaReproduccionViewModel;
import es.riberadeltajo.topify.models.SearchResult;

import java.util.List;

public class SearchResultsAdapter extends ListAdapter<SearchResult.Item, SearchResultsAdapter.SearchResultViewHolder> {

    public interface OnSearchResultLongClickListener {
        void onSearchResultLongClick(DeezerTrackResponse.Track song);
    }

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private ListaReproduccionViewModel listaReproduccionViewModel;
    private boolean isLongClickActive = false;
    private final OnSearchResultLongClickListener longClickListener;



    public SearchResultsAdapter(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner, @NonNull OnSearchResultLongClickListener longClickListener) {
        super(new DiffUtil.ItemCallback<SearchResult.Item>() {
            @Override
            public boolean areItemsTheSame(@NonNull SearchResult.Item oldItem, @NonNull SearchResult.Item newItem) {
                return oldItem.getId() == newItem.getId() && oldItem.getType().equals(newItem.getType());
            }

            @Override
            public boolean areContentsTheSame(@NonNull SearchResult.Item oldItem, @NonNull SearchResult.Item newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.longClickListener = longClickListener; // Asignar el listener
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResult.Item currentItem = getItem(position);
        if (currentItem != null) {
            holder.titleTextView.setText(currentItem.getTitle() != null ? currentItem.getTitle() : currentItem.getName());
            String imageUrl = null;
            Log.d("SearchResultsAdapter", "URL de la imagen: " + imageUrl);

            if ("track".equals(currentItem.getType()) && currentItem.getAlbum() != null && currentItem.getAlbum().getCoverBig() != null) {
                imageUrl = currentItem.getAlbum().getCoverBig();
            } else if (currentItem.getCoverBig() != null) {
                imageUrl = currentItem.getCoverBig();
            }

            Log.d("SearchResultsAdapter", "URL de la imagen: " + (imageUrl != null ? imageUrl : "NULA")); // Log más descriptivo

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)

                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                Log.e("SearchResultsAdapter", "Error al cargar la imagen: " + (e != null ? e.getMessage() : "Error desconocido"));
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Log.d("SearchResultsAdapter", "Imagen cargada con éxito desde: " + dataSource.name());
                                return false;
                            }
                        })
                        .into(holder.coverImageView);
            } else {
                holder.coverImageView.setImageResource(R.drawable.musica);
            }

            String subtitle = "";
            if ("track".equals(currentItem.getType()) && currentItem.getArtist() != null) {
                subtitle = "Artista: " + currentItem.getArtist().getName();
            }
            holder.subtitleTextView.setText(subtitle);

            holder.itemView.setOnClickListener(v -> {
                if ("track".equals(currentItem.getType())) {
                    long trackId = currentItem.getId();
                    if (listaReproduccionViewModel == null) {
                        listaReproduccionViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(ListaReproduccionViewModel.class);
                    }
                    listaReproduccionViewModel.obtenerDetallesCancion(trackId, context);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if ("track".equals(currentItem.getType()) && !isLongClickActive) {
                    isLongClickActive = true;
                    // Convertir SearchResult.Item a DeezerTrackResponse.Track para pasarlo al Fragment
                    DeezerTrackResponse.Track song = convertirSearchResultATrack(currentItem);
                    longClickListener.onSearchResultLongClick(song); // 3. Notificar al Fragment

                    holder.itemView.setOnTouchListener((view, event) -> {
                        if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                            isLongClickActive = false; // Resetea la bandera al levantar el dedo
                            holder.itemView.setOnTouchListener(null); // Limpia el OnTouchListener temporal
                        }
                        return false;
                    });
                    return true;
                }
                return false;
            });
        }
    }


    private DeezerTrackResponse.Track convertirSearchResultATrack(SearchResult.Item item) {
        DeezerTrackResponse.Track track = new DeezerTrackResponse.Track();
        DeezerTrackResponse.Track.Artist artist = new DeezerTrackResponse.Track.Artist();
        DeezerTrackResponse.Track.Album album = new DeezerTrackResponse.Track.Album();

        track.deezer_id = item.getId();
        track.title = item.getTitle();
        if (item.getArtist() != null) {
            artist.name = item.getArtist().getName();
            track.artist = artist;
        }
        if (item.getAlbum() != null) {
            album.title = item.getAlbum().getTitle();
            album.cover_big = item.getAlbum().getCoverBig();
            track.album = album;
        }
        return track;
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final ImageView coverImageView;
        private final TextView titleTextView;
        private final TextView subtitleTextView;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            subtitleTextView = itemView.findViewById(R.id.subtitleTextView);
        }
    }
}