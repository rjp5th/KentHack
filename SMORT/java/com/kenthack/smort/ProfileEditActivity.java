package com.kenthack.smort;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class ProfileEditActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private boolean unsaved = false;
    Context context = this;
    String[] numberDependents = {"0","1", "2", "3", "4", "5", "6"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        this.setTitle("Create/Edit Profile");
        Spinner spin = (Spinner) findViewById(R.id.spinner);
        spin.setOnItemSelectedListener(this);

//Creating the ArrayAdapter instance having the bank name list
        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, numberDependents);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//Setting the ArrayAdapter data on the Spinner
        spin.setAdapter(aa);

        Button twoAct = findViewById(R.id.saveButton);
        twoAct.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                saveInfo(view);
                ProfileEditActivity.this.finish();
            }

        });
        String[] readSave = readFromFile(this).split(";", 6);
        if(readSave.length == 6) {
            TextView firstNameTV = (TextView) findViewById(R.id.firstName);
            firstNameTV.setText(readSave[1]);
            TextView lastNameTV = (TextView) findViewById(R.id.lastName);
            lastNameTV.setText(readSave[0]);
            RadioGroup radioGroup = findViewById(R.id.genderField);
            switch (readSave[2]) {
                case "Male":
                    radioGroup.check(R.id.maleButton);
                    break;
                case "Female":
                    radioGroup.check(R.id.femaleButton);
                    break;
                default:
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Failed to Load Information",
                            Toast.LENGTH_SHORT);

                    toast.show();
                    break;

            }
            Spinner dependentsSpinner = findViewById(R.id.spinner);
            dependentsSpinner.setSelection(Integer.parseInt(readSave[3]));
            TextView phoneNumber = (TextView) findViewById(R.id.editText12);
            phoneNumber.setText(readSave[4]);
            TextView address = (TextView) findViewById(R.id.editText13);
            address.setText(readSave[5]);
        }
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (isAltered()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileEditActivity.this);

                    builder.setTitle("Not Saved");
                    builder.setMessage("Are you sure you want to exit without saving?");
                    builder.setPositiveButton("Discard Changes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ProfileEditActivity.this.finish();

                        }
                    });
                    builder.setNegativeButton("Go Back", null);
                    builder.show();
                } else {
                    ProfileEditActivity.this.finish();
                }
            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, callback);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //String enteredUserName = ((EditText) findViewById(R.id.editText12)).getText().toString();

//    Button twoAct = (Button) findViewById(R.id.saveButton);

    public void saveInfo(View view) {
        String forFile = "";
        EditText lastNameField = findViewById(R.id.lastName);
        String lastName = lastNameField.getText().toString();
        if(lastName.length() == 0)
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Please Enter a Last Name",
                    Toast.LENGTH_SHORT);

            toast.show();
            return;
        }
        forFile += lastName + ";";
        EditText firstNameField = findViewById(R.id.firstName);
        String firstName = firstNameField.getText().toString();
        if(firstName.length() == 0)
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Please Enter a First Name",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
        forFile += firstName + ";";
        RadioGroup radioGroup = findViewById(R.id.genderField);
        switch (radioGroup.getCheckedRadioButtonId()){
            case R.id.maleButton:
                forFile += "Male;";
                break;
            case R.id.femaleButton:
                forFile += "Female;";
                break;
            default:
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Please Select a Gender",
                        Toast.LENGTH_SHORT);

                toast.show();
                return;
        }
        Spinner dependentsSpinner = findViewById(R.id.spinner);
        forFile += (dependentsSpinner.getSelectedItemPosition()) + ";";
        EditText phoneNumberField = findViewById(R.id.editText12);
        String phoneNumber = phoneNumberField.getText().toString();
        if(phoneNumber.length() == 0)
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Please Enter a Phone Number",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
        forFile += phoneNumber + ";";
        EditText addressField = findViewById(R.id.editText13);
        String address = addressField.getText().toString();
        if(address.length() == 0)
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Please Enter an Address",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
        forFile += address;
        writeToFile(forFile, this);



    }

    private String readFromFile(Context context) {
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

    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("person.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    public boolean isAltered(){
        EditText lastNameField = findViewById(R.id.lastName);
        String lastName = lastNameField.getText().toString();
        EditText firstNameField = findViewById(R.id.firstName);
        String firstName = firstNameField.getText().toString();
        RadioGroup radioGroup = findViewById(R.id.genderField);
        Spinner dependentsSpinner = findViewById(R.id.spinner);
        EditText phoneNumberField = findViewById(R.id.editText12);
        String phoneNumber = phoneNumberField.getText().toString();
        EditText addressField = findViewById(R.id.editText13);
        String address = addressField.getText().toString();
        String[] readSave = readFromFile(this).split(";", 6);
        if (readSave.length != 6){
            return true;
        }
        if(!lastName.equals(readSave[0])){
            return true;
        }
        if(!firstName.equals(readSave[1]))
        {
            return true;
        }
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.maleButton:
                if (!readSave[2].equals("Male")) {
                    return true;
                }
                break;
            case R.id.femaleButton:
                if (!readSave[2].equals("Female")) {
                    return true;
                }
                break;
        }
        if(!Integer.toString(dependentsSpinner.getSelectedItemPosition()).equals(readSave[3]))
        {
            return true;
        }
        if(!phoneNumber.equals(readSave[4]))
        {
            return true;
        }
        if(!address.equals(readSave[5]))
        {
            return true;
        }

        return false;
    }

}
