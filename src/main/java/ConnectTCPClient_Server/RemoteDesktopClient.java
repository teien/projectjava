package ConnectTCPClient_Server;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class RemoteDesktopClient extends JFrame {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket fileSocket;
    private DataOutputStream fileDos;
    private DataInputStream fileDis;
    private JFrame frame;
    private JLabel label;
    private BufferedImage currentImage;
    private int serverScreenWidth;
    private int serverScreenHeight;
    private final JTextField Ip4Address;
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final JButton remoteButton;
    private final  String name = System.getenv("COMPUTERNAME");
    private final JTextField chatInput ;
    private final JTextArea chatArea;
    private Socket chatSocket;
    private static DataOutputStream chatDos;
    private DataInputStream chatDis;

    public static void main(String[] args) {
        new RemoteDesktopClient().setVisible(true);
    }

    public RemoteDesktopClient() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeConnections();
                connectButton.setText("Connect");
                connectButton.setEnabled(true);
            }
        });


        setSize(600, 300);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel ipPanel = new JPanel(new FlowLayout());
        Ip4Address = new JTextField(9);
        JLabel ipLabel = new JLabel("IP Address: ");
        JTextField port = new JTextField(4);
        JLabel portLabel = new JLabel("Port: ");
        connectButton = getConnectButton();
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);

        ipPanel.add(ipLabel);
        ipPanel.add(Ip4Address);
        ipPanel.add(portLabel);
        ipPanel.add(port);
        ipPanel.add(connectButton);
        ipPanel.add(disconnectButton);

        remoteButton = new JButton("Remote");
        remoteButton.addActionListener(e -> {
            try {
                startRemote();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        disconnectButton.addActionListener(e -> disconnect());

        JPanel inputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendChatButton = new JButton("Send Chat");
        sendChatButton.addActionListener(e -> {
            try {
                sendChatMessage();
            } catch (SocketException ex) {
                System.out.println("Server Chat đã đóng kết nối");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendChatButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton sendFileButton = getjButton();
        JButton videoCallButton = new JButton("Video Call");
        videoCallButton.addActionListener(e -> startVideoCall());
        JButton callButton = new JButton("Call");
        callButton.addActionListener(e -> startCall());


        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(ipPanel, BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.add(remoteButton);
        buttonPanel.add(sendFileButton);
        buttonPanel.add(videoCallButton);
        buttonPanel.add(callButton);


        add(panel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);
        setFontButton(connectButton, disconnectButton, remoteButton, sendFileButton, videoCallButton, callButton,  sendChatButton);
        setButtonSize(connectButton, disconnectButton, remoteButton, sendFileButton, videoCallButton, callButton,  sendChatButton);

    }

    private @NotNull JButton getjButton() {
        JButton sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(e -> {
            try {
                if (!checkFileConnection()) {
                    connectToFileServer();
                }
                sendFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi kết nối tới server truyền file: " + ex.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            }
        });
        return sendFileButton;
    }

    private void setButtonSize(JButton... buttons) {

        Dimension buttonSize = new Dimension(130, 20);
        for (JButton button : buttons) {
        button.setPreferredSize(buttonSize);
        button.setMaximumSize(buttonSize);}
    }
    private void setFontButton(JButton... buttons) {
        for (JButton button : buttons){
        button.setFont(new Font("JetBrains Mono Light", Font.BOLD, 11));}
    }

    private @NotNull JButton getConnectButton() {
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServerInBackground());
        return connectButton;
    }
    private void disconnect() {
        closeConnections();
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
    }

    private void connectToServerInBackground() {
        connectButton.setText("Connecting...");
        connectButton.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    connectToServer();
                    connectToChatServer();
                    connectToFileServer();
                } catch (ConnectException e1) {
                    JOptionPane.showMessageDialog(null, "Không thể kết nối tới server: " + e1.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Không thể kết nối tới server: " + e1.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                }
                return null;
            }

            @Override
            protected void done() {
                if (checkConnection()) {
                    connectButton.setText("Connected");
                    disconnectButton.setEnabled(true);
                    startReceivingMessages();
                } else {
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);

                }
            }
        }.execute();
    }
    private void connectToFileServer() throws IOException {
        String ip = Ip4Address.getText();
        try {
            fileSocket = new Socket(ip, 49152);
            fileDos = new DataOutputStream(new BufferedOutputStream(fileSocket.getOutputStream()));
            fileDis = new DataInputStream(new BufferedInputStream(fileSocket.getInputStream()));
            System.out.println("Đã kết nối tới server truyền file");
            receiveFile();
        } catch (ConnectException e) {
            throw new ConnectException("Không thể kết nối tới server truyền file tại " + ip + ": " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Lỗi khi kết nối tới server truyền file: " + e.getMessage());
        }
    }

    private void connectToServer() throws IOException {
        String ip = Ip4Address.getText();
        try {
            socket = new Socket(ip, 49150);
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            System.out.println("Đã kết nối tới server");
        } catch (ConnectException e) {
            throw new ConnectException("Không thể kết nối tới server tại " + ip + ": " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Lỗi khi kết nối tới server: " + e.getMessage());
        }
    }

    private void waitForConnection() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!checkConnection()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (checkConnection()) {
                    try {
                        startRemoteAfterConnection();
                        sendChatMessage();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Không thể kết nối tới server.", "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void remoteData() {
        try {
            if (!checkConnection()) {
                connectToServerInBackground();
            }

            if (checkConnection()) {
                dos.writeUTF("REMOTE_DESKTOP");
                dos.flush();
                serverScreenWidth = dis.readInt();
                serverScreenHeight = dis.readInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkConnection() {
        return socket != null && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown() && dis != null && dos != null ;
    }
    private boolean checkFileConnection() {
        return fileSocket != null && !fileSocket.isClosed() && fileSocket.isConnected() && !fileSocket.isInputShutdown() && !fileSocket.isOutputShutdown() && fileDis != null && fileDos != null;
    }


    private void closeConnections() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
            if (chatDis != null) chatDis.close();
            if (chatDos != null) chatDos.close();
            if (chatSocket != null) chatSocket.close();
            if (fileDis != null) fileDis.close();
            if (fileDos != null) fileDos.close();
            if (fileSocket != null) fileSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void startRemote() throws IOException {
        if (!checkConnection()) {
            connectToServerInBackground();
            waitForConnection();
        } else {
            startRemoteAfterConnection();
        }
    }


    private void startRemoteAfterConnection() throws IOException {
        remoteData();
        remoteButton.setEnabled(false);
        remoteButton.setText("Remoted");
        frame = new JFrame("Remote Desktop Viewer");
        label = new JLabel();
        frame.setSize(800, 600);
        frame.add(label);
        frame.setVisible(true);

        addMouseListeners(label, dos);
        addKeyListeners(frame, dos);

        SwingWorker<Void, BufferedImage> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    while (!isCancelled()) {
                        try {
                            String type = dis.readUTF();
                            if ("IMG".equals(type)) {
                                int length = dis.readInt();
                                byte[] imageBytes = new byte[length];
                                dis.readFully(imageBytes);
                                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                                BufferedImage image = ImageIO.read(bais);
                                publish(image);
                            }
                        } catch (EOFException | SocketException e) {
                            System.out.println("Server đã đóng kết nối");
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                } finally {
                    closeConnections();
                }
                return null;
            }

            @Override
            protected void process(List<BufferedImage> chunks) {
                currentImage = chunks.getLast();
                updateImage();
            }

            @Override
            protected void done() {
                closeConnections();
            }
        };

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                worker.cancel(true);
                remoteButton.setEnabled(true);
                connectButton.setText("Connect");
                connectButton.setEnabled(true);
            }
        });
        worker.execute();
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateImage();
            }
        });
    }



    private void startCall() {
        // Chức năng gọi điện chưa được triển khai
    }

    private void startVideoCall() {
        // Chức năng gọi video chưa được triển khai
    }
    //FILE_TRANSFER
    private void sendFile() {

        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file)) {
                fileDos.writeUTF("FILE_SEND");
                fileDos.writeUTF(file.getName());
                fileDos.writeLong(file.length());

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fileDos.write(buffer, 0, bytesRead);
                }
                fileDos.flush();
                JOptionPane.showMessageDialog(this, "File đã được gửi thành công.");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void receiveFile() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    while (!isCancelled()) {
                        String messageType = fileDis.readUTF();
                        if ("FILE_SEND".equals(messageType)) {
                            String fileName = fileDis.readUTF();
                            long fileSize = fileDis.readLong();
                            File file = new File("received_" + fileName);
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                long totalBytesRead = 0;
                                while (totalBytesRead < fileSize && (bytesRead = fileDis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                    totalBytesRead += bytesRead;
                                }
                            }
                            JOptionPane.showMessageDialog(null, "File " + fileName + " đã được nhận thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Server đã đóng kết nối");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }



    private Point adjustMouseCoordinates(Point clientPoint) {
        Dimension frameSize = frame.getContentPane().getSize();
        int x = (int) (clientPoint.x * (serverScreenWidth / (double) frameSize.width));
        int y = (int) (clientPoint.y * (serverScreenHeight / (double) frameSize.height));
        return new Point(x, y);
    }

    private void updateImage() {
        if (currentImage != null) {
            Dimension frameSize = frame.getContentPane().getSize();
            Image scaledImage = currentImage.getScaledInstance(frameSize.width, frameSize.height, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaledImage));
            label.repaint();
        }
    }

    private void addMouseListeners(JLabel label, DataOutputStream dos) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent("MOUSE_PRESS", e.getPoint(), dos, e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent("MOUSE_RELEASE", e.getPoint(), dos, e.getButton());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), dos, -1);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), dos, -1);
            }

            private void sendMouseEvent(String action, Point point, DataOutputStream dos, int button) {
                try {
                    Point adjustedPoint = adjustMouseCoordinates(point);
                    dos.writeUTF("CTL");
                    dos.writeUTF(action);
                    dos.writeInt(adjustedPoint.x);
                    dos.writeInt(adjustedPoint.y);
                    dos.writeInt(button);
                    dos.flush();
                } catch (SocketException ex) {
                    System.out.println("Server đã đóng kết nối");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };

        label.addMouseListener(mouseAdapter);
        label.addMouseMotionListener(mouseAdapter);
    }

    private void addKeyListeners(JFrame frame, DataOutputStream dos) {
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent("PRESS", e.getKeyCode(), dos);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent("RELEASE", e.getKeyCode(), dos);
            }

            private void sendKeyEvent(String action, int keyCode, DataOutputStream dos) {
                try {
                    dos.writeUTF("CTL");
                    dos.writeUTF(action);
                    dos.writeInt(-1);
                    dos.writeInt(-1);
                    dos.writeInt(keyCode);
                    dos.flush();
                } catch (SocketException ex) {
                    System.out.println("Server đã đóng kết nối");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };

        frame.addKeyListener(keyAdapter);
    }
    //
    private void connectToChatServer() throws IOException {
        String ip = Ip4Address.getText();
        try {
            chatSocket = new Socket(ip, 49151);
            chatDos = new DataOutputStream(new BufferedOutputStream(chatSocket.getOutputStream()));
            chatDis = new DataInputStream(new BufferedInputStream(chatSocket.getInputStream()));
            System.out.println("Đã kết nối tới server chat");
        } catch (ConnectException e) {
            throw new ConnectException("Không thể kết nối tới server chat tại " + ip + ": " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Lỗi khi kết nối tới server chat: " + e.getMessage());
        }
    }

    private void sendChatMessage() throws IOException {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && chatDos != null) {
            chatDos.writeUTF(name + ": " + message);
            chatDos.flush();
            chatInput.setText("");
        }
    }
    private void startReceivingMessages() {
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled() && checkConnection()) {
                    try {
                        String message = chatDis.readUTF();
                        publish(message);
                    } catch (SocketException e) {
                        System.out.println("Server đã đóng kết nối");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    appendToChatArea(message);
                }
            }

            @Override
            protected void done() {
                closeConnections();
            }
        }.execute();
    }

    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}
