package es.riberadeltajo.topify;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SongDetailActivity extends AppCompatActivity {

    private ImageView imageViewCoverDetail;
    private TextView textViewTitleDetail;
    private TextView textViewArtistDetail;
    private TextView textViewDurationDetail;
    private Button buttonPlayPreview;
    private TextView textViewPreviewUrl;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);

        imageViewCoverDetail = findViewById(R.id.imageViewCoverDetail);
        textViewTitleDetail = findViewById(R.id.textViewTitleDetail);
        textViewArtistDetail = findViewById(R.id.textViewArtistDetail);
        textViewDurationDetail = findViewById(R.id.textViewDurationDetail);
        buttonPlayPreview = findViewById(R.id.buttonPlayPreview);
        textViewPreviewUrl = findViewById(R.id.textViewPreviewUrl);

        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("title");
            String artist = intent.getStringExtra("artist");
            String coverUrl = intent.getStringExtra("coverUrl");
            int duration = intent.getIntExtra("duration", 0);
            String previewUrl = intent.getStringExtra("previewUrl");

            textViewTitleDetail.setText(title);
            textViewArtistDetail.setText(artist);
            textViewDurationDetail.setText(formatDuration(duration));
            textViewPreviewUrl.setText(previewUrl);
            Glide.with(this).load(coverUrl).into(imageViewCoverDetail);

            buttonPlayPreview.setOnClickListener(v -> playPreview(previewUrl));
        }
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
        } else {
            Log.d("SongDetailActivity", "MediaPlayer is null, nothing to stop.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPreview();
    }
}