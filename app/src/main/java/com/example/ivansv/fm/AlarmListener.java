package com.example.ivansv.fm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.example.ivansv.fm.MonitorActivity.currentTrack;

public class AlarmListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(MonitorActivity.ALARM_LISTENER_ACTION)) {
            new MonitorActivity.RetrieveContentsAsyncTask(currentTrack).execute();
//            StringBuilder sb = new StringBuilder();
//            sb.append("Positions");
//            for (Position pos : currentTrack) {
//                sb.append("-");
//                sb.append(pos.getTime());
//            }
//            Toast.makeText(context, sb, Toast.LENGTH_SHORT).show();
            if (currentTrack != null && currentTrack.size() == 5) {
                MonitorActivity.gMap.addMarker(new MarkerOptions().position(new LatLng(currentTrack.get(4).getLatitude(),
                        currentTrack.get(4).getLongitude())).title((String) android.text.format.DateFormat.format("hh:mm:ss",
                        currentTrack.get(4).getTime())));
            }

        }
    }
}