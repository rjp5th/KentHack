package com.kenthack.smort;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener

{

    public final static String PLNAME_MESSAGE = "com.kenthack.smort.SELECTED_PIN";


    public static List<ShelterData> shelters;
    public static boolean sheltersInitialized = false;
    public static boolean sheltersOnline = true;
    public static int currentLocation = -1;
    public static boolean processingLocation = false;
    /*
    TODO: Thing to do
      - Back Buttons
     */

    @Override
    public void onLocationChanged(Location location) {

    }

    public static class HttpsTrustManager implements X509TrustManager {

        private static TrustManager[] trustManagers;
        private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

        @Override
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws java.security.cert.CertificateException {

        }

        @Override
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] x509Certificates, String s)
                throws java.security.cert.CertificateException {

        }

        public boolean isClientTrusted(X509Certificate[] chain) {
            return true;
        }

        public boolean isServerTrusted(X509Certificate[] chain) {
            return true;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return _AcceptedIssuers;
        }

        public static void allowAllSSL() {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

            });

            SSLContext context = null;
            if (trustManagers == null) {
                trustManagers = new TrustManager[]{new HttpsTrustManager()};
            }

            try {
                context = SSLContext.getInstance("TLS");
                context.init(null, trustManagers, new SecureRandom());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(context
                    .getSocketFactory());
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

        ShelterData(JSONObject shelterData) throws JSONException{
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

        public int predictOccupantCount(){
            double hours = ((System.currentTimeMillis() / 1000.0) - ((double) predictionStartDate)) / 3600.0;
            int prediction = (int) Math.round(hours * predictionCoefficient + predictionConstant);
            return (prediction > capacity ? capacity : prediction);
        }

        JSONObject toObject() throws JSONException{
            JSONObject object = new JSONObject();
            object.accumulate("id", shelterId);
            object.accumulate("name", name);
            object.accumulate("lat", latitude);
            object.accumulate("long", longitude);
            object.accumulate("description", description);
            object.accumulate("capacity", capacity);
            object.accumulate("occupantCount", occupantCount);
            object.accumulate("predictionCoefficient", predictionCoefficient);
            object.accumulate("predictionConstant", predictionConstant);
            object.accumulate("predictionStartDate", predictionStartDate);
            return object;
        }

        float getMarker(){
            double percentFilled;
            if (MapsActivity.sheltersOnline){
                percentFilled = (double) occupantCount / (double) capacity;
            } else {
                percentFilled = (double) predictOccupantCount() / (double) capacity;
            }

            if (percentFilled > 0.99){
                return BitmapDescriptorFactory.HUE_RED;
            } else if (percentFilled > 0.75){
                return BitmapDescriptorFactory.HUE_ORANGE;
            } else if (percentFilled > 0.5){
                return BitmapDescriptorFactory.HUE_YELLOW;
            } else {
                return BitmapDescriptorFactory.HUE_GREEN;
            }
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

    protected static JSONObject dumpShelters(List<ShelterData> shelterList) throws JSONException{
        JSONArray shelters = new JSONArray();
        for (ShelterData shelter : shelterList){
            shelters.put(shelter.toObject());
        }
        JSONObject wrapper = new JSONObject();
        wrapper.accumulate("data", shelters);
        return wrapper;
    }


    public GoogleMap mMap = null;
    public GoogleApiClient googleApiClient;
    public LocationRequest locationRequest;
    public Location lastLocation;
    private static final int Request_User_Location_Code = 99;

    public RequestQueue queue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
           checkUserLocationPermission();
        }

        queue = Volley.newRequestQueue(this);

        String s = ProfileActivity.readFromFile(this);
        String[] info = s.split(";");
        if (info.length == 6) {
            getLocation(info[1], info[0]);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            buildingGoogleApiClient();

            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {

                Intent intent = new Intent(MapsActivity.this, ShelterViewActivity.class);
                intent.putExtra(PLNAME_MESSAGE, marker.getTitle());
                startActivity(intent);


            }
        });

        refreshMap();
    }

    void refreshMap(){
        String url = "https://cscatsendpoint.hs.vc:2468/shelters_info";

        HttpsTrustManager.allowAllSSL();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            shelters = parseShelters(response);
                            writeToFile(dumpShelters(shelters).toString(), MapsActivity.this);
                            for (ShelterData shelter : shelters){
                                LatLng latLng = new LatLng(shelter.latitude, shelter.longitude);

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);
                                markerOptions.title(shelter.name);
                                long lastUpdate = System.currentTimeMillis();
                                while (processingLocation || System.currentTimeMillis()-lastUpdate > 2000){
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e){
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                                if (shelter.shelterId == currentLocation)
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                else
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(shelter.getMarker()));
                                mMap.addMarker(markerOptions);
                            }
                            sheltersInitialized = true;
                        } catch (JSONException e){
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Failed to Read Data",
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Failed to Connect",
                                Toast.LENGTH_SHORT);

                        toast.show();

                        try {
                            shelters = parseShelters(new JSONObject(readFromFile(MapsActivity.this)));
                            for (ShelterData shelter : shelters){
                                LatLng latLng = new LatLng(shelter.latitude, shelter.longitude);

                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);
                                markerOptions.title(shelter.name);
                                long lastUpdate = System.currentTimeMillis();
                                while (processingLocation || System.currentTimeMillis()-lastUpdate > 2000){
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e){
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                                if (shelter.shelterId == currentLocation)
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                else
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(shelter.getMarker()));
                                mMap.addMarker(markerOptions);
                            }
                            sheltersOnline = false;
                            MapsActivity.this.setTitle(MapsActivity.this.getTitle() + " *OFFLINE*");
                            sheltersInitialized = true;
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(),
                                    "Failed to Load Cache",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        queue.add(jsonObjectRequest);
    }

    void getLocation(final String firstName, final String lastName) {
        processingLocation = true;
        String url = "https://cscatsendpoint.hs.vc:2468/get_location";
        HttpsTrustManager.allowAllSSL();
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        try {
                            MapsActivity.currentLocation = Integer.parseInt(response);
                            System.out.println("Location: " + MapsActivity.currentLocation);
                        } catch (NumberFormatException e){
                            Toast.makeText(getApplicationContext(), "Failed to Decode Location", Toast.LENGTH_SHORT).show();
                        }
                        processingLocation = false;
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Unable to get Signed In Location", Toast.LENGTH_SHORT).show();
                        processingLocation = false;
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                // (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
                Map<String, String>  params = new HashMap<>();
                params.put("firstName", firstName);
                params.put("lastName", lastName);

                return params;
            }
        };
        queue.add(postRequest);
    }


    public boolean checkUserLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Request_User_Location_Code);
            else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},Request_User_Location_Code);
            return false;
        }
        else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case Request_User_Location_Code:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                    {
                        if(googleApiClient==null)
                        {
                            buildingGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else
                    Toast.makeText(this, "Permission Denied...", Toast.LENGTH_SHORT).show();
                return;
        }
    }


    protected synchronized void buildingGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }





    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationRequest=new LocationRequest();
        locationRequest.setInterval(1100);
        locationRequest.setFastestInterval(1100);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    /*@Override
    public void onLocationChanged(Location location)
    {
        lastLocation = location;

        if(currentUserLocationMarker != null)
        {
            currentUserLocationMarker.remove();
        }

        atLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("user Current Location");       //text for user location
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));    //set color of marker

        currentUserLocationMarker = mMap.addMarker(markerOptions);



        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(14));         //zoom




        if(googleApiClient !=null)
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);

        }
    }*/

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {

    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.info:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.restart:
                Intent mStartActivity = new Intent(this, MapsActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            default:
                return true;
        }

    }

    private String readFromFile(Context context) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(context.openFileInput("shelters.txt"));
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
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("shelters.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

}
