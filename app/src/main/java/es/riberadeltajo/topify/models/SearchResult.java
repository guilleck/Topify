package es.riberadeltajo.topify.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public class SearchResult {
    @SerializedName("data")
    private List<Item> data;

    public List<Item> getData() {
        return data;
    }

    public static class Item {
        private String type;
        private long id;
        private String title;
        private Artist artist;
        private Album album;
        private String name;
        @SerializedName("cover_big")
        private String coverBig;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return id == item.id &&
                    Objects.equals(type, item.type) &&
                    Objects.equals(title, item.title) &&
                    Objects.equals(artist, item.artist) &&
                    Objects.equals(album, item.album) &&
                    Objects.equals(name, item.name) &&
                    Objects.equals(coverBig, item.coverBig);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type, id, title, artist, album, name, coverBig);
        }

        // Getters
        public String getType() {
            return type;
        }

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Artist getArtist() {
            return artist;
        }

        public Album getAlbum() {
            return album;
        }

        public String getName() {
            return name;
        }

        public String getCoverBig() {
            return coverBig;
        }

        public static class Artist {
            private long id;
            private String name;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Artist artist = (Artist) o;
                return id == artist.id && Objects.equals(name, artist.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, name);
            }

            public long getId() {
                return id;
            }

            public String getName() {
                return name;
            }
        }

        public static class Album {
            private long id;
            private String title;
            @SerializedName("cover_big")
            private String coverBig;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Album album = (Album) o;
                return id == album.id && Objects.equals(title, album.title) && Objects.equals(coverBig, album.coverBig);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, title, coverBig);
            }

            public long getId() {
                return id;
            }

            public String getTitle() {
                return title;
            }

            public String getCoverBig() {
                return coverBig;
            }
        }
    }
}