package es.riberadeltajo.topify.api;

import es.riberadeltajo.topify.models.DeezerTrackResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("chart/{country_code}/tracks")
    Call<DeezerTrackResponse> getTopTracksByCountry(
            @Path("country_code") String countryCode
    );

}
