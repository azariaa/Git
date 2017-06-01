package com.azariaa.lia.liaClient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.azariaa.lia.Consts;


public class ChangeWakeupActivity extends ActionBarActivity
{
    EditText wakeupPhraseEditText;
    NumberPicker sensitivityPicker;
    CheckBox shouldListenForWakeup;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_wakeup);
        wakeupPhraseEditText = ((EditText)findViewById(R.id.wakeup_phrase));
        wakeupPhraseEditText.setText(getIntent().getStringExtra("current_wakeup"));
        sensitivityPicker = ((NumberPicker)findViewById(R.id.sensitivityPicker));
        sensitivityPicker.setMinValue(1);
        sensitivityPicker.setMaxValue(200);
        sensitivityPicker.setValue(getIntent().getIntExtra("current_sensitivity", 1));
        shouldListenForWakeup = ((CheckBox)findViewById(R.id.shouldListenCheckBox));
        shouldListenForWakeup.setChecked(getIntent().getBooleanExtra("current_isListening", false));

        wakeupPhraseEditText.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                shouldListenForWakeup.setChecked(true);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });
        sensitivityPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                shouldListenForWakeup.setChecked(true);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateWakeupWord(View v)
    {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("wakeupWord", wakeupPhraseEditText.getText().toString());
        returnIntent.putExtra("sensitivity", sensitivityPicker.getValue());
        returnIntent.putExtra("shouldListen", shouldListenForWakeup.isChecked());
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void restoreDefault(View view)
    {
        sensitivityPicker.setValue(Consts.defaultWakeupSensitivity);
        wakeupPhraseEditText.setText(Consts.defaultWakeupWord);
    }
}
