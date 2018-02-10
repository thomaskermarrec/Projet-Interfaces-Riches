package fr.enssat.tkermarrec.ar_tp1;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private String file_url;
    private String title;
    private String synopsis_url;

    private VideoView myVideoView;
    private MediaController mediaController;
    private int position = 0;
    private MapView myMapView;
    JSONObject jObject;
    JSONArray myChapters;
    JSONArray myWaypoints;
    JSONObject myFilmData;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String JSON_LAT="lat";
    private static final String JSON_LNG="lng";
    private static final String JSON_LABEL="label";
    private static final String JSON_TIMESTAMP="timestamp";
    private static final String JSON_FILE_URL = "file_url";
    private static final String JSON_FILE_TITLE="title";
    private static final String JSON_SYNOPSIS_URL="synopsis_url";
    private static final String JSON_POS="pos";
    private static final String JSON_CHAPTER_TITLE="title";
    private static final String JSON_FILM="Film";
    private static final String JSON_CHAPTERS="Chapters";
    private static final String JSON_WAYPOINTS="Waypoints";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            readJSON();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        try {
            initData();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        //Videoview
        myVideoView = (VideoView) findViewById(R.id.videoView);
        try {
            myVideoView.setVideoURI(Uri.parse(file_url));
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        //MediaController
        if (mediaController == null) {
            mediaController = new MediaController(MainActivity.this);
            mediaController.setAnchorView(myVideoView);
            myVideoView.setMediaController(mediaController);
        }

        //Chapters list
        try {
            initChapters();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        myMapView = findViewById(R.id.mapView);
        myMapView.onCreate(mapViewBundle);

        //myVideoView.requestFocus();
        myVideoView.start();

        try {
            initWaypoints();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        // When the video file ready for playback.
        myVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                myVideoView.seekTo(position);
            if (position == 0) {
                myVideoView.start();
            }

            // Quand la taille d'écran change
            mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    // Fait l'association avec la videoView
                    mediaController.setAnchorView(myVideoView);
                }
            });
            }
        });

        initMap();
    }


    // Lit le fichier JSON contenant toutes les informations
    // Convertit ce fichier sous la forme d'un JSONObject
    private void readJSON(){
        InputStream inputStream = getResources().openRawResource(R.raw.chapters);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int ctr;
        try {
            ctr = inputStream.read();
            while (ctr != -1) {
                byteArrayOutputStream.write(ctr);
                ctr = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        try {
            jObject = new JSONObject(byteArrayOutputStream.toString());
        } catch(Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initData() {
        try {
            myFilmData = jObject.getJSONObject(JSON_FILM);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        try {
            title = myFilmData.getString(JSON_FILE_TITLE);
            file_url = myFilmData.getString(JSON_FILE_URL);
            synopsis_url = myFilmData.getString(JSON_SYNOPSIS_URL);
        } catch(Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

    }

    private void initChapters() {
        try {
            myChapters = jObject.getJSONArray(JSON_CHAPTERS);

            int chapterPos = 0;
            String chapterTitle = "";
            LinearLayout chapters = (LinearLayout)findViewById(R.id.buttonwrapper);

            for (int i = 0; i < myChapters.length(); i++) {
                chapterPos = myChapters.getJSONObject(i).getInt(JSON_POS);
                chapterTitle = myChapters.getJSONObject(i).getString(JSON_CHAPTER_TITLE);

                Button button = new Button(this);
                button.setTag(chapterPos);
                button.setText(chapterTitle);
                button.setOnClickListener(chaptersListener);
                chapters.addView(button);
            }
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initWaypoints() {
        try {
            myWaypoints = jObject.getJSONArray(JSON_WAYPOINTS);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
    }


    private void initMap(){
        myMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                try {
                    long lat = 0;
                    long lng = 0;
                    String label = "";
                    int timestamp = 0;
                    for (int i = 0; i < myWaypoints.length(); i++) {
                        lat = myWaypoints.getJSONObject(i).getLong(JSON_LAT);
                        lng = myWaypoints.getJSONObject(i).getLong(JSON_LNG);
                        label = myWaypoints.getJSONObject(i).getString(JSON_LABEL);
                        timestamp = myWaypoints.getJSONObject(i).getInt(JSON_TIMESTAMP);
                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(lat,lng))
                                .title(label));
                        marker.setTag(timestamp);
                    }
                } catch (JSONException e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }

                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        int timestamp = (int)marker.getTag();
                        myVideoView.seekTo(timestamp * 1000);
                        return false;
                    }
                });
            }
        });
    }


    private View.OnClickListener chaptersListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button)v;
            final int position = (int)b.getTag();
            final String buttonText = (String)b.getText();
            myVideoView.seekTo(position * 1000);

            Toast.makeText(MainActivity.this, buttonText, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        myMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myMapView.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        myMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        myMapView.onLowMemory();
    }
}
