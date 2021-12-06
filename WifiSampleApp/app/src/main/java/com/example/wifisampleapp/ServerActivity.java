package com.example.wifisampleapp;

import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerActivity extends AppCompatActivity {

    static final String TAG = "[Wifi] Server";
    private static final int PORT_NUMBER = 8085;
    private Spinner spinner;
    private EditText data;
    private Button sendData;
    private TextView incomingData;

    private ArrayAdapter<String> myAdapter;
    private ArrayList<String> deviceList = new ArrayList<>(Collections.singletonList("All"));
    private ServerSocket serverSocket;
    private Map<Socket, DataOutputStream> outputStreams = new HashMap();
    private Map<String, DataOutputStream> devices = new HashMap();
    private Map<Socket, String> devicesSocket = new HashMap();
    private String selectedDevice = "All";
    private StringBuilder mMessages = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        spinner = findViewById(R.id.deviceList);
        data = findViewById(R.id.data);
        sendData = findViewById(R.id.sendData);
        incomingData = findViewById(R.id.messages);
        incomingData.setMovementMethod(new ScrollingMovementMethod());
        myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, deviceList);
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(myAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), deviceList.get(position), Toast.LENGTH_LONG)
                        .show();
                selectedDevice = deviceList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sendData.setOnClickListener(v -> {
            String message = data.getText().toString() + " " + System.currentTimeMillis();
            synchronized (outputStreams) {
                if (selectedDevice.equals("All")) {
                    for (Map.Entry<Socket, DataOutputStream> entry : outputStreams.entrySet()) {
                        DataOutputStream outputStream = entry.getValue();
                        Socket socket = entry.getKey();
                        incomingData.setText(mMessages.toString());
                        sendMessage(outputStream, message);
                    }
                } else {
                    DataOutputStream outputStream = devices.get(selectedDevice);
                    incomingData.setText(mMessages.toString());
                    sendMessage(outputStream, message);
                }
            }
        });

        new Thread(() -> {
            startListeningOnPort(PORT_NUMBER);
        }).start();
    }

    private void sendMessage(DataOutputStream outputStream, String message) {

        new Thread(() -> {
            try {
                outputStream.writeUTF(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startListeningOnPort(int port) {
        try {
            serverSocket = new ServerSocket(port);
            Log.d(TAG, "startListeningOnPort: " + serverSocket);
            while (true) {
                Socket s = serverSocket.accept();
                Log.d(TAG, "Connection from " + s);
                DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());
                outputStreams.put(s, dataOutputStream);
                new ServerThread(mListener, s);
                String deviceName = s.getInetAddress().getHostAddress() + ":" + s.getPort();
                Log.d(TAG, "Connection from " + deviceName);
                devices.put(deviceName, dataOutputStream);
                devicesSocket.put(s, deviceName);
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Device connected " + deviceName, Toast.LENGTH_LONG).show();
                    deviceList.add(deviceName);
                    myAdapter.notifyDataSetChanged();
                    spinner.setAdapter(myAdapter);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ServerActivityListener mListener = new ServerActivityListener() {

        @Override
        public void onMessageReceived(String message, Socket socket) {
            runOnUiThread(() -> {

                String extra = " -> " + socket.getInetAddress().getHostAddress()
                        + ":" + socket.getPort();
                Log.d(TAG, "onMessageReceived: " + message.length()
                        + " " + extra + " " + System.currentTimeMillis());
                mMessages.append(message.length() + " " + extra
                        + " " + System.currentTimeMillis() + "\n");
                incomingData.setText(mMessages.toString());
            });
        }

        @Override
        public void removeMessage(Socket socket) {
            synchronized (outputStreams) {
                Log.d(TAG, "Removing connection to " + socket);
                String deviceName = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                String device = devicesSocket.get(socket);
                devices.remove(device);
                devicesSocket.remove(socket);
                outputStreams.remove(socket);
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Device removed " + deviceName, Toast.LENGTH_LONG).show();
                    deviceList.remove(device);
                    myAdapter.notifyDataSetChanged();
                });
                try {
                    socket.close();
                } catch (IOException ie) {
                    System.out.println("Error closing " + socket);
                    ie.printStackTrace();
                }
            }

        }
    };
}