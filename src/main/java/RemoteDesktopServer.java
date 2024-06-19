import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.*;

public class RemoteDesktopServer extends JFrame {
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private ServerSocket screenSocket;
    private ServerSocket controlSocket;
    private boolean running;

    public RemoteDesktopServer() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Remote Desktop Server");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        statusLabel = new JLabel("Server is stopped", SwingConstants.CENTER);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        stopButton.setEnabled(false);

        setLayout(new BorderLayout());
        add(statusLabel, BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);
        add(panel, BorderLayout.SOUTH);
    }

    private void startServer() {
        if (running) return;

        try {
            screenSocket = new ServerSocket(12345);
            controlSocket = new ServerSocket(12346);
            running = true;

            new Thread(this::handleScreenSocket).start();
            new Thread(this::handleControlSocket).start();

            statusLabel.setText("Server is running");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        if (!running) return;

        try {
            running = false;
            if (screenSocket != null) screenSocket.close();
            if (controlSocket != null) controlSocket.close();

            statusLabel.setText("Server is stopped");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleScreenSocket() {
        try {
            while (running) {
                Socket clientSocket = screenSocket.accept();
                // Handle screen data from the client here
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    private void handleControlSocket() {
        try {
            while (running) {
                Socket clientSocket = controlSocket.accept();
                // Handle control data from the client here
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RemoteDesktopServer server = new RemoteDesktopServer();
            server.setVisible(true);
        });
    }
}
