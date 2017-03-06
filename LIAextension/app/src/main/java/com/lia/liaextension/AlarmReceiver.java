package com.lia.liaextension;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmReceiver extends WakefulBroadcastReceiver
{
    public static final String strRingBell = "ringBell";
    public static final String strToSay = "toSay";
    public static final String strSpeakHandler = "speakHandler";

    @Override
    public void onReceive(final Context context, Intent intent)
    {
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
                //this will sound the alarm tone
                //this will sound the alarm once, if you wish to
                //raise alarm in loop continuously then use MediaPlayer and setLooping(true)
                Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null)
                {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
                ringtone.play();
            }

            //this will send a notification message
            ComponentName comp = new ComponentName(context.getPackageName(),
                    AlarmService.class.getName());
            startWakefulService(context, (intent.setComponent(comp)));


            MainActivity inst = MainActivity.instance();
            //speak!
            if (toSay != null && !toSay.isEmpty())
            {
                Message msg = new Message();
                msg.obj = toSay;
                inst.speakHandler.sendMessage(msg);
            }
            //delete alarm from alarmIntents
            inst.removeOldAlarms();

            setResultCode(Activity.RESULT_OK);
        }
        catch (Exception e)
        {
            Log.e("AlarmReceiver", "Error during playing alarm: " + e.getMessage());
        }
    }
}
