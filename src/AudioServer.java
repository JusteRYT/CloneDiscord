import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class AudioServer {
    private static final int SERVER_PORT = 12345;
    private static List<Socket> clientSockets = new ArrayList<>();
    private static boolean isRunning = true;

    public static void main(String[] args) {
        new AudioServer().createUI();
        startServer();
    }

    private void createUI() {
        JFrame frame = new JFrame("Voice Chat Server");
        JTextArea clientListArea = new JTextArea(10, 30);
        JButton stopButton = new JButton("Выключить сервер");

        stopButton.addActionListener(e -> stopServer());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new JScrollPane(clientListArea), "Center");
        frame.getContentPane().add(stopButton, "South");
        frame.pack();
        frame.setVisible(true);

        // Установим клиентский список
        new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(1000);
                    StringBuilder sb = new StringBuilder();
                    for (Socket socket : clientSockets) {
                        sb.append(socket.getInetAddress()).append(":").append(socket.getPort()).append("\n");
                    }
                    clientListArea.setText(sb.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Сервер запущен на порту " + SERVER_PORT);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Клиент подключен: " + clientSocket.getInetAddress());

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream()) {
            byte[] buffer = new byte[4096];
            while (isRunning) {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) break;

                // Передаем данные всем клиентам
                for (Socket socket : clientSockets) {
                    if (socket != clientSocket) {
                        OutputStream out = socket.getOutputStream();
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSockets.remove(clientSocket);
                clientSocket.close();
                System.out.println("Клиент отключен: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopServer() {
        isRunning = false;
        for (Socket socket : clientSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Сервер остановлен.");
        System.exit(0);
    }
}
