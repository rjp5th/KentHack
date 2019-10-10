package com.kenthack.receiver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    public enum Mode{
        SIGN_IN, SIGN_OUT
    }

    private String[] shelterNames;
    private int[] shelterIds;
    private boolean sheltersInitialized = false;

    public static class ModeShelterData{
        final int shelter;
        final Mode mode;

        ModeShelterData(Mode mode, int shelter){
            this.shelter = shelter;
            this.mode = mode;

        }
    }

    public static class ShelterData {
        final int shelterId;
        final String name;
        final double latitude;
        final double longitude;
        final String description;
        final int capacity;
        final int occupantCount;
        final double predictionCoefficient;
        final double predictionConstant;
        final double predictionStartDate;

        ShelterData(JSONObject shelterData) throws JSONException {
            shelterId = shelterData.getInt("id");
            name = shelterData.getString("name");
            latitude = shelterData.getDouble("lat");
            longitude = shelterData.getDouble("long");
            description = shelterData.getString("description");
            capacity = shelterData.getInt("capacity");
            if (shelterData.get("occupantCount") == JSONObject.NULL)
                occupantCount = 0;
            else
                occupantCount = shelterData.getInt("occupantCount");
            predictionCoefficient = shelterData.getDouble("predictionCoefficient");
            predictionConstant = shelterData.getDouble("predictionConstant");
            predictionStartDate = shelterData.getDouble("predictionStartDate");
        }
    }

    protected static List<ShelterData> parseShelters(JSONObject object) throws JSONException {
        List<ShelterData> shelterList = new ArrayList<>();
        JSONArray payload = object.getJSONArray("data");
        for (int i = 0; i < payload.length(); i++){
            shelterList.add(new ShelterData(payload.getJSONObject(i)));
        }
        return shelterList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "https://cscatsendpoint.hs.vc:2468/shelters_info";

        Spinner shelterSelector = MainActivity.this.findViewById(R.id.shelterSelector);
        String[] error_data = new String[]{"Loading..."};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_item, error_data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shelterSelector.setAdapter(adapter);
        shelterSelector.setEnabled(false);

        Button applyModeSelection = findViewById(R.id.applyModeSelection);
        applyModeSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sheltersInitialized){
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Unable to Save",
                            Toast.LENGTH_SHORT);

                    toast.show();
                    return;
                }
                Spinner shelterSelector = MainActivity.this.findViewById(R.id.shelterSelector);
                int id = shelterIds[shelterSelector.getSelectedItemPosition()];
                Mode mode;
                RadioButton sign_in = findViewById(R.id.signIn);
                RadioButton sign_out = findViewById(R.id.signOut);
                if (sign_in.isChecked())
                    mode = Mode.SIGN_IN;
                else if (sign_out.isChecked())
                    mode = Mode.SIGN_OUT;
                else
                    mode = Mode.SIGN_IN;
                setValue(mode, id);
            }
        });

        switch (getValue(this).mode){
            case SIGN_IN:
                RadioButton sign_in = findViewById(R.id.signIn);
                sign_in.setChecked(true);
                break;
            case SIGN_OUT:
                RadioButton sign_out = findViewById(R.id.signOut);
                sign_out.setChecked(true);
                break;
        }

        ScanActivity.HttpsTrustManager.allowAllSSL();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Spinner shelterSelector = MainActivity.this.findViewById(R.id.shelterSelector);
                        try {
                            List<ShelterData> shelters = parseShelters(response);
                            shelterNames = new String[shelters.size()];
                            shelterIds = new int[shelters.size()];
                            for (int i = 0; i < shelters.size(); i++){
                                shelterNames[i] = shelters.get(i).name;
                                shelterIds[i] = shelters.get(i).shelterId;
                            }
                            sheltersInitialized = true;

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_item, shelterNames);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            shelterSelector.setAdapter(adapter);
                            int shelterId = getValue(MainActivity.this).shelter;

                            for (int i = 0; i < MainActivity.this.shelterIds.length; i++){
                                if (shelterId == MainActivity.this.shelterIds[i]){
                                    shelterSelector.setSelection(i);
                                }
                            }
                            shelterSelector.setEnabled(true);
                        } catch (JSONException e){
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Failed to Read Data",
                                    Toast.LENGTH_SHORT);
                            e.printStackTrace();

                            toast.show();

                            String[] error_data = new String[]{String.format(Locale.US, "Shelter %d: Error Reading", getValue(MainActivity.this).shelter)};
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_item, error_data);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            shelterSelector.setAdapter(adapter);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Failed to Connect",
                                Toast.LENGTH_SHORT);

                        error.printStackTrace();

                        toast.show();
                        Spinner shelterSelector = MainActivity.this.findViewById(R.id.shelterSelector);
                        String[] error_data = new String[]{String.format(Locale.US, "Shelter %d: Error Reading", getValue(MainActivity.this).shelter)};
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_item, error_data);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        shelterSelector.setAdapter(adapter);

                    }
                });
        queue.add(jsonObjectRequest);
    }

    public static ModeShelterData getValue(Context context){
        String contents = readFromFile(context);
        String[] data = contents.split(";");
        if (data.length != 2){
            Log.e("Receiver:MainActivity", "Failed to decode file");
            return new ModeShelterData(Mode.SIGN_IN, 1);
        }
        int shelterId = Integer.parseInt(data[0]);
        String mode = data[1];
        if (mode.equals("sign_in")){
            return new ModeShelterData(Mode.SIGN_IN, shelterId);
        } else if (mode.equals("sign_out")){
            return new ModeShelterData(Mode.SIGN_OUT, shelterId);
        } else {
            Log.e("Receiver:MainActivity", "Failed to decode mode");
            return new ModeShelterData(Mode.SIGN_IN, shelterId);
        }
    }

    public void setValue(Mode mode, int shelterId){
        if (mode.equals(Mode.SIGN_IN)){
            writeToFile(shelterId + ";sign_in", this);
        } else if (mode.equals(Mode.SIGN_OUT)){
            writeToFile(shelterId + ";sign_out", this);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Failed to Write to File",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
    }


    private static String readFromFile(Context context) {
        InputStreamReader reader;
        try {
            reader = new InputStreamReader(context.openFileInput("mode.txt"));
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
            Toast toast = Toast.makeText(context.getApplicationContext(),
                    "An IOException Occurred",
                    Toast.LENGTH_SHORT);

            toast.show();
            Log.e("Exception", "File write failed: " + e.toString());
        }

        return stringBuilder.toString();

    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("mode.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            Toast toast = Toast.makeText(context.getApplicationContext(),
                    "An IOException Occurred",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
    }
}


