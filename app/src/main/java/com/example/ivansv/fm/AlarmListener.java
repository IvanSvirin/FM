package com.example.ivansv.fm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import static com.example.ivansv.fm.MonitorActivity.currentTrack;

public class AlarmListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(MonitorActivity.ALARM_LISTENER_ACTION)) {
            new MonitorActivity.RetrieveContentsAsyncTask(currentTrack).execute();
            StringBuilder sb = new StringBuilder();
            sb.append("Positions");
            for (Position pos : currentTrack) {
                sb.append("-");
                sb.append(pos.getTime());
            }
            Toast.makeText(context, sb, Toast.LENGTH_SHORT).show();
        }
    }
}