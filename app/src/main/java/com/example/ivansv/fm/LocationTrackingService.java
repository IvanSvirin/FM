package com.example.ivansv.fm;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class LocationTrackingService extends Service {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    private ArrayList<Position> sentTrack;

    @Override
    public void onCreate() {
        super.onCreate();
        sentTrack = new ArrayList<>();
        Toast.makeText(this, "CREATE_SERVICE", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "START_SERVICE", Toast.LENGTH_SHORT).show();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20000, 0, listener);
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "STOP_SERVICE", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
        }
        locationManager.removeUpdates(listener);
    }

    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Toast.makeText(getApplicationContext(), "Location changed", Toast.LENGTH_SHORT).show();
            if (isBetterLocation(loc, previousBestLocation)) {
                Position position = new Position();
                position.setLatitude(loc.getLatitude());
                position.setLongitude(loc.getLongitude());
                position.setTime(loc.getTime());
                if (sentTrack.size() > 4) sentTrack.remove(0);
                sentTrack.add(position);
                new EditContentsAsyncTask(sentTrack).execute();
                Toast.makeText(getApplicationContext(), "Latitude" + loc.getLatitude(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "Longitude" + loc.getLongitude(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "Provider" + loc.getProvider(), Toast.LENGTH_SHORT).show();
            }
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    public class EditContentsAsyncTask extends AsyncTask<Void, Void, Void> {
        ArrayList<Position> positions;

        public EditContentsAsyncTask(ArrayList<Position> positions) {
            super();
            this.positions = positions;
        }

        @Override
        protected Void doInBackground(Void... params) {
            FileOutputStream fileOutputStream;

            DriveFile file = MainActivity.driveId.asDriveFile();
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        MainActivity.googleApiClient, DriveFile.MODE_READ_WRITE, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Toast.makeText(LocationTrackingService.this, "No file!!!!", Toast.LENGTH_SHORT).show();
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();
                ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();
                fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(positions);
                objectOutputStream.close();
                com.google.android.gms.common.api.Status status = driveContents.commit(MainActivity.googleApiClient, null).await();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
