package com.inMind.inMindAgent;

import java.util.ArrayList;
import java.util.Date;

import com.inMind.inMindAgent.InMindCommandListener.InmindCommandInterface;
//import com.yahoo.inmind.comm.generic.control.MessageBroker;

import InMind.Consts;
import InMind.simpleUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.CharacterPickerDialog;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * Created by Amos Azaria on 31-Dec-14.
 */
public class MainActivity extends AppCompatActivity
{
    public static MainActivity selfPointer = null; //Added to communicate with Sugilite, probably not the best way.

    TTScontroller ttsCont;
    LogicController logicController;
    InMindCommandListener inmindCommandListener;

    private ImageButton startButton, startFromCircle, stopKeyword;
    private Button stopButton;
    private RelativeLayout mainLayout;
    private ListView chatView;
    EditText editText;
    ArrayList<String> chatArray = new ArrayList<>();
    //ArrayAdapter<String> chatAdapter;
    ListViewCustomAdapter<String> chatAdapter;

    public Handler userNotifierHandler, talkHandler, launchHandler, ttsCompleteHandler; // TODO: should
    // these all be
    // combined to
    // one handler?
    private LogicController.syncNotifiers startStopRecNotifier;

    static final String userPrefix = "User: ";
    static final String agentPrefix = "Agent: ";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("Main", "onCreate");
        selfPointer = this;


        // Initializing the Message Broker
        //MessageBroker.getInstance(this);


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
                        guiTalk(true);
                    }
                    else
                    {
                        guiTalk(false);
                    }
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
                {
                    guiTalk(false);
                }
                return false;
            }

            private void guiTalk(boolean talking)
            {
                if (talking)
                {
                    ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.rec_recording);
                    mainLayout.setBackgroundColor(0x778800DD);
                    ttsCont.stop();

                    //turn on flashlight
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                //in newer versions (23 and up) can use CameraManager.setTorchMode instead and no need for camera permission
//                                    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//                                    String[] list = manager.getCameraIdList();
//                                    manager.setTorchMode(list[0], true);

                                final Camera cam = Camera.open();
                                cam.setPreviewTexture(new SurfaceTexture(312)); //this line was added to support Android 6.0
                                Camera.Parameters p = cam.getParameters();
                                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                cam.setParameters(p);
                                cam.startPreview();

                                new Thread()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            sleep(10);
                                            cam.stopPreview();
                                            cam.release();
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
                else
                {
                    ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.not_recording);
                    mainLayout.setBackgroundColor(0xFFFFFFFF);
                }
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
                chatArray.add(((msg.arg2 == 2) ? userPrefix : agentPrefix) + toSay);
                chatAdapter.notifyDataSetChanged();
                chatView.setSelection(chatAdapter.getCount() - 1);

                if ((msg.arg1 & 1) > 0) //speak
                {
                    ttsCont.speakThis(toSay);
                }
                if ((msg.arg1 & 2) > 0) //toast
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
                Log.d("handleMessage", "launchHandler arg1=" + msg.arg1 + " obj=" + msg.obj.toString());
                if (msg.arg1 == 1) //launch app
                {
                    // Pattern p = Pattern.compile("(.*)/(.*)");
                    // Matcher m = p.matcher(msg.obj.toString());
                    // m.find();
                    String appToLaunch = msg.obj.toString();
                    Intent intent;
                    if (appToLaunch.equalsIgnoreCase("InMind agent"))
                    {
                        intent = new Intent(MainActivity.this, MainActivity.class);
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
                        }
                        catch (Exception ex)
                        {
                            Log.e("MainActivity", "error starting activity" + ex.getMessage());
                        }
                    }

                }
                else if (msg.arg1 == 2) //Sugilite
                {
                    String sugiliteExtra = msg.obj.toString();
                    String[] sugiliteArgs = sugiliteExtra.split(Consts.messageSeparatorForPattern);
                    Log.d("handleMessage", "start: " + sugiliteArgs[0] + " " + sugiliteArgs[1]);
                    //Sugilite requires screen to be on in order to operate
                    turnOnScreen();
                    try
                    {
                        if (sugiliteArgs[0].equalsIgnoreCase(Consts.sugiliteStartRecording))
                        {
                            Log.d("handleMessage", "starting new recording named: " + sugiliteArgs[1]);
                            Intent intent = new Intent("edu.cmu.hcii.sugilite.COMMUNICATION");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("messageType", "START_RECORDING");
                            intent.putExtra("arg1"/*"scriptName"*/, sugiliteArgs[1]);
                            intent.putExtra("arg2"/*"callbackString"*/, "android.intent.action.ComWithSugilite");
                            MainActivity.this.startActivityForResult(intent, 1);
                        }
                        else if (sugiliteArgs[0].equalsIgnoreCase(Consts.sugiliteRun))
                        {
                            Log.d("handleMessage", "running script named: " + sugiliteArgs[1]);
                            Intent intent = new Intent("edu.cmu.hcii.sugilite.COMMUNICATION");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("messageType", "RUN_SCRIPT");
                            intent.putExtra("scriptName", sugiliteArgs[1]);
                            MainActivity.this.startActivityForResult(intent, 1);
                        }
                        else if (sugiliteArgs[0].equalsIgnoreCase(Consts.sugiliteExecJson))
                        {
                            Log.d("handleMessage", "running script using json");
                            Log.d("handleMessage", "json: " + sugiliteArgs[1]);
                            Intent intent = new Intent("edu.cmu.hcii.sugilite.COMMUNICATION");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("messageType", "RUN_JSON");
                            intent.putExtra("arg1"/*JSON*/, sugiliteArgs[1]);
                            intent.putExtra("arg2"/*callbackString*/, "android.intent.action.ComWithSugilite");
                            MainActivity.this.startActivityForResult(intent, 1);
                        }

                    }
                    catch (Exception e)
                    {
                        String toSay = "Can't launch Sugilite ";
                        Log.w("MainAtivity", "can't launch Sugilite");
                        if (e instanceof android.content.ActivityNotFoundException)
                            toSay += ", this is most likely because Sugilite is not installed, please install Sugilite.";
                        Log.d("MainAtivity", "saying: " + toSay);
                        Message msgTalk = new Message();
                        msgTalk.arg1 = 3; //say aloud and toast
                        msgTalk.obj = toSay;
                        talkHandler.sendMessage(msgTalk);
                    }
                }
                else if (msg.arg1 == 3) //youtube
                {
                    Log.d("handleMessage", "openning YouTube");
                    String videoId = msg.obj.toString();
                    Log.d("handleMessage", "videoId:" + videoId);
                    //YouTube requires screen to be on to work.
                    //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/" + videoId)));
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"+videoId));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("VIDEO_ID", videoId);
                    startActivity(intent);
                    turnOnScreen("YouTube", videoId);

                }
                return false;
            }
        });

//        clearScreenFlags = new Handler(new Handler.Callback()
//        {
//            @Override
//            public boolean handleMessage(Message message)
//            {
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
//                return false;
//            }
//        });

        startStopRecNotifier = new LogicController.syncNotifiers()
        {
            @Override
            public void startStopRec(boolean start)
            {
                if (start)
                    inmindCommandListener.stopListening();
                else if (isKeywordWakeupOn)
                    inmindCommandListener.listenForInmindCommand();
            }
        };


        editText = (EditText) findViewById(R.id.text_to_send);
        editText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void afterTextChanged(Editable e)
            {
                String textFromEditView = e.toString();
                if (textFromEditView.contains("\n"))
                    sendText(editText);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                //nothing needed here...
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
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
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean b)
            {
                chatView.setSelection(chatAdapter.getCount() - 1);
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
                    bringToForegroundAndTurnOnScreen();
                    connectAudioToServer();
                }
            }, this);
            inmindCommandListener.listenForInmindCommand();
        }

        startButton = (ImageButton) findViewById(R.id.button_rec);
        startFromCircle = (ImageButton) findViewById(R.id.image_recording);
        stopKeyword = (ImageButton) findViewById(R.id.listen_keyword);
        stopButton = (Button) findViewById(R.id.button_stop);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);

        startButton.setOnClickListener(startListener);
        startFromCircle.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);


        chatView = (ListView) findViewById(R.id.listView);
        chatAdapter = new ListViewCustomAdapter<String>(this, android.R.layout.simple_list_item_1, chatArray);
        //chatAdapter = new ListViewCustomAdapter<String>(this, R.layout.chat_list_item, chatArray);
        chatView.setAdapter(chatAdapter);

        chatView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
            {
                if (position >= 0 && chatView.getCount() > position)
                {
                    String selectedFromList = (chatView.getItemAtPosition(position)).toString();
                    if (selectedFromList.startsWith(userPrefix))
                        selectedFromList = selectedFromList.substring(userPrefix.length());
                    else if (selectedFromList.startsWith(agentPrefix))
                        selectedFromList = selectedFromList.substring(agentPrefix.length());
                    editText.setText(selectedFromList);
                }
            }
        });

        // minBufSize += 2048;
        // System.out.println("minBufSize: " + minBufSize);

        // attach a Message. set msg.arg to 1 and msg.obj to string for toast.
        Log.d("Main", "onCreate-End");
    }

    private void bringToForegroundAndTurnOnScreen()
    {
        bringApplicationToFront();
        turnOnScreen();
    }

    private void bringApplicationToFront()
    {

        Log.d("MainActivity", "====Bringging Application to Front====");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        try
        {
            pendingIntent.send();
        }
        catch (PendingIntent.CanceledException e)
        {
            Log.e("MainActivity", "PendingIntent.CanceledException while Bringging Application to Front");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void turnOnScreen()
    {
        turnOnScreen("", "");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void turnOnScreen(String launchAfterScreenOn, String arg)
    {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            return;
        try
        {
            DisplayManager dm = (DisplayManager) getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays())
            {
                if (display.getState() != Display.STATE_ON)
                {

                    Intent intent = new Intent(this, ScreenOn.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("launchAfterScreenOn", launchAfterScreenOn);
                    intent.putExtra("arg", arg);
                    MainActivity.this.startActivityForResult(intent, 1);
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            Log.w("MainAtivity", "exception while trying to turn on screen.");
        }

//        try
//        {
//            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
//            if (!pm.isScreenOn())
//            {
//                PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "LIA");
//                wakeLock.acquire();
//                wakeLock.release();
//            }
//        } catch (Exception ex)
//        {
//            Log.w("MainAtivity", "exception while trying to turn on screen.");
//        }

        //flags seems to be very hard...

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
////                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        Thread turnOfFlagsWithDelay = new Thread(new Runnable() {
//            @Override
//            public void run(){
//                try
//                {
//                    Thread.sleep(5000);
//                }
//                catch (Exception ignored)
//                {
//                }
//                clearScreenFlags.sendEmptyMessage(0);
//            }
//        });
//        turnOfFlagsWithDelay.start();

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
                logicController.changeInitIpAddr("34.193.23.122");
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
            intent.putExtra("ip", logicController.tcpIpAddr);
            startActivityForResult(intent, 1);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        if (requestCode == 1)
        {
            if (resultCode == RESULT_OK)
            {
                logicController.changeInitIpAddr(data.getStringExtra("ip"));
            }
            if (resultCode == RESULT_CANCELED)
            {
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
        logicController.ConnectToServer(toSay, false);
        //ttsCont.speakThis(toSay);
        //toastWithTimer(toSay, true);
    }

    public void sayYes(View view)
    {
        logicController.ConnectToServer("yes", false);
    }

    private final OnClickListener stopListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Stop Clicked");
            // audioStreamer.stopStreaming();
            logicController.stopStreaming();
            ttsCont.stop();
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

    boolean isKeywordWakeupOn = true;

    public void toggleListenKeyword(View view)
    {
        if (isKeywordWakeupOn)
        {
            stopKeyword.setImageResource(R.drawable.start_listening_keyword);
            inmindCommandListener.stopListening();
        }
        else
        {
            stopKeyword.setImageResource(R.drawable.stop_listening_keyword);
            inmindCommandListener.listenForInmindCommand();
        }
        isKeywordWakeupOn = !isKeywordWakeupOn;
    }


    @Override
    public void onDestroy()
    {
        logicController.stopStreaming();
        inmindCommandListener.stopListening();
        ttsCont.close();
        //MessageBroker.getInstance(this).destroy();
        super.onDestroy();
    }
}
