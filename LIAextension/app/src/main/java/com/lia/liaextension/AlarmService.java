package com.lia.liaextension;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends IntentService
{

    public AlarmService()
    {
        super("AlarmService");
    }

    @Override
    public void onHandleIntent(Intent intent)
    {
        try
        {
            String toSay = "Alarm Activated.";
            Bundle extras = intent.getExtras();
            if (extras.containsKey(AlarmReceiver.strToSay))
                toSay = extras.getString(AlarmReceiver.strToSay);
            sendNotification(toSay);
        }
        catch (Exception e)
        {
            Log.e("AlarmService", "Error during sending notification: " + e.getMessage());
        }
    }

    private void sendNotification(String msg)
    {
        Log.d("AlarmService", "Preparing to send notification...: " + msg);
        NotificationManager alarmNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder alamNotificationBuilder = new NotificationCompat.Builder(
                this).setContentTitle("Alarm").setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg);


        alamNotificationBuilder.setContentIntent(contentIntent);
        alarmNotificationManager.notify(1, alamNotificationBuilder.build());
        Log.d("AlarmService", "Notification sent.");
    }
}
