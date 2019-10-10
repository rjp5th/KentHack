package com.kenthack.smort;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


public class ProfileActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback{

    NfcAdapter nfcAdapter;
    String contents = "No Data to Transmit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        this.setTitle("My Profile");
        String[] readSave = readFromFile(this).split(";", 6);
        String s = readFromFile(this);
        String[] info = s.split(";");
        if (info.length != 6){
            Intent intent = new Intent(this, ProfileEditActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        TextView showName = findViewById(R.id.fullName);
        //TextView showSS = findViewById(R.id.showSS);
        TextView showPhone = findViewById(R.id.textView5);
        TextView showAddress = findViewById(R.id.address);
        TextView showDependents = findViewById(R.id.textView6);
        //TextView showgender = findViewById(R.id.showGender);

        showName.setText(info[1] + " " + info[0]);
        //showSS.setText(info[2]);
        showPhone.setText(info[4]);
        showAddress.setText(info[5]);
        showDependents.setText(info[3] + " Dependencies");

        JSONObject dataToTransmit = new JSONObject();
        try {
            dataToTransmit.accumulate("firstName", info[1]);
            dataToTransmit.accumulate("lastName", info[0]);
            dataToTransmit.accumulate("gender", info[2]);
            dataToTransmit.accumulate("dependents", info[3]);
            dataToTransmit.accumulate("phoneNumber", info[4]);
            dataToTransmit.accumulate("address", info[5]);
        } catch (JSONException e) {
            Toast.makeText(this, "Unable to convert to JSON", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        contents = dataToTransmit.toString();

        // Check for available NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Register callback
        nfcAdapter.setNdefPushMessageCallback(this, this);
    }


    public static String readFromFile(Context context) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(context.openFileInput("person.txt"));
        } catch (FileNotFoundException e) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NdefMessage msg = new NdefMessage(
                new NdefRecord[]{NdefRecord.createMime(
                        "application/vnd.com.kenthack.beam.person", contents.getBytes())
                        /*
                         * The Android Application Record (AAR) is commented out. When a device
                         * receives a push with an AAR in it, the application specified in the AAR
                         * is guaranteed to run. The AAR overrides the tag dispatch system.
                         * You can add it back in to guarantee that this
                         * activity starts when receiving a beamed message. For now, this code
                         * uses the tag dispatch system.
                        */
                        //,NdefRecord.createApplicationRecord("com.example.android.beam")
                });
        return msg;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.edit:
                intent = new Intent(this, ProfileEditActivity.class);
                startActivity(intent);
                return true;
            default:
                return true;
        }

    }
}
