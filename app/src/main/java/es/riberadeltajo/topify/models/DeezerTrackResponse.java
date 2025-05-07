package es.riberadeltajo.topify.models;

import java.util.List;

public class DeezerTrackResponse {

    public List<Track> data;

    public static class Track {
        public long deezer_id;
        public String title;
        public Artist artist;
        public Album album;
        public int duration;
        public String preview;

        public static class Artist {
            public String name;
        }

        public static class Album {
            public String title;
            public String cover_big;
        }
    }
}
