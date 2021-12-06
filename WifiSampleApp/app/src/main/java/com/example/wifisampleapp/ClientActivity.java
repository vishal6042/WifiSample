package com.example.wifisampleapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ClientActivity extends AppCompatActivity {
    private static final int PORT_NUMBER = 8085;
    private EditText hostAddress;
    private Button connectButton;
    private Button disconnectButton;
    private Spinner spinner;
    private Button sendData;
    private TextView data;
    private ArrayAdapter<String> myAdapter;
    private ArrayList<String> bitRate = new ArrayList<>(Arrays.asList("15 KBPS", "30 KBPS", "60 KBPS", "150 KBPS"));
    private final int bitRateInt[] = {15, 30, 60, 150};
    private int selectedBitRate = 15;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private static final String TAG = "[Wifi] Client";
    private boolean isStopped = true;

    String data15KB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        spinner = findViewById(R.id.bitrate);
        hostAddress = findViewById(R.id.ipAddress);
        sendData = findViewById(R.id.start_sending);
        connectButton = findViewById(R.id.connect);
        disconnectButton = findViewById(R.id.disconnect);
        data = findViewById(R.id.recv_data);

        data.setMovementMethod(new ScrollingMovementMethod());
        myAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bitRate);
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(myAdapter);
        generateRandomData();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), bitRate.get(position), Toast.LENGTH_LONG)
                        .show();
                selectedBitRate = bitRateInt[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        connectButton.setOnClickListener(v -> {
            new Thread(() -> {

                try {
                    socket = new Socket(hostAddress.getText().toString(), PORT_NUMBER);
                    Log.d(TAG, "onCreate: connected to " + socket);


                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());

                    new Thread(new IncomingMessageRunnable()).start();

                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(),
                                "Connected to Server",
                                Toast.LENGTH_SHORT).show();
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        });

        disconnectButton.setOnClickListener(v -> {
            data.setText("");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sendData.setOnClickListener(v -> {
            if (isStopped) {
                sendData.setText("Stop");
                new Thread(new SendingMessageRunnable()).start();
            } else {
                isStopped = true;
                sendData.setText("Start Sending Data");
            }
        });

    }

    private void generateRandomData() {
        byte[] array = new byte[1024];
        new Random().nextBytes(array);
        String str = new String(array, StandardCharsets.UTF_8);

        for (int i = 0; i < 15; i++) {
            data15KB += str;
        }
    }

    private long getSleepTime(int selectedBitRate) {
        if (selectedBitRate == 15) {
            return 1000;
        }
        if (selectedBitRate == 30) {
            return 500;
        }
        if (selectedBitRate == 60) {
            return 250;
        }
        if (selectedBitRate == 150) {
            return 100;
        }
        return 0;
    }

    private void updateMessage(String message) {
        runOnUiThread(() -> data.append("Message Coming from Server " + message.length() + "\n"));

    }

    private class SendingMessageRunnable implements Runnable {
        @Override
        public void run() {
            try {
                isStopped = false;
                while (true) {
                    outputStream.writeUTF(data15KB);
                    Thread.sleep(getSleepTime(selectedBitRate));
                    if (isStopped) {
                        break;
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class IncomingMessageRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String message = inputStream.readUTF();
                    updateMessage(message);
                }
            } catch (EOFException e) {

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}