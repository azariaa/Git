package com.inMind.inMindAgent;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

/**
 * Created by Amos Azaria on 01-Nov-14.
 */
public class TTScontroller {

	final boolean alwaysUseFlite = false;

	TextToSpeech ttobj;
	Context appContect;
	Handler mCallWhenDone;

	Integer lastMessageQueued = 0;

	// should be constructed in OnCreate
	public TTScontroller(Context appContext, Handler callWhenDone) {
		mCallWhenDone = callWhenDone;
		this.appContect = appContext;

		TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR) {
					ttobj.setLanguage(Locale.US);
					// ttobj.setPitch(3f);
					// ttobj.setSpeechRate(0.1f);

					ttobj.setOnUtteranceProgressListener(new UtteranceProgressListener() {

						@Override
						public void onDone(String utteranceId) {
							Log.e("TTScontroller", "OnDone called");
							if (utteranceId.equals(lastMessageQueued.toString())) {
								Message msg = new Message();
								msg.arg1 = 1;
								mCallWhenDone.sendMessage(msg);
							}
						}

						@Override
						public void onError(String utteranceId) {
						}

						@Override
						public void onStart(String utteranceId) {
						}
					});
				}
			}
		};

		if (alwaysUseFlite)
			ttobj = new TextToSpeech(appContext, listener, "edu.cmu.cs.speech.tts.flite");
		else
			ttobj = new TextToSpeech(appContext, listener);
	}

	@SuppressWarnings("deprecation")
	public void speakThis(String message) {

		Integer currentMessageId = ++lastMessageQueued;
		// Toast.makeText(appContect, message, Toast.LENGTH_SHORT).show();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
				currentMessageId.toString());
		ttobj.speak(message, TextToSpeech.QUEUE_ADD, map);
	}

}
