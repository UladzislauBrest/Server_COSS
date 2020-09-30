package server;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends JFrame implements Runnable {
    private JPanel panel;
    private JButton startTheServerButton;
    private JButton stopTheServerButton;
    private JList<String> list;
    private JTextField portTextField;
    private JLabel portLabel;
    private static DefaultListModel<String> listModel;
    private ServerSocket serverSocket = null;
    static boolean workServer = false;
    private int numberClient = 0;

    Server() {
        setSize(683, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(panel);

        listModel = new DefaultListModel<String>();
        list.setModel(listModel);

        stopTheServerButton.setEnabled(false);

        startTheServerButton.addActionListener(e -> startActionListener());
        stopTheServerButton.addActionListener(e -> stopActionListener());
    }

    static void addRecordToList(String string) {
        listModel.addElement(string);
    }

    private void startActionListener() {
        // Запустить сервер
        try {
            int port = getPort();
            if(port == -1) {
                JOptionPane.showMessageDialog(this, "Порт введён не корректно. Введите от 1 до 6000.");
                return;
            }
            serverSocket = new ServerSocket(port);
            addRecordToList("TCPServer: Запущен");

            stopTheServerButton.setEnabled(true);
            startTheServerButton.setEnabled(false);
            portLabel.setEnabled(false);
            portTextField.setEnabled(false);
            list.setEnabled(true);
            workServer = true;
            Thread thread = new Thread(this);
            thread.start();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(workServer) {
            try {
                Socket clientSocket = serverSocket.accept();
                addRecordToList("Соединение установлено. Подключился: " + clientSocket.getInetAddress().getHostName() + ++numberClient);
                new ThreadClient(clientSocket, clientSocket.getInetAddress().getHostName() + numberClient);
            } catch (IOException ignored) { }
        }
    }

    private void stopActionListener() {
        // Остановить сервер
        try {
            serverSocket.close();
            workServer = false;
            stopTheServerButton.setEnabled(false);
            startTheServerButton.setEnabled(true);
            portLabel.setEnabled(true);
            portTextField.setEnabled(true);
            list.setEnabled(false);
            addRecordToList("TCPServer: Остановлен");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private int getPort() {
        try {
            int port = Integer.parseInt(portTextField.getText());
            if (port >= 0 && port <= 6000)
                return port;
        } catch (Exception ignored) { }
        return -1;
    }
}

