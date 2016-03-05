package com.example.ivansv.fm;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class MonitorActivity extends Activity {
    private ArrayList<Position> currentTrack = new ArrayList<>();
//    private DriveId selectedFileId;
    private boolean isSubscribed = false;
//    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_activity);

        toggle();
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
//            currentTrack = readTrack();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new RetrieveContentsAsyncTask(currentTrack).execute();
            StringBuilder sb = new StringBuilder();
            sb.append("Positions");
            for (Position pos : currentTrack) {
                sb.append(pos.getTime() + "-");
            }
            Toast.makeText(MonitorActivity.this, sb, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onPause() {
        toggle();
        super.onPause();
    }

    private ArrayList<Position> readTrack() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = openFileInput("appconfig.txt");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            ArrayList<Position> positions = (ArrayList<Position>) objectInputStream.readObject();
            objectInputStream.close();
            return positions;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class RetrieveContentsAsyncTask extends AsyncTask<Void, Void, Void> {
        ArrayList<Position> positions;

        public RetrieveContentsAsyncTask(ArrayList<Position> positions) {
            super();
            this.positions = positions;
        }

        @Override
        protected Void doInBackground(Void... params) {
            FileInputStream fileInputStream = null;

            DriveFile file = MainActivity.driveId.asDriveFile();
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        MainActivity.googleApiClient, DriveFile.MODE_READ_WRITE, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Toast.makeText(MonitorActivity.this, "No file!!!!", Toast.LENGTH_SHORT).show();
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();

                ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();
                fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                currentTrack = (ArrayList<Position>) objectInputStream.readObject();
                objectInputStream.close();

//                InputStream inputStream = driveContents.getInputStream();
//                ObjectInputStream ois = new ObjectInputStream(inputStream);
//                currentTrack = (ArrayList<Position>) ois.readObject();
//                ois.close();

//                OutputStream outputStream = driveContents.getOutputStream();
//                outputStream.write("LOCATION".getBytes());
                com.google.android.gms.common.api.Status status = driveContents.commit(MainActivity.googleApiClient, null).await();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
