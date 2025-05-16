package es.riberadeltajo.topify.api;

import es.riberadeltajo.topify.models.DeezerTrackResponse;
import es.riberadeltajo.topify.models.SearchResult;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("chart/{country_code}/tracks")
    Call<DeezerTrackResponse> getTopTracksByCountry(
            @Path("country_code") String countryCode
    );
    @GET("search")
    Call<SearchResult> searchAll(@Query("q") String query);

    @GET("search/track")
    Call<SearchResult> searchTracks(@Query("q") String query);

    @GET("search/artist")
    Call<SearchResult> searchArtists(@Query("q") String query);

    @GET("search/album")
    Call<SearchResult> searchAlbums(@Query("q") String query);

    @GET("track/{id}")
    Call<DeezerTrackResponse.Track> getTrackDetails(@Path("id") long id);
}
