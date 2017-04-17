package com.company;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            System.out.println("Starting client");
            Socket socket = new Socket("localhost", 5555);
            new Thread(new MessageListener(socket)).start();

            Scanner stdIn = new Scanner(System.in);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String message;
            System.out.println(" Write something below:");
            while ((message = stdIn.nextLine()) != null) {
                writer.write(message + "\r\n");
                writer.flush();
            }
            writer.close();
            stdIn.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MessageListener implements Runnable {
        Socket socket;

        public MessageListener(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    try {
                        String message = null;
                        while ((message = reader.readLine()) != null) {
                            System.out.println("Someone said:  " + message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}