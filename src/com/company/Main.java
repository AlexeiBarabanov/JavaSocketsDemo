package com.company;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class Main {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5555);
        System.out.println("Starting server");

        SocketPool pool = new SocketPool();
        new Thread(pool).start();

        while (true) {
            Socket socket = serverSocket.accept();
            pool.addSocket(socket);
        }
    }

    static class SocketHandler implements Runnable {
        BlockingDeque<String> sharedMessagePool;
        Socket user;
        BufferedReader reader;
        BufferedWriter writer;
        Boolean isOnline = true;

        public Boolean isOnline() {
            return isOnline;
        }

        public SocketHandler(Socket socket, BlockingDeque<String> messagePool) {
            try {
                user = socket;
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                sharedMessagePool = messagePool;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            try {
                writer.write(message);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String message = null;
            try {
                while ((message = reader.readLine()) != null) {
                    System.out.println("Someone said: " + message);
                    synchronized (sharedMessagePool) {
                        sharedMessagePool.push(message + "\r\n");
                        sharedMessagePool.notifyAll();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                isOnline = false;
                System.out.println("Someone went offline");
                try {
                    user.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    static class SocketPool implements Runnable {

        List<SocketHandler> users = new ArrayList<>();
        LinkedBlockingDeque<String> messagePool = new LinkedBlockingDeque<>();

        public synchronized void addSocket(Socket user) {
            SocketHandler sh = new SocketHandler(user, messagePool);
            users.add(sh);
            new Thread(sh).start();
        }

        public void broadcast(String message) {
            for (SocketHandler user : users) {
                if (user.isOnline)
                    user.sendMessage(message);
            }
        }

        @Override
        public void run() {
            while (true) {
                synchronized (messagePool) {
                    try {
                        messagePool.wait();
                        while (!messagePool.isEmpty()) {
                            broadcast(messagePool.pop());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}