package com.example.ivansv.fm;

import android.app.Application;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;

public class FMApplication extends Application {
    public static GoogleApiClient googleApiClient;
    public static DriveId driveId;

}
