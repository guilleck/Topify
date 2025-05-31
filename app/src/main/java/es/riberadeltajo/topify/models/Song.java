package es.riberadeltajo.topify.models;

public class Song {
    private String title, artist, album, albumCover;
    public Song(String title, String artist, String album, String albumCover) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumCover = albumCover;
    }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getAlbumCover() { return albumCover; }
}
