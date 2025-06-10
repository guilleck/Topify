package es.riberadeltajo.topify.models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class DeezerTrackResponse {

    public List<Track> data;

    public static class Track {
        @SerializedName("id")
        public long deezer_id;
        public String title;
        public Artist artist;
        public Album album;
        public int duration;
        public String preview;

        public static class Artist {
            @SerializedName("id")
            public long id;
            public String name;
        }

        public static class Album {
            @SerializedName("id")
            public long id;
            public String title;
            public String cover_big;
        }
    }
}
