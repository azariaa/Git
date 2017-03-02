package com.inMind.inMindAgent;

import android.app.Activity;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import InMind.Consts;

public class ComWithSugilite extends Activity
{

    public static final int FINISHED_RECORDING = 19;
    public static final int RUN_JSON_EXCEPTION = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("InMind ComWithSugilite", "starting onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com_with_sugilite);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            Object messageType = extras.get("messageType");
            int messageTypeAsInt = 0; //supporting string for legacy Sugilite
            if (messageType != null)
            {
                if (messageType instanceof String)
                {
                    if (messageType.toString().equals("FINISHED_RECORDING"))
                        messageTypeAsInt = FINISHED_RECORDING;
                    else if (messageType.toString().equals("RUN_JSON_EXCEPTION"))
                        messageTypeAsInt = RUN_JSON_EXCEPTION;
                }
                else
                {
                    messageTypeAsInt = extras.getInt("messageType");
                }
            }
            String messageBody = extras.getString("messageBody");
            Log.d("InMind ComWithSugilite", " messageTypeAsInt = " + messageTypeAsInt + " messageBody=" + messageBody);
            if (MainActivity.selfPointer != null)
            {
                if (messageTypeAsInt == FINISHED_RECORDING)
                {
                    MainActivity.selfPointer.logicController.ConnectToServer("FINISHED_RECORDING" + ":" + messageBody, true);
                }
                if (messageTypeAsInt == RUN_JSON_EXCEPTION)
                {
                    String toSay = "Can't launch Sugilite, this is most likely because Sugilite accessibility service is not enabled, please enable it at the accessibility tab under settings.";
                    Log.w("ComWithSugilite", "can't launch Sugilite");
                    Log.d("ComWithSugilite", "saying: " + toSay);
                    Message msgTalk = new Message();
                    msgTalk.arg1 = 3; //say aloud and toast
                    msgTalk.obj = toSay;
                    MainActivity.selfPointer.talkHandler.sendMessage(msgTalk);
                }
            }
        }
        finish();
    }
}
