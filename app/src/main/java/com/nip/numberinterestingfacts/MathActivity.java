package com.nip.numberinterestingfacts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class MathActivity extends AppCompatActivity {
    private TextView resultsField;
    private EditText editTextNumber;
    private ImageView menuIcon, exitIcon;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private Button randomDownButton, randomTopButton, yearButton, dateButton, searchButton, mathButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math);
        //Initialize the objects
        resultsField = (TextView) findViewById(R.id.results);
        randomDownButton = (Button) findViewById(R.id.randomDownButton);
        yearButton = (Button) findViewById(R.id.year);
        dateButton = (Button) findViewById(R.id.date);
        mathButton = (Button) findViewById(R.id.math);
        searchButton = (Button) findViewById(R.id.searchButton);
        randomTopButton = (Button) findViewById(R.id.randomTopButton);
        menuIcon = (ImageView) findViewById(R.id.menu_icon);
        exitIcon = (ImageView) findViewById(R.id.exit_icon);
        mathButton = (Button) findViewById(R.id.math);
        mathButton.setClickable(false);
        mathButton.setBackgroundColor(Color.parseColor("#cccccc"));
        mathButton.setTextColor(Color.parseColor("#666666"));
        getDataMath();
        setAdds();
        //Initialize the ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        //Load the ads
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    //Method is used to set up Interstitial ads
    private void setAdds() {
        //Load the ads
        AdRequest adRequest1 = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest1, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                mInterstitialAd = interstitialAd;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                mInterstitialAd = null;
            }
        });
    }

    //Method is used to call random math api by clicking on Random Math button
    private void getDataMath() {
        //Create a String request using Volley Library
        String myUrl = "http://numbersapi.com/random/math?json";
        StringRequest myRequest = new StringRequest(Request.Method.GET, myUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    //Create a JSON object containing information from the API.
                    JSONObject myJsonObject = new JSONObject(response.toString());
                    resultsField.setText(myJsonObject.getString("text"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(MathActivity.this, volleyError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(myRequest);
    }

    //Method is used to call math api by passing number parameter and clicking on Search button
    private void getDataSearchRandom() {
        //Create a String request using Volley Library
        editTextNumber = (EditText) findViewById(R.id.editTextNumber);
        String edit_text_data = editTextNumber.getText().toString();
        String myUrl = "http://numbersapi.com/" + edit_text_data + "/math?json";
        StringRequest myRequest = new StringRequest(Request.Method.GET, myUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    //Create a JSON object containing information from the API.
                    JSONObject myJsonObject = new JSONObject(response.toString());
                    resultsField.setText(myJsonObject.getString("text"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(MathActivity.this, volleyError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(myRequest);
    }

    //Method is used to close keyboard after entering value in text field
    private void closeKeyboard() {
        //This will give us the view which is currently focus in this layout
        View view = this.getCurrentFocus();
        //If nothing is currently focus then this will protect the app from crash
        if (view != null) {
            //Now assign the system service to InputMethodManager
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    //Method is used to open activity Menu by pressing Menu icon
    private void openActivityMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        //Close previous activities
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    //Method is used to close keyboard after entering value in text field
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, RandomActivity.class);
        startActivity(intent);
        finish();
    }

    //Method is used to open activity Year by pressing button Year
    private void openActivityYear() {
        Intent intent = new Intent(this, YearActivity.class);
        //Close previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //Method is used to open activity Date by pressing button Date
    private void openActivityDate() {
        Intent intent = new Intent(this, DateActivity.class);
        //Close previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //Method is used to open activity Random by pressing button Random
    private void openActivityRandom() {
        Intent intent = new Intent(this, RandomActivity.class);
        //Close previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //Click listener is used to call getDataMath method to call Math api by pressing Random Math button
        randomDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataMath();
            }
        });

        //Click listener is used to call openActivityYear method to open Year activity by pressing Random Year button
        yearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityYear();
            }
        });

        //Click listener is used to call openActivityDate method to open Date activity by pressing Random date button
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityDate();
            }
        });

        //Click listener is used to call openActivityRandom method to open Random activity by pressing Random button
        randomTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityRandom();
            }
        });

        //Click listener is used to call getDataSearchRandom method to call Math api by passing number parameter and clicking on Search button
        //Click listener is used to call closeKeyboard method to close keyboard by pressing somewhere on the screen
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editTextNumber = (EditText) findViewById(R.id.editTextNumber);
                String sUsername = editTextNumber.getText().toString();
                if (sUsername.matches("")) {
                    Toast.makeText(MathActivity.this, "Please enter numeric value!", Toast.LENGTH_SHORT).show();
                } else {
                    getDataSearchRandom();
                    closeKeyboard();
                }
            }
        });

        //Click listener is used to call openActivityMenu method to open Menu activity by press Menu icon
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityMenu();
            }
        });

        //Click listener is used to call openActivityExit method to open Exit activity by press Exit icon
        exitIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mInterstitialAd.show(MathActivity.this);
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        startActivity(new Intent(MathActivity.this, ExitActivity.class));
                        finish();
                        System.exit(0);
                    }
                });
            }
        });
    }
}