package com.inMind.inMindAgent;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import InMind.Consts;

public class ComWithSugilite extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("InMind ComWithSugilite", "starting onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com_with_sugilite);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            String messageType = extras.getString("messageType");
            String messageBody = extras.getString("messageBody");
            Log.d("InMind ComWithSugilite", " messageType = " + messageType + " messageBody=" + messageBody);
            if (messageType != null && messageType.equals("FINISHED_RECORDING"))
            {
                if (MainActivity.selfPointer != null) //TODO: try finding a better way to do this
                    MainActivity.selfPointer.logicController.ConnectToServer(messageType + ":" + messageBody, true);
            }
        }
        finish();
    }
}
