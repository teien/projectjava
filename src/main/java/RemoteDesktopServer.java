import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;

public class RemoteDesktopServer extends JFrame {
    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private JTextArea logArea;

    public RemoteDesktopServer() {
        setTitle("Remote Desktop Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        setLocationRelativeTo(null);

    }

    private void initComponents() {
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        logArea = new JTextArea();
        logArea.setEditable(false);

        startButton.addActionListener(e -> startServer());

        stopButton.addActionListener(e -> stopServer());

        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(logArea), BorderLayout.CENTER);
    }

    private void startServer() {
        if (serverThread == null || !serverThread.isAlive()) {
            try {
                serverSocket = new ServerSocket(5000);
                serverThread = new ServerThread();
                serverThread.start();
                logArea.append("Server started on port 5000\n");
            } catch (IOException e) {
                logArea.append("Error starting server: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }

    private void stopServer() {
        if (serverThread != null && serverThread.isAlive()) {
            try {
                serverSocket.close();
                serverThread.interrupt();
                logArea.append("Server stopped\n");
            } catch (IOException e) {
                logArea.append("Error stopping server: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }

    private class ServerThread extends Thread {
        @Override
        public void run() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logArea.append("Client connected: " + clientSocket.getInetAddress() + "\n");
                    new ClientHandler(clientSocket).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        logArea.append("Server exception: " + e.getMessage() + "\n");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private Robot robot;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try (BufferedOutputStream imageOutputStream = new BufferedOutputStream(socket.getOutputStream());
                 ObjectInputStream eventInputStream = new ObjectInputStream(socket.getInputStream())) {
                while (true) {
                    // Capture the screen
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    ImageIO.write(screenCapture, "jpeg", imageOutputStream);
                    imageOutputStream.flush();

                    // Receive and process client events
                    Object event = eventInputStream.readObject();
                    if (event instanceof String) {
                        String[] eventDetails = ((String) event).split(":");
                        String eventType = eventDetails[0];
                        int x = Integer.parseInt(eventDetails[1]);
                        int y = Integer.parseInt(eventDetails[2]);
                        if ("MOUSE_MOVE".equals(eventType)) {
                            robot.mouseMove(x, y);
                        } else if ("MOUSE_CLICK".equals(eventType)) {
                            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        } else if ("KEY_PRESS".equals(eventType)) {
                            robot.keyPress(x);
                            robot.keyRelease(x);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logArea.append("ClientHandler exception: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RemoteDesktopServer().setVisible(true);
        });
    }
}
