package com.example.wifisampleapp;

import java.net.Socket;

public interface ServerActivityListener {

    void onMessageReceived(String message, Socket socket);

    void removeMessage(Socket socket);
}
