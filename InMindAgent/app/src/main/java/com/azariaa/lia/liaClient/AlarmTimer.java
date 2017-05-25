package com.azariaa.lia.liaClient;

/**
 * Created by Amos Azaria on 01-May-17.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.os.Handler;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.ALARM_SERVICE;

/**
 * Singleton
 */
public class AlarmTimer
{

    interface SpeakThis
    {
        void speakThis(String toSay); //, int flag);
    }

    private static AlarmTimer instance = null;
    AlarmManager alarmManager;
    Context context;
    SpeakThis speakThis;
    MediaPlayer mediaPlayer = null;
    final Object mediaPlayerSynchronizer = new Object();
    private final List<AlarmIntent> alarmIntents = new LinkedList<>();

    SimpleDateFormat sdfForReadingAloud = new SimpleDateFormat("h:mm aa");

    private AlarmTimer()
    {
    }

    public static AlarmTimer CreateAlarmTimer(Context context, SpeakThis speakThis, Handler speakHandler)
    {
        if (instance == null)
        {
            instance = new AlarmTimer();
            instance.alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            instance.context = context.getApplicationContext();
            instance.speakThis = speakThis;
            //instance.speakHandler = speakHandler;
        }
        return instance;
    }

    public static void setMediaPlayer(MediaPlayer mediaPlayer)
    {
        synchronized (instance.mediaPlayerSynchronizer)
        {
            instance.mediaPlayer = mediaPlayer;
        }
    }

    public static void removeMediaPlayer()
    {
        synchronized (instance.mediaPlayerSynchronizer)
        {
            instance.mediaPlayer = null;
        }
    }

    public void stopAlarm()
    {
        synchronized (mediaPlayerSynchronizer)
        {
            if (mediaPlayer != null)
                mediaPlayer.stop();
        }
    }

//    public static Handler getSpeakHandler()
//    {
//        return instance.speakHandler;
//    }

    public static SpeakThis getSpeakThis()
    {
        return instance.speakThis;
    }

    void dealWithTimeMessage(String jsonStr)
    {
        try
        {
            JSONObject json = new JSONObject(jsonStr);
            String type = json.getString("type");
            if (type.equals("read"))
            {
                String whatToRead = json.getString("whatToRead");
                if (whatToRead.equals("currentTime"))
                {
                    readCurrentTime();
                }
                else if (whatToRead.equals("currentDate"))
                {
                    readCurrentDate();
                }
                else if (whatToRead.equals("allAlarms"))
                {
                    readAllAlarms(false);
                }
                else if (whatToRead.equals("allTimers"))
                {
                    readAllAlarms(true);
                }
            }
            else if (type.equals("setAlarm") || type.equals("setTimer")
                    || type.equals("delAlarm") || type.equals("delTimer"))
            {
                boolean isTimer;
                boolean toDelete = type.startsWith("del");
                Date alarmTimerDate;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                alarmTimerDate = dateFormat.parse(json.getString("alarmTimerDate"));
                isTimer = (type.equals("setTimer") || type.equals("delTimer"));
//                if (type.equals("setAlarm") || type.equals("delAlarm"))
//                {
//                    isTimer = false;
//                }
//                else
//                {
//                    isTimer = true;
////                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
////                    alarmTimerDate = dateFormat.parse(json.getString("alarmTimerDate"));
//                }
                String toSay = "";
                if (json.has("toSay"))
                    toSay = json.getString("toSay");
                boolean bell = true;
                if (json.has("bell"))
                    bell = json.getBoolean("bell");
                if (toDelete)
                {
                    deleteAlarmTimer(isTimer, alarmTimerDate);
                }
                else
                    setAlarm(alarmTimerDate, isTimer, toSay, bell);
                //SimpleDateFormat sdf = new SimpleDateFormat("MMM dd hh:mm aa", Locale.US); //TODO: today, tomorrow, etc. Take from liaExtension
                //ttsCont.speakThis("Alarm not set for: " + sdf.format(currTime));
            }
            else if (type.equals("delAll"))
            {
                deleteAllAlarms();
            }

        } catch (Exception ex)
        {
            Log.e("AlarmTimer", "dealWithTimeMessage exception:" + ex.getMessage());
        }

    }

    private void deleteAlarmTimer(boolean isTimer, Date alarmTimerDate)
    {
        //find the alarm closest to the requested time
        final long alarmTimeDateFinal = alarmTimerDate.getTime();
        AlarmIntent closest = Collections.min(alarmIntents, new Comparator<AlarmIntent>() {
            public int compare(AlarmIntent d1, AlarmIntent d2) {
                long diff1 = Math.abs(d1.alarmTime.getTime().getTime() - alarmTimeDateFinal);
                long diff2 = Math.abs(d2.alarmTime.getTime().getTime() - alarmTimeDateFinal);
                return Long.compare(diff1, diff2);
            }
        });
        //if closest alarm is within 10 minutes of requested time delete it, otherwise say not found
        long distance = Math.abs(closest.alarmTime.getTime().getTime() - alarmTimeDateFinal);
        boolean closeEnough = (distance / 1000) / 60 < 10;
        if (closeEnough)
        {
            alarmManager.cancel(closest.pendingIntent);
            speakThis(getStrForReadingAlarm(closest, false) + ", was canceled successfully");
            alarmIntents.remove(closest);
        }
        else
        {
            speakThis((isTimer ? "Timer" : "Alarm") +" not found");
        }
    }

    private void readCurrentDate()
    {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE. MMM dd yyyy", Locale.US);
        String currTime = sdf.format(now.getTime());
        speakThis("Today is: " + currTime);
    }

    public void readCurrentTime()
    {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa", Locale.US);
        String currTime = sdf.format(now.getTime());
        speakThis("The time is: " + currTime);
    }

    class AlarmIntent
    {
        public AlarmIntent(boolean isATimer, PendingIntent pendingIntent, String toSay, boolean ringBell, Calendar alarmTime)
        {
            this.isATimer = isATimer;
            this.pendingIntent = pendingIntent;
            this.toSay = toSay;
            this.ringBell = ringBell;
            this.alarmTime = alarmTime;
        }

        boolean isATimer = false;
        public PendingIntent pendingIntent;
        public String toSay;
        public boolean ringBell;
        public Calendar alarmTime;
    }

    /**
     *
     * @param date
     * @param isTimer The difference between an alarm and a timer is currently only when read back:
     *                a. they are devided into 2 different groups
     *                b. timers are called by how much time from now they will go off
     *                c. future may support repeating alarms / timers (with different interpretation
     * @param toSay
     * @param ringBell
     */
    public void setAlarm(Date date, boolean isTimer, String toSay, boolean ringBell)
    {
        Log.d("MainActivity", "Setting alarm");
        try
        {
//            String dateText = alarmTimeText.getText().toString();
//            Date date = parseDateTime(dateText, false);

//            String timerAsStr = timerTimeText.getText().toString();
//            Date timerDate = parseDateTime(timerAsStr, true);
            if (date == null) // && timerDate == null)
            {
                speakThis("Bad format of alarm or timer, alarm not set.");
                return;
            }
            // = (date == null);
            Calendar calendar = date2Calendar(date);

            if (toSay.isEmpty() && !ringBell)
            {
                speakThis("You must either have a message or set the bell on (or both).");
                return;
            }

            Intent myIntent = new Intent(context, AlarmReceiver.class);
            myIntent.putExtra(AlarmReceiver.strToSay, toSay);
            myIntent.putExtra(AlarmReceiver.strRingBell, ringBell);
            myIntent.putExtra(AlarmReceiver.strIsTimer, isTimer);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmIntent alarmIntent = new AlarmIntent(isTimer, pendingIntent, toSay, ringBell, calendar);
            synchronized (alarmIntents)
            {
                alarmIntents.add(alarmIntent);
            }
            //AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo();
            if (isTimer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            else
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

            //clearTexts();
            speakThis(getStrForReadingAlarm(alarmIntent, true));
        }
        catch (Exception e)
        {
            Log.e("AlarmTimer", "Error during setting alarm: " + e.getMessage());
        }
    }

    @NonNull
    private Calendar date2Calendar(Date date) // , boolean isTimer)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
//        if (isTimer)
//        {
//            calendar.add(Calendar.HOUR, date.getHours());
//            calendar.add(Calendar.MINUTE, date.getMinutes());
//            calendar.add(Calendar.SECOND, date.getSeconds());
//        }
//        else
//        {
//            calendar.set(Calendar.HOUR_OF_DAY, date.getHours());//alarmTimeText.getCurrentHour());
//            calendar.set(Calendar.MINUTE, date.getMinutes());//alarmTimeText.getCurrentMinute());
//            //make sure we set only alarms in the future!
//            if (calendar.before(Calendar.getInstance()))
//                calendar.add(Calendar.DATE, 1);
//        }
        return calendar;
    }

    Date parseDateTime(String dateString, boolean isTimer)
    {
        String[] supportedDateFormats = {"h:mmaa", "h:mm aa", "h:mm"};
        String[] supportedTimerFormats = {"h:mm:ss", "m:ss"};
        String[] supportedFormats = supportedDateFormats;
        if (isTimer)
            supportedFormats = supportedTimerFormats;
        for (String formatString : supportedFormats)
        {
            try
            {
                return new SimpleDateFormat(formatString).parse(dateString);
            }
            catch (ParseException ignored) {}
        }

        return null;
    }

    private void readAllAlarms(boolean isATimer)
    {
        try
        {
            //first delete old alarms if present
            removeOldAlarms();

            Log.d("AlarmTimer", "Reading All Alarms");
            List<AlarmIntent> toRead = new LinkedList<>();
            for (AlarmIntent alarmIntent : alarmIntents)
            {
                if(isATimer == alarmIntent.isATimer)
                    toRead.add(alarmIntent);
            }
            String toSay = "You have " + (toRead.size() == 0 ? "no" : toRead.size()) +
                    " " + (isATimer? "timer" : "alarm") + ((toRead.size() == 1) ? "" : "s") + " set";
            speakThis(toSay);
            for (AlarmIntent alarmIntent : toRead)
            {
                speakThis(getStrForReadingAlarm(alarmIntent, false));
            }
        }
        catch (Exception e)
        {
            Log.e("AlarmTimer", "Error during reading all alarms: " + e.getMessage());
        }
    }

    private String getStrForReadingAlarm(AlarmIntent alarmIntent, boolean nowSet)
    {
        Log.d("AlarmTimer", "Reading Alarm");
        Calendar now = Calendar.getInstance();
        String toSay = (alarmIntent.isATimer ? "Timer " : "Alarm ") ;
        if (nowSet)
            toSay += "set to go off ";

        if (alarmIntent.isATimer)
        {
            long totalMillis = alarmIntent.alarmTime.getTimeInMillis() - now.getTimeInMillis();
            if (totalMillis < 1000)
                toSay += "right now!";
            else
            {
                toSay += "in ";
                int hours = (int) (totalMillis / 1000) / 3600;
                if (hours > 1)
                    toSay += hours + " hour" + (hours > 1 ? "s" : "") + " ";
                int minutes = ((int) (totalMillis / 1000) / 60) % 60;
                int seconds = (int) (totalMillis / 1000) % 60;
                toSay += minutes + " minute" + (minutes != 1 ? "s" : "") + " and " + seconds +" second" + (seconds != 1 ? "s" : "");
            }
        }
        else
        {
            if (alarmIntent.alarmTime.get(Calendar.DATE) == now.get(Calendar.DATE))
                toSay += "Today ";
            else if ((int)alarmIntent.alarmTime.get(Calendar.DATE) == (int)now.get(Calendar.DATE) + 1)
                toSay += "Tomorrow ";
            else
                toSay = "On " + alarmIntent.alarmTime.toString();
            toSay += "at: " + sdfForReadingAloud.format(alarmIntent.alarmTime.getTime());
        }

        toSay += " with " + (alarmIntent.ringBell ? "a" : "no") + " bell.";
        if (!alarmIntent.toSay.isEmpty())
            toSay += " Message:" + alarmIntent.toSay;
        return toSay;
        //speakThis(toSay);
    }

    static void staticRemoveOldAlarms()
    {
        instance.removeOldAlarms();
    }

    private void removeOldAlarms()
    {
        synchronized (alarmIntents)
        {
            Log.d("AlarmTimer", "Removing Old Alarms");
            for (Iterator<AlarmIntent> iterator = alarmIntents.iterator(); iterator.hasNext(); )
            {
                AlarmIntent alarmIntent = iterator.next();
                if (alarmIntent.alarmTime.before(Calendar.getInstance()))
                {
                    iterator.remove();
                }
            }
        }
    }

    public void deleteAllAlarms()
    {
        synchronized (alarmIntents)
        {
            Log.d("AlarmTimer", "Deleting All Alarms");
            for (AlarmIntent alarmIntent : alarmIntents)
            {
                alarmManager.cancel(alarmIntent.pendingIntent);
            }
            alarmIntents.clear();
        }
        speakThis("All alarms deleted");//, TextToSpeech.QUEUE_FLUSH);
    }

    public void speakThis(String toSay)
    {
        speakThis.speakThis(toSay);//, TextToSpeech.QUEUE_ADD);
    }

//    public void speakThis(String toSay, int flag)
//    {
//        speakThis.speakThis(toSay, flag);
//    }

}

