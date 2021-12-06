package com.example.wifisampleapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button clientButton;
    private Button serverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clientButton = findViewById(R.id.client);
        serverButton = findViewById(R.id.server);

        clientButton.setOnClickListener(view -> {
            startActivity(
                    new Intent(MainActivity.this, ClientActivity.class)
            );
        });

        serverButton.setOnClickListener((view -> {
            startActivity(
                    new Intent(MainActivity.this, ServerActivity.class)
            );
        }));

    }
}