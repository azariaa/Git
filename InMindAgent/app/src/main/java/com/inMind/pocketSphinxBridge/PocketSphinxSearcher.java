package com.inMind.pocketSphinxBridge;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;
import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;



public class PocketSphinxSearcher {


    public interface SphinxRes
	{
		void keyDetected();				
	}
	
	private class MyListener implements RecognitionListener
	{
		SphinxRes sphinxRes = null;
		String keyPhrase;
		
		public MyListener(SphinxRes sphinxRes, String keyPhrase)
		{
			this.sphinxRes = sphinxRes;
			this.keyPhrase = keyPhrase;
		}
		
		public void testRes(Hypothesis hypothesis)
		{
            if (hypothesis != null)
            {
                String text = hypothesis.getHypstr();
                if (text.equals(keyPhrase) && sphinxRes != null)
                {
                    recognizer.stop(); //must stop and start to restart. otherwise will keep getting text.equals(keyPhrase)
                    recognizer.startListening(KWS_SEARCH);
                    Log.d("Sphinx", "keyword detected.");
                    sphinxRes.keyDetected();
                }
            }
		}

	    @Override
	    public void onPartialResult(Hypothesis hypothesis) {
            testRes(hypothesis);
	    }

	    @Override
	    public void onResult(Hypothesis hypothesis) {
	    	//testRes(hypothesis);
	    }

        @Override
        public void onError(Exception e)
        {

        }

        @Override
        public void onTimeout()
        {

        }

        @Override
	    public void onBeginningOfSpeech() {
	    }

	    @Override
	    public void onEndOfSpeech() {
	    }
		
	}

    private static final String KWS_SEARCH = "wakeup";
    //private static final String KEYPHRASE = "in mind agent";//"isteveni";

    private SpeechRecognizer recognizer;
    //private HashMap<String, Integer> captions;
    SphinxRes sphinxRes;
    String keyPhrase;
    Context context;

    final Object initializedLock = new Object();
    boolean wasInitialized = false;
    boolean startedInitializing = false;

    public PocketSphinxSearcher(Context contextvar, String keyPhrase, SphinxRes sphinxRes)
    {
    	this.sphinxRes = sphinxRes;
    	this.keyPhrase = keyPhrase;
    	this.context = contextvar;
    }
    
    public void startListeningForKeyword()
    {
        if (wasInitialized)
        {
            recognizer.startListening(KWS_SEARCH);
        }
        else
        {
            initializeAndListen();
        }
    }

    public void initializeAndListen()
    {
        if (!wasInitialized && !startedInitializing)
        {
            synchronized (initializedLock)
            {
                if (!startedInitializing)
                {
                    startedInitializing = true;
                    // Recognizer initialization is a time-consuming and it involves IO,
                    // so we execute it in async task

                    new AsyncTask<Void, Void, Exception>()
                    {
                        @Override
                        protected Exception doInBackground(Void... params)
                        {
                            try
                            {
                                Assets assets = new Assets(context);
                                File assetDir = assets.syncAssets();
                                setupRecognizer(assetDir);
                            }
                            catch (IOException e)
                            {
                                return e;
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Exception result)
                        {
                            if (result != null)
                            {
                            }
                            else
                            {
                                recognizer.startListening(KWS_SEARCH);
                                wasInitialized = true;
                            }
                        }
                    }.execute();
                }
            }
        }
    }


    private void setupRecognizer(File assetsDir)
    {
        File modelsDir = new File(assetsDir, "models");
        try
        {
            recognizer = defaultSetup()
                    .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                    .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                    .setFloat("-kws_threshold", 1e-45)//1e-320)//.setKeywordThreshold(1e-45f)//(1e-45f)//(1e-20f)
                    //.setRawLogDir(assetsDir) //takes a lot of space on phone (and also time?)
                    .getRecognizer();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        recognizer.addListener(new MyListener(sphinxRes, keyPhrase));

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, keyPhrase);
    }

    public void stopListening()
    {
        recognizer.stop();
    }
}
