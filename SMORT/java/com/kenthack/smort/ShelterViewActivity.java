package com.kenthack.smort;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ShelterViewActivity extends AppCompatActivity {
    public MapsActivity.ShelterData shelter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelter_view);
        this.setTitle("Shelter Information" + (!MapsActivity.sheltersOnline ? " *OFFLINE*" : ""));


        Intent intent = getIntent();
        String shelterName = intent.getStringExtra(MapsActivity.PLNAME_MESSAGE);

        if (!MapsActivity.sheltersInitialized){
            Toast.makeText(getApplicationContext(), "Failed to Load Shelters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MapsActivity.ShelterData selectedShelter = null;

        for (MapsActivity.ShelterData shelterItem : MapsActivity.shelters){
            if (shelterItem.name.equals(shelterName)){
                selectedShelter = shelterItem;
            }
        }
        if (selectedShelter == null) {
            Toast.makeText(getApplicationContext(), "Failed to Find Shelter" + shelterName, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        shelter = selectedShelter;

        TextView shelterOccupancy = findViewById(R.id.occupantsOverCap);
        if (MapsActivity.sheltersOnline)
            shelterOccupancy.setText(shelter.occupantCount + " / " + shelter.capacity);
        else
            shelterOccupancy.setText("~" + shelter.predictOccupantCount() + " / " + shelter.capacity);
        TextView shelterNameText = findViewById(R.id.shelterName);
        shelterNameText.setText(shelter.name);
        TextView shelterDesc = findViewById(R.id.description);
        shelterDesc.setText(shelter.description);

        Resources res = getResources();
        final ProgressBar mProgress = (android.widget.ProgressBar) findViewById(R.id.circularProgressbar);
        Drawable drawable = res.getDrawable(R.drawable.circular);
        if (MapsActivity.sheltersOnline)
            mProgress.setProgress((int) Math.ceil((((double) shelter.occupantCount) / ((double) shelter.capacity))*100));   // Main Progress
        else
            mProgress.setProgress((int) Math.ceil((((double) shelter.predictOccupantCount()) / ((double) shelter.capacity))*100));   // Main Progress
//mProgress.setSecondaryProgress(); // Secondary Progress
        mProgress.setMax(100); // Maximum Progress
        mProgress.setProgressDrawable(drawable);
    }

}
