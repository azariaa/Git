package com.inMind.inMindAgent;

import java.util.ArrayList;
import java.util.Date;

import com.inMind.inMindAgent.InMindCommandListener.InmindCommandInterface;
import com.yahoo.inmind.comm.generic.control.MessageBroker;

import InMind.simpleUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by Amos Azaria on 31-Dec-14.
 */
public class MainActivity extends AppCompatActivity
{

    TTScontroller ttsCont;
    LogicController logicController;
    InMindCommandListener inmindCommandListener;

    private ImageButton startButton, startFromCircle;
    private Button stopButton;
    private ListView chatView;
    EditText editText;
    ArrayList<String> chatArray = new ArrayList<>();
    //ArrayAdapter<String> chatAdapter;
    ListViewCustomAdapter<String> chatAdapter;

    private Handler userNotifierHandler, talkHandler, launchHandler, ttsCompleteHandler; // TODO: should
    // these all be
    // combined to
    // one handler?
    private LogicController.syncNotifiers startStopRecNotifier;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("Main", "onCreate");


        // Initializing the Message Broker
        MessageBroker.getInstance( this );


        userNotifierHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {
                if (msg.arg1 == 1)
                {
                    boolean important = msg.arg2 == 1;
                    String toToast = msg.obj.toString();
                    toastWithTimer(toToast, important);
                    if (toToast.equals("Talk!")) //if needs to talk, set recording image. //TODO: should be done nicer (all strings should be refactorred).
                    {
                        ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.rec_recording);
                    }
                    else
                        ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.not_recording);
                }
                else if (msg.arg1 == 2)
                {
                    Log.d("Main", "Playing notification");
                    Uri notification = RingtoneManager
                            .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(
                            MainActivity.this, notification);
                    r.play();
                }
                else if (msg.arg1 == 0)
                    ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.not_recording); //just turn off recording image.
                return false;
            }

        });

        ttsCompleteHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {

                boolean isReconnecting = logicController.reconnectIfNeeded();
                return false;
            }
        });

        talkHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {

                String toSay = msg.obj.toString();
                //chatView.setTextColor(Color.RED);
                chatArray.add(((msg.arg2 == 2) ? "User: " : "Agent: ") + toSay);
                chatAdapter.notifyDataSetChanged();
                chatView.setSelection(chatAdapter.getCount() - 1);

                if ((msg.arg1 & 1) > 0 ) //speak
                {
                    ttsCont.speakThis(toSay);
                }
                if ((msg.arg1 & 2) > 0 ) //toast
                {
                    toastWithTimer(toSay, true);
                }
                return false;
            }
        });

        launchHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {
                if (msg.arg1 == 1)
                {
                    // Pattern p = Pattern.compile("(.*)/(.*)");
                    // Matcher m = p.matcher(msg.obj.toString());
                    // m.find();
                    String appToLaunch = msg.obj.toString();
                    Intent intent;
                    if (appToLaunch.equalsIgnoreCase("InMind agent"))
                    {
                        intent = new Intent(MainActivity.this,MainActivity.class);
                        //getconinical...
                        //intent = getIntent();
                    }
                    else if (appToLaunch.startsWith("http"))
                    {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(appToLaunch));
                    }
                    else
                    {
                        intent = MainActivity.this.getPackageManager()
                                .getLaunchIntentForPackage(appToLaunch);// m.group(1));
                    }
                    if (intent != null)
                    {
                        try
                        {
                            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            MainActivity.this.startActivity(intent);
                            Log.d("Main Activity", "Launching intent");
                        }catch (Exception ex)
                        {
                            Log.e("MainActivity","error starting activity" + ex.getMessage());
                        }
                    }

                }
                return false;
            }
        });

        startStopRecNotifier = new LogicController.syncNotifiers()
        {
            @Override
            public void startStopRec(boolean start)
            {
                if (start)
                    inmindCommandListener.stopListening();
                else
                    inmindCommandListener.listenForInmindCommand();
            }
        };

        editText = (EditText) findViewById(R.id.text_to_send);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                String textFromEditView = e.toString();
                if (textFromEditView.contains("\n"))
                    sendText(editText);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //nothing needed here...
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //nothing needed here...
            }
        });
        editText.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            sendText(v);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });


        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        //intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                {
                    Log.d("Main Activity", Intent.ACTION_SCREEN_OFF);
                    logicController.closeConnection();
                }
            }
        }, intentFilter);

        if (ttsCont == null)
        {
            ttsCont = new TTScontroller(this, ttsCompleteHandler);
        }

        if (logicController == null)
        {
            final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
            String uniqueId = tm.getDeviceId();
            if (uniqueId == null)
                uniqueId = tm.getSimSerialNumber();
            if (uniqueId == null)
                uniqueId = "errorId";
            logicController = new LogicController(userNotifierHandler, talkHandler,
                    launchHandler, startStopRecNotifier, uniqueId);
        }

        if (inmindCommandListener == null)
        {
            inmindCommandListener = new InMindCommandListener(new InmindCommandInterface()
            {

                @Override
                public void commandDetected()
                {
                    // TODO Auto-generated method stub
                    connectAudioToServer();
                }
            }, this);
            inmindCommandListener.listenForInmindCommand();
        }

        startButton = (ImageButton) findViewById(R.id.button_rec);
        startFromCircle  = (ImageButton) findViewById(R.id.image_recording);
        stopButton = (Button) findViewById(R.id.button_stop);

        startButton.setOnClickListener(startListener);
        startFromCircle.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);

        chatView = (ListView) findViewById(R.id.listView);
        chatAdapter = new ListViewCustomAdapter<String>(this, android.R.layout.simple_list_item_1, chatArray);
        //chatAdapter = new ListViewCustomAdapter<String>(this, R.layout.chat_list_item, chatArray);
        chatView.setAdapter(chatAdapter);

        // minBufSize += 2048;
        // System.out.println("minBufSize: " + minBufSize);

        // attach a Message. set msg.arg to 1 and msg.obj to string for toast.
        Log.d("Main", "onCreate-End");
    }


    void connectAudioToServer()
    {
        //inmindCommandListener.stopListening();
        logicController.ConnectToServer();
    }


    Date lastToastFinishes = new Date();

    private void toastWithTimer(String toToast, boolean important)
    {
        // toastCanceller.removeCallbacks(null);//make sure it won't be removed
        // by previous calls
        Date timeNow = new Date();
        boolean isAfter = timeNow.after(lastToastFinishes); //did we already pass the last toast finish time?

        int toastTime = important ? (int) ((toToast.length() / 75.0) * 2500 + 1000)
                : 1000;
        if (toastTime > 3500) // max toast time is 3500...
            toastTime = 3500;

        final int toastTimeFinal = toastTime;
        final Toast toast = Toast.makeText(this, toToast,
                Toast.LENGTH_LONG);


        if (isAfter)
        {
            toast.show();
            {
                Handler toastCanceller = new Handler();
                toastCanceller.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        toast.cancel();
                    }
                }, toastTimeFinal);
            }
            // set for when this toast will finish
            lastToastFinishes = simpleUtils.addMillisec(timeNow, toastTime);
        }
        else // if not, need to take care of delay for start as well
        {
            int startIn = simpleUtils.subtractDatesInMillisec(lastToastFinishes, timeNow);//lastToastFinishes - timeNow;
            Handler toastStarter = new Handler();
            toastStarter.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    toast.show();
                    Handler toastCanceller = new Handler();
                    toastCanceller.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            toast.cancel();
                        }
                    }, toastTimeFinal);

                }
            }, startIn);
            // set for when this toast will finish
            lastToastFinishes = simpleUtils.addMillisec(lastToastFinishes, toastTime);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if (id == R.id.action_toDesk)
        {
            if (logicController != null)
                logicController.changeInitIpAddr("128.2.213.163");
            return true;
        }
        else if (id == R.id.action_toLap)
        {
            if (logicController != null)
                logicController.changeInitIpAddr("128.2.209.220");
            return true;
        }
        else if (id == R.id.action_changeIp)
        {
            Intent intent = new Intent(this, IpEditActivity.class);
            intent.putExtra("ip",logicController.tcpIpAddr);
            startActivityForResult(intent,1);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == RESULT_OK)
            {
                logicController.changeInitIpAddr(data.getStringExtra("ip"));
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult

    /**
     * Called when the user clicks the send button
     */
    public void sendText(View view)
    {
        String toSay = editText.getText().toString();
        editText.getText().clear();
        findViewById(R.id.main_layout).requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        logicController.ConnectToServer(toSay);
        //ttsCont.speakThis(toSay);
        //toastWithTimer(toSay, true);
    }

    private final OnClickListener stopListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Stop Clicked");
            // audioStreamer.stopStreaming();
            logicController.stopStreaming();
            //inmindCommandListener.stopListening();
        }

    };

    private final OnClickListener startListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Start Clicked");
            // audioStreamer.startStreaming();
            connectAudioToServer();
        }

    };


    @Override
    public void onDestroy(){
        logicController.stopStreaming();
        inmindCommandListener.stopListening();
        MessageBroker.getInstance( this ).destroy();
        super.onDestroy();
    }
}
