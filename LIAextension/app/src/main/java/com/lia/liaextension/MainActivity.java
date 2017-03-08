package com.lia.liaextension;

import android.os.Build;
import android.os.Bundle;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity
{

    AlarmManager alarmManager;
    private final List<AlarmIntent> alarmIntents = new LinkedList<>();
    private EditText alarmTimeText;
    private EditText timerTimeText;
    private EditText alarmToSay;
    private static MainActivity inst;
    ToggleButton alarmRingToggle;
    TextToSpeech tts;
    SimpleDateFormat sdfForReadingAloud = new SimpleDateFormat("h:mm aa");
    public Handler speakHandler;

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

    public static MainActivity instance()
    {
        return inst;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alarmTimeText = (EditText) findViewById(R.id.alarmTimePicker);
        timerTimeText = (EditText) findViewById(R.id.timerTime);
        alarmToSay = (EditText) findViewById(R.id.alarmToSay);
        alarmRingToggle = (ToggleButton) findViewById(R.id.alarmToggle);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        initializeTTS();
        speakHandler = new Handler(new Handler.Callback()
        {
            @Override
            public boolean handleMessage(Message message)
            {
                speakThis(message.obj.toString());
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    private void initializeTTS()
    {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status == TextToSpeech.SUCCESS)
                {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Log.e("error", "This Language is not supported");
                    }
//                    else
//                    {
//                        ConvertTextToSpeech();
//                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });
        tts.setLanguage(Locale.US);
    }

//    public void onRingClicked(View view) {
//        if (((ToggleButton) view).isChecked()) {
//            Log.d("MainActivity", "Trying to turn on Alarm");
//            try
//            {
//
//            }
//            catch (Exception e)
//            {
//                Log.e("MainActivity", "Error during setting alarm" + e.getMessage());
//            }
//        } else {
//            alarmManager.cancel(pendingIntent);
//            setAlarmText("");
//            Log.d("MainActivity", "Alarm Off");
//        }
//    }

    public void onSetClicked(View view)
    {
        Log.d("MainActivity", "Setting alarm");
        try
        {
            String dateText = alarmTimeText.getText().toString();
            Date date = parseDateTime(dateText, false);

            String timerAsStr = timerTimeText.getText().toString();
            Date timerDate = parseDateTime(timerAsStr, true);
            if (date == null && timerDate == null)
            {
                speakThis("Bad format of alarm or timer, alarm not set.");
                return;
            }
            boolean isTimer = (date == null);
            Calendar calendar = Calendar.getInstance();
            if (isTimer)
            {
                calendar.add(Calendar.HOUR, timerDate.getHours());
                calendar.add(Calendar.MINUTE, timerDate.getMinutes());
                calendar.add(Calendar.SECOND, timerDate.getSeconds());
            }
            else
            {
                calendar.set(Calendar.HOUR_OF_DAY, date.getHours());//alarmTimeText.getCurrentHour());
                calendar.set(Calendar.MINUTE, date.getMinutes());//alarmTimeText.getCurrentMinute());
                //make sure we set only alarms in the future!
                if (calendar.before(Calendar.getInstance()))
                    calendar.add(Calendar.DATE, 1);
            }

            String toSay = alarmToSay.getText().toString();
            boolean ringBell = alarmRingToggle.isChecked();
            if (toSay.isEmpty() && !ringBell)
            {
                speakThis("You must either have a message or set the bell on (or both).");
                return;
            }

            Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
            myIntent.putExtra(AlarmReceiver.strToSay, toSay);
            myIntent.putExtra(AlarmReceiver.strRingBell, ringBell);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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

            clearTexts();
            readAlarm(alarmIntent, true);
        }
        catch (Exception e)
        {
            Log.e("MainActivity", "Error during setting alarm: " + e.getMessage());
        }
    }

    private void clearTexts()
    {
        alarmTimeText.setText("");
        timerTimeText.setText("");
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

    public void onReadAllAlarms(View view)
    {
        try
        {
            //first delete old alarms if present
            removeOldAlarms();

            Log.d("MainActivity", "Reading All Alarms");
            String toSay = "You have " + (alarmIntents.size() == 0 ? "no" : alarmIntents.size()) +
                    " alarm" + ((alarmIntents.size() == 1) ? "" : "s") + " set";
            speakThis(toSay);
            for (AlarmIntent alarmIntent : alarmIntents)
            {
                readAlarm(alarmIntent, false);
            }
        }
        catch (Exception e)
        {
            Log.e("MainActivity", "Error during reading all alarms: " + e.getMessage());
        }
    }

    private void readAlarm(AlarmIntent alarmIntent, boolean nowSet)
    {
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
                toSay += " In";
                int hours = (int) (totalMillis / 1000) / 3600;
                if (hours > 1)
                    toSay += hours + " hour" + (hours > 1 ? "s" : "");
                int minutes = ((int) (totalMillis / 1000) / 60) % 60;
                int seconds = (int) (totalMillis / 1000) % 60;
                toSay += minutes + "minute" + (minutes != 1 ? "s" : "") + " and" + seconds +" second" + (seconds != 1 ? "s" : "");
            }
        }
        else
        {
            if (alarmIntent.alarmTime.get(Calendar.DATE) == now.get(Calendar.DATE))
                toSay += "Today ";
            else if (alarmIntent.alarmTime.get(Calendar.DATE) == now.get(Calendar.DATE) + 1)
                toSay += "Tomorrow ";
            else
                toSay = "On " + alarmIntent.alarmTime.toString();
            toSay += "at: " + sdfForReadingAloud.format(alarmIntent.alarmTime.getTime());
        }

        toSay += " with " + (alarmIntent.ringBell ? "a" : "no") + " bell.";
        if (!alarmIntent.toSay.isEmpty())
            toSay += " Message:" + alarmIntent.toSay;
        speakThis(toSay);
    }

    public void removeOldAlarms()
    {
        synchronized (alarmIntents)
        {
            Log.d("MainActivity", "Removing Old Alarms");
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

    public void onDeleteAllClicked(View view)
    {
        synchronized (alarmIntents)
        {
            Log.d("MainActivity", "Deleting All Alarms");
            for (AlarmIntent alarmIntent : alarmIntents)
            {
                alarmManager.cancel(alarmIntent.pendingIntent);
            }
            alarmIntents.clear();
        }
        tts.speak("All alarms deleted", TextToSpeech.QUEUE_FLUSH, null);
    }

    public void speakThis(String toSay)
    {
        if (tts == null)
            initializeTTS();
        tts.speak(toSay, TextToSpeech.QUEUE_ADD, null);
    }

}
