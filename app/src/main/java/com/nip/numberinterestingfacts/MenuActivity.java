package com.nip.numberinterestingfacts;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class MenuActivity extends AppCompatActivity {
    TextView textView;
    StringBuilder text = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        //Initialize the objects
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        //Read and write text file
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("TermsAndCondition.txt")))) {
            //Do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                text.append(mLine);
                text.append('\n');
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error reading file!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            textView = (TextView) findViewById(R.id.textView);
            textView.setText((CharSequence) text);
        }
    }

    //Method is used to close keyboard after entering value in text field
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, RandomActivity.class);
        startActivity(intent);
        finish();
    }
}