package com.inMind.inMindAgent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ComWithSugilite extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com_with_sugilite);

        Bundle b = getIntent().getExtras();
    }
}
