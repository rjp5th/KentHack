package com.kenthack.receiver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ScanActivity extends AppCompatActivity {

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


    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        queue = Volley.newRequestQueue(this);
        loading();
    }

    public void loading(){
        TextView fullName = findViewById(R.id.occupantName);
        fullName.setText("Reading...");
        TextView totalAmount = findViewById(R.id.numberOfPeople);
        totalAmount.setText("");
        TextView ratio = findViewById(R.id.occupantsOverCap);
        ratio.setText("? / ?");
        Resources res = getResources();
        final ProgressBar mProgress = (android.widget.ProgressBar) findViewById(R.id.circularProgressbar);
        Drawable drawable = res.getDrawable(R.drawable.circular);
        mProgress.setProgress(0);   // Main Progress
        //mProgress.setSecondaryProgress(); // Secondary Progress
        mProgress.setMax(100); // Maximum Progress
        mProgress.setProgressDrawable(drawable);

        TextView signInOutBox = findViewById(R.id.signInOutStatus);
        if (MainActivity.getValue(this).mode == MainActivity.Mode.SIGN_IN) {
            signInOutBox.setText("Signing In");
            signInOutBox.setTextColor(Color.rgb(30, 76, 176));
        } else {
            signInOutBox.setText("Signing Out");
            signInOutBox.setTextColor(Color.rgb(255, 25, 25));
        }
    }


    public void updateName(String firstName, String lastName, int dependents){
        TextView fullName = findViewById(R.id.occupantName);
        fullName.setText(firstName + " " + lastName);
        TextView totalAmount = findViewById(R.id.numberOfPeople);
        if (dependents != -1)
            totalAmount.setText(Integer.toString(++dependents) + " People");
        else
            totalAmount.setText("");
    }

    public void updateOccupants(int occupants, int capacity){

        TextView ratio = findViewById(R.id.occupantsOverCap);
        ratio.setText(Integer.toString(occupants) + " / " + Integer.toString(capacity));
        Resources res = getResources();
        final ProgressBar mProgress = (android.widget.ProgressBar) findViewById(R.id.circularProgressbar);
        Drawable drawable = res.getDrawable(R.drawable.circular);
        mProgress.setProgress((int) Math.round(((double) occupants / (double) capacity) * 100));   // Main Progress
        //mProgress.setSecondaryProgress(); // Secondary Progress
        mProgress.setMax(100); // Maximum Progress
        mProgress.setProgressDrawable(drawable);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        if (rawMsgs == null || rawMsgs.length == 0){
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Unable to Read Beam",
                    Toast.LENGTH_SHORT);

            toast.show();
            return;
        }
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        if (MainActivity.getValue(this).mode == MainActivity.Mode.SIGN_IN) {
            try {
                signIn(new JSONObject(new String(msg.getRecords()[0].getPayload())));
            } catch (JSONException e) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Invalid Sign In Packet",
                        Toast.LENGTH_SHORT);

                toast.show();
            }
        } else{
            try {
                signOut(new JSONObject(new String(msg.getRecords()[0].getPayload())));
            } catch (JSONException e) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Invalid Sign Out Packet",
                        Toast.LENGTH_SHORT);

                toast.show();
            }
        }

    }

    void signIn(JSONObject object) throws JSONException {
        String url = "https://cscatsendpoint.hs.vc:2468/sign_in";
        final String locationId = Integer.toString(MainActivity.getValue(this).shelter);
        final String firstName = object.getString("firstName");
        final String lastName = object.getString("lastName");
        final String gender = object.getString("gender");
        final String dependents = Integer.toString(object.getInt("dependents"));
        final String phoneNumber = Integer.toString(object.getInt("phoneNumber"));
        final String address = object.getString("address");
        final String scanTime = Double.toString(System.currentTimeMillis() / 1000.0);
        HttpsTrustManager.allowAllSSL();
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        loadOccupants();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Unable to Submit Request",
                                Toast.LENGTH_SHORT);

                        toast.show();
                        loadOccupants();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                // (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
                Map<String, String>  params = new HashMap<>();
                params.put("locationId", locationId);
                params.put("firstName", firstName);
                params.put("lastName", lastName);
                params.put("gender", gender);
                params.put("dependents", dependents);
                params.put("phoneNumber", phoneNumber);
                params.put("address", address);
                params.put("scanTime", scanTime);

                return params;
            }
        };

        updateName(firstName, lastName, Integer.parseInt(dependents));
        queue.add(postRequest);
    }

    void signOut(JSONObject object) throws JSONException {
        String url = "https://cscatsendpoint.hs.vc:2468/sign_out";
        final String firstName = object.getString("firstName");
        final String lastName = object.getString("lastName");
        HttpsTrustManager.allowAllSSL();
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        loadOccupants();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (error.networkResponse != null && error.networkResponse.statusCode == 404)
                            Toast.makeText(getApplicationContext(), "Person does not exist", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getApplicationContext(), "Unable to Submit Request", Toast.LENGTH_SHORT).show();
                        loadOccupants();
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
        updateName(firstName, lastName, -1);
        queue.add(postRequest);
    }

    public void loadOccupants(){
        String url = "https://cscatsendpoint.hs.vc:2468/shelters_info";

        ScanActivity.HttpsTrustManager.allowAllSSL();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            List<MainActivity.ShelterData> shelters = MainActivity.parseShelters(response);
                            int requiredId = MainActivity.getValue(ScanActivity.this).shelter;
                            for (MainActivity.ShelterData shelter : shelters){
                                if (shelter.shelterId == requiredId) {
                                    updateOccupants(shelter.occupantCount, shelter.capacity);
                                }
                            }
                        } catch (JSONException e){
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Failed to Read Shelter Data",
                                    Toast.LENGTH_SHORT);
                            e.printStackTrace();

                            toast.show();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Failed to Connect",
                                Toast.LENGTH_SHORT);

                        toast.show();
                    }
                });
        queue.add(jsonObjectRequest);
    }
}
