package com.azariaa.lia.liaClient;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * All this does is turns on the screen and then finishes the activity after 3 seconds.
 * Note that if we remove the delay it won't turn on the screen.
 * Also, if using android:theme="@android:style/Theme.NoDisplay", screen doesn't turn on either.
 * Is used for Sugilite which needs the screen to be on to operate.
 */
public class ScreenOn extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_on);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Intent intent = getIntent();
        final String launchAfterScreenOn = intent.getStringExtra("launchAfterScreenOn");
        final String arg = intent.getStringExtra("arg");
        Thread finishWithDelay = new Thread(new Runnable() {
            @Override
            public void run(){
                try
                {
                    Thread.sleep(1000);//(3000)
                }
                catch (Exception ignored)
                {
                }
                if (launchAfterScreenOn.equals("YouTube"))
                {
                    String videoId = arg;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("VIDEO_ID", videoId);
                    startActivity(intent);

                }
                finish();
            }
        });
        finishWithDelay.start();
    }
}
