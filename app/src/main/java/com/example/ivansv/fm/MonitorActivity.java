package com.example.ivansv.fm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class MonitorActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static ArrayList<Position> currentTrack = new ArrayList<>();
    //    private DriveId selectedFileId;
    private boolean isSubscribed = false;
    //    private GoogleApiClient googleApiClient;
    public static final String ALARM_LISTENER_ACTION = "alarm listener action";
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    public static GoogleMap gMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_activity);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = new Intent(this, AlarmListener.class);
        intent.setAction(ALARM_LISTENER_ACTION);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 20 * 1000, pendingIntent);
        toggle();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
    }

    private void toggle() {
        if (MainActivity.driveId == null) {
            return;
        }
        DriveFile file = MainActivity.driveId.asDriveFile();
        if (!isSubscribed) {
            file.addChangeListener(MainActivity.googleApiClient, changeListener);
            isSubscribed = true;
        } else {
            file.removeChangeListener(MainActivity.googleApiClient, changeListener);
            isSubscribed = false;
        }
    }

    final private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void onChange(ChangeEvent event) {
        }
    };

    @Override
    protected void onPause() {
        toggle();
        alarmManager.cancel(pendingIntent);
        super.onPause();
    }

    public static class RetrieveContentsAsyncTask extends AsyncTask<Void, Void, Void> {
        ArrayList<Position> positions;

        public RetrieveContentsAsyncTask(ArrayList<Position> positions) {
            super();
            this.positions = positions;
        }

        @Override
        protected Void doInBackground(Void... params) {
            FileInputStream fileInputStream;

            DriveFile file = MainActivity.driveId.asDriveFile();
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        MainActivity.googleApiClient, DriveFile.MODE_READ_WRITE, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
//                    Toast.makeText(this, "No file!!!!", Toast.LENGTH_SHORT).show();
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();
                ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();
                fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                currentTrack = (ArrayList<Position>) objectInputStream.readObject();
                objectInputStream.close();
                com.google.android.gms.common.api.Status status = driveContents.commit(MainActivity.googleApiClient, null).await();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
