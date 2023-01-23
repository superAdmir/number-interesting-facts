package com.nip.numberinterestingfacts;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ExitActivity extends AppCompatActivity {
    private Button yesButton, noButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exit);
        //Initialize the objects
        yesButton = (Button) findViewById(R.id.yes_button);
        noButton = (Button) findViewById(R.id.no_button);
    }

    //Method is used to open activity Random by pressing button No
    private void openActivityRandom() {
        Intent intent = new Intent(this, RandomActivity.class);
        //Close all previous activities
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
        //Click listener is used to exit from the application by pressing Yes button
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                finish();
                System.exit(0);
            }
        });

        //Click listener is used to cancel exiting from the application by pressing No button
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                openActivityRandom();
                finish();
            }
        });
    }
}