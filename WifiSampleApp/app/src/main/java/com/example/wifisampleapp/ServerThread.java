package com.example.wifisampleapp;

import android.util.Log;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ServerThread extends Thread {
    private ServerActivityListener mListener;
    private Socket socket;

    public ServerThread(ServerActivityListener listener, Socket socket) {
        this.mListener = listener;
        this.socket = socket;
        this.start();
    }

    @Override
    public void run() {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            while (true) {
                String message = inputStream.readUTF();

                mListener.onMessageReceived(message, socket);
            }

        } catch (EOFException e) {
            Log.d(ServerActivity.TAG, e.getMessage());
        } catch (IOException e) {
            Log.d(ServerActivity.TAG, e.getMessage());
        } finally {
            mListener.removeMessage(socket);
        }
    }
}
