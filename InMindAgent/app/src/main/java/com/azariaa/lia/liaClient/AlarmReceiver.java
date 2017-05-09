package com.azariaa.lia.liaClient;

/**
 * Created by Amos Azaria on 01-May-17.
 */

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.os.Handler;

public class AlarmReceiver extends WakefulBroadcastReceiver
{
    public static final String strRingBell = "ringBell";
    public static final String strToSay = "toSay";
    public static final String strSpeakHandler = "speakHandler";

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        Log.d("AlarmReceiver", "playing alarm");
        try
        {
            Bundle extras = intent.getExtras();
            Boolean ringBell = false;
            if (extras.containsKey(strRingBell))
                ringBell = extras.getBoolean(strRingBell);

            String toSay = null;
            if (extras.containsKey(strToSay))
                toSay = extras.getString(strToSay);

            if (ringBell)
            {
                Log.d("AlarmReceiver", "ringing bell");

                Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null)
                {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                final MediaPlayer mediaPlayer = MediaPlayer.create(context, alarmUri);
                mediaPlayer.setLooping(false);
                mediaPlayer.start();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mediaPlayer.stop();
                    }
                }, 10000);
//                Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
//                ringtone.play();
            }

//            //this will send a notification message
//            ComponentName comp = new ComponentName(context.getPackageName(),
//                    AlarmNotificationSender.class.getName());
//            startWakefulService(context, (intent.setComponent(comp)));


            Handler speakHandler = AlarmTimer.getSpeakHandler();
            if (speakHandler != null)
            {
                //speak!
                if (toSay != null && !toSay.isEmpty())
                {
                    Message msg = new Message();
                    msg.obj = toSay;
                    msg.arg1 = 1;
                    speakHandler.sendMessage(msg);
                }
                //delete alarm from alarmIntents
                AlarmTimer.staticRemoveOldAlarms();

                setResultCode(Activity.RESULT_OK);
            }
        }
        catch (Exception e)
        {
            Log.e("AlarmReceiver", "Error during playing alarm: " + e.getMessage());
        }
    }
}

