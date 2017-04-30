package com.azariaa.lia.liaClient;

import android.content.Context;
import android.util.Log;

import com.azariaa.pocketSphinxBridge.PocketSphinxSearcher;

import com.azariaa.lia.Consts;

public class InMindCommandListener
{

    interface InmindCommandInterface
    {
        void commandDetected();
    }

    InmindCommandInterface minmindCommandInterface;
    Context context;
    PocketSphinxSearcher pocketSphinxSearcher = null;

    public boolean listeningForCommand()
    {
        return isListeningForCommand;
    }

    boolean isListeningForCommand = false;

    InMindCommandListener(InmindCommandInterface inmindCommandInterface, final Context context)
    {
        minmindCommandInterface = inmindCommandInterface;
        this.context = context;

        pocketSphinxSearcher = new PocketSphinxSearcher(context, Consts.agentNameKeyword, new PocketSphinxSearcher.SphinxRes(){

            int i =0;
            @Override
            public void keyDetected() {
                minmindCommandInterface.commandDetected();
            }});
    }

    public void stopListening()
    {
        if (isListeningForCommand)
        {
            Log.d("listener","stopped listening for command.");
            pocketSphinxSearcher.stopListening();
            isListeningForCommand = false;
        }
    }

    public void listenForInmindCommand()
    {
        if (!isListeningForCommand)
        {
            Log.d("listener","listening for command.");
            pocketSphinxSearcher.startListeningForKeyword();
            isListeningForCommand = true;
        }
    }
}
