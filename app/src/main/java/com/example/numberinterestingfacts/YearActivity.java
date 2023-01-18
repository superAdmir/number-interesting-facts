package com.example.numberinterestingfacts;

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

public class YearActivity extends AppCompatActivity {
    private TextView resultsField;
    private EditText editTextNumber;
    private ImageView menuIcon, exitIcon;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private Button randomDownButton, randomTopButton, dateButton, mathButton, searchButton, yearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_year);
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
        yearButton.setClickable(false);
        yearButton.setBackgroundColor(Color.parseColor("#cccccc"));
        yearButton.setTextColor(Color.parseColor("#666666"));
        getDataYear();
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

    //Method is used to call random year api by clicking on Random Year button
    private void getDataYear() {
        //Create a String request using Volley Library
        String myUrl = "http://numbersapi.com/random/year?json";
        StringRequest myRequest = new StringRequest(Request.Method.GET, myUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    //Create a JSON object containing information from the API.
                    JSONObject myJsonObject = new JSONObject(response);
                    resultsField.setText(myJsonObject.getString("text"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(YearActivity.this, volleyError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(myRequest);
    }

    //Method is used to call year api by passing number parameter and clicking on Search button
    private void getDataSearchRandom() {
        //Create a String request using Volley Library
        editTextNumber = (EditText) findViewById(R.id.editTextNumber);
        String edit_text_data = editTextNumber.getText().toString();
        String myUrl = "http://numbersapi.com/" + edit_text_data + "/year?json";
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
                Toast.makeText(YearActivity.this, volleyError.getMessage(), Toast.LENGTH_SHORT).show();
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

    //Method is used to open activity Random by pressing button Random
    private void openActivityRandom() {
        Intent intent = new Intent(this, RandomActivity.class);
        //Close previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    //Method is used to open activity Date by pressing button Date
    private void openActivityDate() {
        Intent intent = new Intent(this, DateActivity.class);
        //Close previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    //Method is used to open activity Math by pressing button Math
    private void openActivityMath() {
        Intent intent = new Intent(this, MathActivity.class);
        //Close previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //Click listener is used to call getDataYear method to call Year api by pressing Random Year button
        randomDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataYear();
            }
        });

        //Click listener is used to call openActivityRandom method to open Random activity by pressing Random button
        randomTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityRandom();
            }
        });

        //Click listener is used to call openActivityDate method to open Date activity by press Date button
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityDate();
            }
        });

        //Click listener is used to call openActivityMath method to open Math activity by press Math button
        mathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityMath();
            }
        });

        //Click listener is used to call getDataSearchRandom method to call Year api by passing number parameter and clicking on Search button
        //Click listener is used to call closeKeyboard method to close keyboard by pressing somewhere on the screen
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editTextNumber = (EditText) findViewById(R.id.editTextNumber);
                String sUsername = editTextNumber.getText().toString();
                if (sUsername.matches("")) {
                    Toast.makeText(YearActivity.this, "Please enter year value!", Toast.LENGTH_SHORT).show();
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
                mInterstitialAd.show(YearActivity.this);
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        startActivity(new Intent(YearActivity.this, ExitActivity.class));
                        finish();
                        System.exit(0);
                    }
                });
            }
        });
    }
}