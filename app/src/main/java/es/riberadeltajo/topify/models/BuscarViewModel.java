package es.riberadeltajo.topify.models;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import es.riberadeltajo.topify.SongDetailActivity;
import es.riberadeltajo.topify.api.ApiService;
import es.riberadeltajo.topify.models.SearchResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class BuscarViewModel extends ViewModel {
    private MutableLiveData<List<SearchResult.Item>> searchResults = new MutableLiveData<>();
    private ApiService apiService;

    public BuscarViewModel() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    public LiveData<List<SearchResult.Item>> getSearchResults() {
        return searchResults;
    }

    public void buscar(String query, String type) {
        Call<SearchResult> call;
        switch (type.toLowerCase()) {
            case "track":
                call = apiService.searchTracks(query);
                break;
            case "artist":
                call = apiService.searchArtists(query);
                break;
            case "album":
                call = apiService.searchAlbums(query);
                break;
            default:
                call = apiService.searchAll(query);
                break;
        }

        call.enqueue(new Callback<SearchResult>() {
            @Override
            public void onResponse(Call<SearchResult> call, Response<SearchResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    searchResults.setValue(response.body().getData());
                } else {
                    searchResults.setValue(null); // O manejar el error de otra manera
                }
            }

            @Override
            public void onFailure(Call<SearchResult> call, Throwable t) {
                searchResults.setValue(null); // O manejar el error
            }
        });
    }


}