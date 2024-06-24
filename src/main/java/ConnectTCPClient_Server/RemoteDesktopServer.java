package ConnectTCPClient_Server;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea chatArea;
    private JTextArea logArea;
    private JTextField chatField;
    private JButton sendButton;
    private final ConcurrentHashMap<Socket, DataOutputStream> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopServer().createAndShowGUI());
    }

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Remote Desktop Server");
        frame.setSize(500, 600);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());

        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));

        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());

        chatArea = new JTextArea(10, 30);
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        chatField = new JTextField(20);
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(chatField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(logPanel, BorderLayout.CENTER);
        frame.add(chatPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startServer() {
        if (!isRunning) {
            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(12345);
                    appendToLogArea("Server đang chờ kết nối...");

                    while (isRunning) {
                        try {
                            Socket socket = serverSocket.accept();
                            appendToLogArea("Client đã kết nối");

                            new Thread(() -> handleClient(socket)).start();

                        } catch (IOException e) {
                            if (isRunning) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void handleClient(Socket socket) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String connectionType = dis.readUTF();
                 //case
                    switch (connectionType) {
                        case "CHAT":
                            new ChatHandler(socket, connectionType).start();
                            break;
                        case "REMOTE_DESKTOP":
                            new RemoteClientHandler(socket, connectionType).start();
                            break;
                    }

                } catch (EOFException e) {
                    System.out.println("Client đã ngắt kết nối");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        worker.execute();
    }


    private void stopServer() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                appendToLogArea("Server đã dừng");
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (Socket socket : clients.keySet()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            clients.clear();
        }
    }

    private void appendToLogArea(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendMessage() {
        String message = chatField.getText().trim();
        if (!message.isEmpty()) {
            appendToChatArea("Server: " + message);
            broadcastMessage("Server: " + message);
            chatField.setText("");
        }
    }

    private void broadcastMessage(String message) {
        for (DataOutputStream dos : clients.values()) {
            try {
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class RemoteClientHandler extends Thread {
        private final Socket socket;
        private final String requestType;
        private Robot robot;
        private Rectangle screenRect;

        public RemoteClientHandler(Socket socket, String requestType) {
            this.socket = socket;
            this.requestType = requestType;
            try {
                robot = new Robot();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                screenRect = new Rectangle(screenSize);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                clients.put(socket, dos);
                if ("REMOTE_DESKTOP".equalsIgnoreCase(requestType)) {
                    dos.writeInt(screenRect.width);
                    dos.writeInt(screenRect.height);
                    dos.flush();

                    // Thread để gửi ảnh màn hình
                    new Thread(() -> {
                        try {
                            while (isRunning && !socket.isClosed()) {
                                BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(screenCapture, "jpg", baos);
                                byte[] imageBytes = baos.toByteArray();

                                dos.writeUTF("IMG");
                                dos.writeInt(imageBytes.length);
                                dos.write(imageBytes);
                                dos.flush();

                                Thread.sleep(10);
                            }
                        } catch (SocketException e) {
                            System.out.println("Client đã ngắt kết nối");
                        } catch (IOException | InterruptedException e) {
                            if (isRunning) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    // Nhận các lệnh điều khiển từ client
                    while (isRunning && !socket.isClosed()) {
                        try {
                            String type = dis.readUTF();
                            if ("CTL".equals(type)) {
                                String action = dis.readUTF();
                                int x = dis.readInt();
                                int y = dis.readInt();
                                int data = dis.readInt();

                                handleControlAction(action, x, y, data);
                            }
                        } catch (EOFException e) {
                            System.out.println("Client đã ngắt kết nối");
                            break;
                        } catch (SocketException e) {
                            System.out.println("Server đã đóng kết nối");
                            break;
                        } catch (IOException | InterruptedException e) {
                            if (isRunning) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clients.remove(socket);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                appendToLogArea("Client đã ngắt kết nối");
            }
        }

        private void handleControlAction(String action, int x, int y, int data) throws InterruptedException {
            switch (action) {
                case "MOUSE_PRESS":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(data));
                    break;
                case "MOUSE_RELEASE":
                    robot.mouseMove(x, y);
                    robot.mouseRelease(InputEvent.getMaskForButton(data));
                    break;
                case "MOVE":
                    robot.mouseMove(x, y);
                    break;
                case "PRESS":
                    robot.keyPress(data);
                    break;
                case "RELEASE":
                    robot.keyRelease(data);
                    break;
            }
        }
    }

    class ChatHandler extends Thread {
        private final Socket socket;
        private final DataInputStream dis;
        private final DataOutputStream dos;

        public ChatHandler(Socket socket, String requestType) {
            this.socket = socket;
            DataInputStream tempDis = null;
            DataOutputStream tempDos = null;
            try {
                tempDis = new DataInputStream(socket.getInputStream());
                tempDos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            dis = tempDis;
            dos = tempDos;
        }

        @Override
        public void run() {
            clients.put(socket, dos);
            try {
                while (isRunning && !socket.isClosed()) {
                    try {
                        String message = dis.readUTF();
                        System.out.println("Client: " + message);
                        appendToChatArea("Client: " + message);
                        // Gửi phản hồi lại cho client
                        dos.writeUTF("Server: " + message);
                        dos.flush();
                    } catch (EOFException e) {
                        // Client ngắt kết nối
                        System.out.println("Client đã ngắt kết nối");
                        break;
                    } catch (SocketException e) {
                        System.out.println("Server đã đóng kết nối");
                        break;
                    } catch (IOException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } finally {
                clients.remove(socket);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                appendToLogArea("Client đã ngắt kết nối");
            }
        }
    }
}
