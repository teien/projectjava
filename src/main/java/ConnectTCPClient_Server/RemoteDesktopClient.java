package ConnectTCPClient_Server;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteDesktopClient extends JFrame {
    private final JTextField nameField;
    private final JPanel chatPanel;
    private final JButton callButton;
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

    private final JButton disconnectButton;
    private final JButton remoteButton;
    private final JTextField chatInput ;
    private final JTextArea chatArea;
    private Socket chatSocket;
    private static DataOutputStream chatDos;
    private DataInputStream chatDis;
    private final JButton sendFileButton;
    private final JButton sendChatButton;
    private Socket audioSocket;


    public static void main(String[] args) {
        new RemoteDesktopClient().setVisible(true);
    }
    public RemoteDesktopClient() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeConnections();
            }
        });

        setTitle("Remote Desktop Client");
        setSize(600, 300);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("JetBrains Mono NL", Font.PLAIN, 11));

        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel ipPanel = new JPanel(new FlowLayout());
        Ip4Address = new JTextField(9);
        JLabel ipLabel = new JLabel("IP4: ");
        nameField = new JTextField(7);
        JLabel nameLabel = new JLabel("Name: ");
        //connectButton = getConnectButton();
        disconnectButton = new JButton("Disconnect");


        ipPanel.add(ipLabel);
        ipPanel.add(Ip4Address);
        ipPanel.add(nameLabel);
        ipPanel.add(nameField );
       // ipPanel.add(connectButton);
        ipPanel.add(disconnectButton);

        remoteButton = new JButton("Remote");
        remoteButton.setEnabled(true);
        remoteButton.addActionListener(e -> {
            try {
                connectToRemoteServer(Ip4Address.getText());
                startRemote();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

        });
        disconnectButton.addActionListener(e -> disconnect());

        JPanel inputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        sendChatButton = new JButton("Send Chat");
        sendChatButton.setEnabled(true);
        sendChatButton.addActionListener(e -> {
            try {
                sendChatMessage();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendChatButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        sendFileButton = getjButton();
        callButton = new JButton("Call");
        callButton.addActionListener(e -> {
            try {
                if (callButton.getText().equals("Call")) {
                    if (checkAudioConnection()) {
                        connectToAudioServer(Ip4Address.getText());}
                    startCall();
                    callButton.setText("End Call");
                } else {
                    disConnections(audioSocket, null, null);
                    callButton.setText("Call");
                }

            } catch (LineUnavailableException | IOException ex) {
                throw new RuntimeException(ex);
            }
        });


        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(ipPanel, BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.add(remoteButton);
        buttonPanel.add(sendFileButton);

        buttonPanel.add(callButton);


        add(chatPanel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);
        setFontButton( disconnectButton, remoteButton, sendFileButton, callButton,  sendChatButton);
        setButtonSize( disconnectButton, remoteButton, sendFileButton, callButton,  sendChatButton);

    }



    private void disConnections(Socket socket, DataInputStream dis, DataOutputStream dos) {
        try (

             DataOutputStream ignored = dos;
             DataInputStream ignored1 = dis;
             Socket ignored2 = socket) {

        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }



    private void setJButton(JButton... JButton) {
        for (JButton jbutton : JButton) {
            jbutton.setEnabled(false);
        }
    }
    private @NotNull JButton getjButton() {
        JButton sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(e ->
        {
            try {
                if (!checkFileConnection()){
                connectToFileServer(Ip4Address.getText());}
                if (checkFileConnection()) {
                sendFile();}
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return sendFileButton;
    }
    private void setButtonSize(JButton... buttons) {

        Dimension buttonSize = new Dimension(130, 23);
        for (JButton button : buttons) {
            button.setPreferredSize(buttonSize);
            button.setMaximumSize(buttonSize);}
    }
    private void setFontButton(JButton... buttons) {
        for (JButton button : buttons){
            button.setFont(new Font("JetBrains Mono Light", Font.BOLD, 11));}
    }
    /*private @NotNull JButton getConnectButton() {
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServerInBackground());
        return connectButton;
    }*/
    private void disconnect() {
        closeConnections();
    }
    private void connectToServerInBackground() {
        disconnectButton.setEnabled(true);
        setJButton(sendChatButton, remoteButton, sendFileButton); // set visible false

        new SwingWorker<Void, Void>() {
            private boolean fileServerConnected = false;
            private boolean remoteServerConnected = false;
            private boolean chatServerConnected = false;
            private boolean audioServerConnected = false;

            @Override
            protected Void doInBackground() throws Exception {
                String ip = Ip4Address.getText();

                // Connect to file server
                if (!checkFileConnection()) {
                    try {
                        connectToFileServer(ip);
                        fileServerConnected = true;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi kết nối File Server", JOptionPane.ERROR_MESSAGE);
                    }
                }

                // Connect to remote server
                if (!checkRemoteConnection()) {
                    try {
                        connectToRemoteServer(ip);
                        remoteServerConnected = true;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi kết nối Remote Server", JOptionPane.ERROR_MESSAGE);
                    }
                }
                // Connect to chat server
                if (!checkChatConnection()) {
                    try {
                        connectToChatServer(ip);
                        chatServerConnected = true;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi kết nối Chat Server", JOptionPane.ERROR_MESSAGE);
                    }
                }
                // Connect to audio server
                if (checkAudioConnection()) {
                    try {
                        connectToAudioServer(ip);
                        audioServerConnected = true;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi kết nối Audio Server", JOptionPane.ERROR_MESSAGE);
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                if (fileServerConnected || remoteServerConnected || chatServerConnected|| audioServerConnected) {

                    disconnectButton.setEnabled(true);
                    if (fileServerConnected) {
                        receiveFile();
                    }
                    if (chatServerConnected) {
                        startReceivingMessages();
                    }
                }
                remoteButton.setEnabled(remoteServerConnected);
                sendFileButton.setEnabled(fileServerConnected);
                sendChatButton.setEnabled(chatServerConnected);
                callButton.setEnabled(audioServerConnected);
            }
        }.execute();
    }



    private void connectToFileServer(String ip) throws IOException {
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
    private void connectToChatServer(String ip) throws IOException {
        try {
            chatSocket = new Socket(ip, 49151);
            chatDos = new DataOutputStream(new BufferedOutputStream(chatSocket.getOutputStream()));
            chatDis = new DataInputStream(new BufferedInputStream(chatSocket.getInputStream()));
            System.out.println("Đã kết nối tới server chat");
            startReceivingMessages();
        } catch (ConnectException e) {
            throw new ConnectException("Không thể kết nối tới server chat tại " + ip + ": " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Lỗi khi kết nối tới server chat: " + e.getMessage());
        }
    }
    private void connectToRemoteServer(String ip) throws IOException {

        try {
            socket = new Socket(ip, 49150);
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            System.out.println("Đã kết nối tới server");
        } catch (ConnectException e) {
            throw new ConnectException("Không thể kết nối tới server Remote tại " + ip + ": " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Lỗi khi kết nối tới server: " + e.getMessage());
        }
    }
    private void connectToAudioServer(String ip) throws  IOException {
        try {
            audioSocket = new Socket(ip, 49149);
            System.out.println("Đã kết nối tới server");
        } catch (ConnectException e) {
            throw new ConnectException("Không thể kết nối tới server Remote tại " + ip + ": " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("Lỗi khi kết nối tới server: " + e.getMessage());
        }
    }
    private void connectToOneServer(String svName) {
        String ip = Ip4Address.getText();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    switch (svName) {
                        case "Remote" -> connectToRemoteServer(ip);
                        case "File" -> connectToFileServer(ip);
                        case "Chat" -> connectToChatServer(ip);
                        case "Audio" -> connectToAudioServer(ip);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi kết nối " + svName + " Server", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }
        }.execute();

    }
    private void remoteData() {
        try {
            if (checkRemoteConnection()) {
                dos.writeUTF("REMOTE_DESKTOP");
                dos.flush();
                serverScreenWidth = dis.readInt();
                serverScreenHeight = dis.readInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean checkRemoteConnection() {
        if (socket == null || socket.isClosed()) {
            return false;
        }
        return socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown() &&
                dis != null && dos != null;
    }
    private boolean checkFileConnection() {
        if (fileSocket == null || fileSocket.isClosed()) {
            return false;
        }
        return fileSocket.isConnected() && !fileSocket.isInputShutdown() && !fileSocket.isOutputShutdown() &&
                fileDis != null && fileDos != null;
    }
    private boolean checkChatConnection() {
        if (chatSocket == null || chatSocket.isClosed()) {
            return false;
        } else return chatSocket.isConnected() && !chatSocket.isInputShutdown() && !chatSocket.isOutputShutdown() &&
                chatDis != null && chatDos != null;
    }
    private boolean checkAudioConnection() {
        if (audioSocket == null || audioSocket.isClosed()) {
            return true;
        }
        return !audioSocket.isConnected() || audioSocket.isInputShutdown() || audioSocket.isOutputShutdown();
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
            if (audioSocket != null) audioSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private void startCall() throws LineUnavailableException, IOException {
       AudioHandler audioHandler = new AudioHandler(audioSocket, listener -> {});
       audioHandler.start();
    }
    private void startRemote() throws IOException, InterruptedException {

        remoteData(); // Thiết lập dữ liệu ban đầu cho kết nối remote
        // Khởi tạo giao diện
        frame = new JFrame("Remote Desktop Viewer");
        label = new JLabel();
        frame.setSize(800, 600);
        frame.add(label);
        frame.setVisible(true);

        // Thêm bộ lắng nghe sự kiện chuột và bàn phím
        addMouseListeners(label, dos);
        addKeyListeners(frame, dos);

        BlockingQueue<BufferedImage> imageQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isConnected = new AtomicBoolean(true);

        SwingWorker<Void, Void> receiverWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    while (!isCancelled() && isConnected.get()) {
                        try {
                            String type = dis.readUTF();
                            if ("IMG".equals(type)) {
                                int length = dis.readInt();
                                byte[] imageBytes = new byte[length];
                                dis.readFully(imageBytes);
                                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                                BufferedImage image = ImageIO.read(bais);
                                imageQueue.put(image);
                            }
                        } catch (SocketException | EOFException e) {
                            System.out.println("Mất kết nối đến máy chủ.");
                            isConnected.set(false); // Cập nhật trạng thái kết nối
                            break;
                        } catch (IOException e) {
                            disConnections(socket,dis,dos);
                            break;
                        }
                    }
                } finally {
                    disConnections(socket,dis,dos);
                    isConnected.set(false);
                }
                return null;
            }

            @Override
            protected void done() {
                disConnections(socket,dis,dos);
                isConnected.set(false);
            }
        };

        SwingWorker<Void, BufferedImage> imageWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled() && isConnected.get()) { // Kiểm tra kết nối
                    BufferedImage image = imageQueue.take();
                    publish(image);
                }
                return null;
            }

            @Override
            protected void process(List<BufferedImage> chunks) {
                if (!chunks.isEmpty()) {
                    currentImage = chunks.get(chunks.size() - 1);
                    updateImage();
                }
            }
        };

        receiverWorker.execute();
        imageWorker.execute();

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                disConnections(socket,dis,dos);
                remoteButton.setText("Remote");
                isConnected.set(false);
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateImage();
            }
        });
       frame.repaint();
    }
    private Point adjustMouseCoordinates(Point clientPoint) {
        Dimension frameSize = frame.getContentPane().getSize();
        int x = (int) (clientPoint.x * (serverScreenWidth / (double) frameSize.width));
        int y = (int) (clientPoint.y * (serverScreenHeight / (double) frameSize.height));
        return new Point(x, y);
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
                    disConnections(socket,dis,dos);
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
    private void updateImage() {
        if (currentImage != null) {
            Dimension frameSize = frame.getContentPane().getSize();
            Image scaledImage = currentImage.getScaledInstance(frameSize.width, frameSize.height, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaledImage));
            label.repaint();
        }
    }

    public void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            JProgressBar progressBar = new JProgressBar(0, (int) file.length());
            progressBar.setStringPainted(true);


            chatArea.append("Đang gửi file: " + file.getName() + "\n");
            chatPanel.add(progressBar, BorderLayout.SOUTH);
            chatPanel.revalidate();
            chatPanel.repaint();

            SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fileDos.writeUTF("FILE_SEND");
                        fileDos.writeUTF(file.getName());
                        fileDos.writeLong(file.length());

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        int totalBytesRead = 0;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            fileDos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            publish(totalBytesRead);
                        }
                        fileDos.flush();
                    }
                    return null;
                }

                @Override
                protected void process(List<Integer> chunks) {
                    if (!chunks.isEmpty()) {
                        int mostRecentValue = chunks.get(chunks.size() - 1);
                        progressBar.setValue(mostRecentValue);
                    }
                }

                @Override
                protected void done() {
                    try {
                        get();
                        chatArea.append("File " + file.getName() + " đã được gửi thành công.\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Lỗi khi gửi file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    } finally {

                        chatPanel.remove(progressBar);
                        chatPanel.revalidate();
                        chatPanel.repaint();
                    }
                }
            };

            worker.execute();
        }
    }
    private void receiveFile() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    while (!isCancelled()) {
                        String messageType = fileDis.readUTF();
                        if ("FILE_RECEIVE".equals(messageType)) {
                            String fileName = fileDis.readUTF();
                            long fileSize = fileDis.readLong();
                            String userHome = System.getProperty("user.home");
                            String downloadsPath = userHome + File.separator + "Downloads";
                            File file = new File(downloadsPath, fileName);
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
                } catch (SocketException | EOFException e) {
                    System.out.println("Server đã đóng kết nối");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private void sendChatMessage() throws IOException {
        if (!checkChatConnection()) {
            connectToChatServer(Ip4Address.getText());
            System.out.println("Đã kết nối tới server chat");
        }
        String message = chatInput.getText();
        String name = nameField.getText();
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
                while (!isCancelled() && checkChatConnection()) {
                    try {
                        String message = chatDis.readUTF();
                        publish(message);
                    } catch (SocketException | EOFException e) {
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

