
package ConnectTCPClient_Server;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;


public class RemoteDesktopServer {
    private ServerSocket serverRemoteSocket;
    private ServerSocket serverChatSocket;
    private boolean isRunning = false;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea chatArea;
    private JTextArea logArea;
    private JTextField chatField;
    private ServerSocket serverFileSocket;
    private JCheckBox chatCheckBox;
    private JCheckBox remoteCheckBox;
    private JCheckBox fileCheckBox;
    private static boolean micOpen = false;
    private static final Map<Socket, DataOutputStream> clients = new ConcurrentHashMap<>();
    private static final Map<Socket, DataOutputStream> fileClients = new ConcurrentHashMap<>();
    private static final Map<Socket, DataOutputStream> chatClients = new ConcurrentHashMap<>();
    private static final Map<Socket, DataOutputStream> audioClients = new ConcurrentHashMap<>();

    private JButton sendButton;
    private static JButton callButton;
    private JCheckBox audioCheckBox;
    private ServerSocket serverAudioSocket;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopServer().createAndShowGUI());
    }

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Remote Desktop Server");
        frame.setSize(400, 600);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });

        JPanel controlPanel = new JPanel();
        JPanel checkBoxPanel = new JPanel();
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout());
        controlPanel.setLayout(new FlowLayout());
        checkBoxPanel.setLayout(new FlowLayout());

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        remoteCheckBox = new JCheckBox("Remote Desktop");
        chatCheckBox = new JCheckBox("Chat");
        fileCheckBox = new JCheckBox("File Transfer");
        audioCheckBox = new JCheckBox("Audio");

        stopButton.setEnabled(false);
        startButton.setEnabled(true);
        startButton.addActionListener(e -> {
            startServer();
        });

        stopButton.addActionListener(e -> stopServer());

        remoteCheckBox.setSelected(true);
        chatCheckBox.setSelected(true);
        fileCheckBox.setSelected(true);
        audioCheckBox.setSelected(true);

        remoteCheckBox.addActionListener(e -> {
            startButton.setEnabled(remoteCheckBox.isSelected() || chatCheckBox.isSelected() || fileCheckBox.isSelected());
        });
        chatCheckBox.addActionListener(e -> {
            startButton.setEnabled(remoteCheckBox.isSelected() || chatCheckBox.isSelected() || fileCheckBox.isSelected());
        });
        fileCheckBox.addActionListener(e -> {
            startButton.setEnabled(remoteCheckBox.isSelected() || chatCheckBox.isSelected() || fileCheckBox.isSelected());
        });
        audioCheckBox.addActionListener(e -> {
            startButton.setEnabled(remoteCheckBox.isSelected() || chatCheckBox.isSelected() || fileCheckBox.isSelected());
        });


        checkBoxPanel.add(remoteCheckBox);
        checkBoxPanel.add(chatCheckBox);
        checkBoxPanel.add(fileCheckBox);
        checkBoxPanel.add(audioCheckBox);

        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());

        logArea = new JTextArea(6, 10);
        logArea.setEditable(false);
        logArea.setFont(new Font("JetBrains Mono Light", Font.ITALIC, 9));

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logPanel.add(logScrollPane, BorderLayout.NORTH);
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));

        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());

        chatArea = new JTextArea(20, 20);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("JetBrains Mono NL", Font.PLAIN, 11));

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));


        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        chatField = new JTextField(20);
        sendButton = new JButton("Send");
        callButton = new JButton("Call");
        JButton sendFile = new JButton("File");


        sendFile.addActionListener(e -> {
            new Thread(() -> {
                try {
                    sendFileToAllClients();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });


        callButton.addActionListener(e -> {
                    if (!micOpen) {
                        AudioHandler.startSending();
                        micOpen = true;
                        callButton.setText("End Call");
                    } else {
                        AudioHandler.stopSending();
                        micOpen = false;
                        callButton.setText("Call");
                    }
                });

        inputPanel.add(chatField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(sendFile, BorderLayout.WEST);
        inputPanel.add(callButton, BorderLayout.SOUTH);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        northPanel.add(controlPanel, BorderLayout.NORTH);
        northPanel.add(checkBoxPanel, BorderLayout.CENTER);

        frame.add(northPanel, BorderLayout.NORTH);
        frame.add(logPanel, BorderLayout.CENTER);
        frame.add(chatPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startServer() {
        if (!isRunning) {
            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            chatCheckBox.setEnabled(false);
            remoteCheckBox.setEnabled(false);
            fileCheckBox.setEnabled(false);
            audioCheckBox.setEnabled(false);
            appendToLogArea("Server đang khởi động...");

            if (chatCheckBox.isSelected()) {
                new Thread(this::startChatServer).start();
            }
            if (remoteCheckBox.isSelected()) {
                new Thread(this::startRemoteServer).start();
            }
            if (fileCheckBox.isSelected()) {
                new Thread(this::startFileServer).start();
            }
            if (audioCheckBox.isSelected()) {
                new Thread(this::startAudioServer).start();
            }
        }
    }
    private void startChatServer() {
        try {
            serverChatSocket = new ServerSocket(49151);
            appendToLogArea("Server Chat đang chờ kết nối...");
            while (isRunning) {
                try {
                    Socket socket = serverChatSocket.accept();
                    String clientAddress = socket.getInetAddress().getHostAddress();
                   //lam sao de check khi client mat ket noi

                    appendToLogArea("Client " + clientAddress + " đã kết nối ChatServer");
                    new Thread(() -> handleChatClient(socket)).start();
                } catch (SocketException e) {
                    System.out.println("ChatServer đã đóng kết nối");
                    appendToLogArea("ChatServer đã đóng kết nối");
                } catch (IOException e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            appendToLogArea("Lỗi khi khởi tạo ChatServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void startRemoteServer() {
        try {
            serverRemoteSocket = new ServerSocket(49150);
            appendToLogArea("Server Remote Desktop đang chờ kết nối...");
            while (isRunning) {
                try {
                    Socket socket = serverRemoteSocket.accept();
                    String clientAddress = socket.getInetAddress().getHostAddress();
                    appendToLogArea("Client " + clientAddress + " đã kết nối RemoteServer");
                    new Thread(() -> handleRemoteClient(socket)).start();
                } catch (SocketException e) {
                    System.out.println("RemoteServer đã đóng kết nối");
                    appendToLogArea("RemoteServer đã đóng kết nối");
                } catch (IOException e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            appendToLogArea("Lỗi khi khởi tạo RemoteServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void startFileServer() {
        try {
            serverFileSocket = new ServerSocket(49152);
            appendToLogArea("Server File Transfer đang chờ kết nối...");
            while (isRunning) {
                try {
                    Socket socket = serverFileSocket.accept();
                    String clientAddress = socket.getInetAddress().getHostAddress();
                    appendToLogArea("Client " + clientAddress + " đã kết nối FileServer");
                    new Thread(() -> handleFileClient(socket)).start();
                } catch (SocketException e) {
                    System.out.println("FileServer đã đóng kết nối");
                    appendToLogArea("FileServer đã đóng kết nối");
                } catch (IOException e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            appendToLogArea("Lỗi khi khởi tạo FileServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void startAudioServer() {
        try {
            serverAudioSocket = new ServerSocket(49149);
            appendToLogArea("Server Audio đang chờ kết nối...");
            while (isRunning) {
                try {
                    Socket socket = serverAudioSocket.accept();
                    String clientAddress = socket.getInetAddress().getHostAddress();
                    appendToLogArea("Client " + clientAddress + " đã kết nối AudioServer");
                    audioClients.put(socket, new DataOutputStream(socket.getOutputStream()));
                    new Thread(() -> handleAudioClient(socket)).start();
                } catch (SocketException e) {
                    System.out.println("AudioServer đã đóng kết nối");
                    appendToLogArea("AudioServer đã đóng kết nối");
                } catch (IOException e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            appendToLogArea("Lỗi khi khởi tạo AudioServer: " + e.getMessage());
            e.printStackTrace();
        }
    }




    private void stopServer() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            chatCheckBox.setEnabled(true);
            remoteCheckBox.setEnabled(true);
            fileCheckBox.setEnabled(true);
            audioCheckBox.setEnabled(true);

            try {
                if (serverRemoteSocket != null && !serverRemoteSocket.isClosed()) {
                    serverRemoteSocket.close();
                }
                if (serverChatSocket != null && !serverChatSocket.isClosed()) {
                    serverChatSocket.close();
                }
                if (serverFileSocket != null && !serverFileSocket.isClosed()) {
                    serverFileSocket.close();
                }
                if (serverAudioSocket != null && !serverAudioSocket.isClosed()) {
                    serverAudioSocket.close();
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
            for (Socket socket : chatClients.keySet()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            chatClients.clear();
            for (Socket socket : fileClients.keySet()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fileClients.clear();
            for (Socket socket : audioClients.keySet()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            audioClients.clear();
        }
    }

    private void handleFileClient(Socket socket) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                new FileHandler(socket).start();
                return null;
            }
        };
        worker.execute();
    }
    private void handleRemoteClient(Socket socket) {
        String clientAddress = socket.getInetAddress().getHostAddress();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws LineUnavailableException {
                try {
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String connectionType = dis.readUTF();
                    if (connectionType.equals("REMOTE_DESKTOP")) {
                        new RemoteClientHandler(socket, connectionType).run();
                    }

                } catch (EOFException e) {
                    System.out.println("Client " + clientAddress + " đã ngắt kết nối");

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }
    private void handleChatClient(Socket socket) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws LineUnavailableException, IOException {
                new ChatHandler(socket).start();

                return null;
            }
        };
        worker.execute();
    }
    private void handleAudioClient(Socket socket) {
       /* AudioHandler.ConnectionListener listener = clientAddress -> {
            appendToLogArea("Client " + clientAddress + " đã ngắt kết nối AudioServer");

        };
*/
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws LineUnavailableException, IOException {
                new AudioHandler(socket).start();
                return null;
            }
        };
        worker.execute();
    }

    private void appendToLogArea(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            System.out.println(message);

        });
    }
    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendFileToAllClients() throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            for (DataOutputStream dos : fileClients.values()) {
                sendFile(dos, selectedFile);
            }
            appendToLogArea("Đã gửi file: " + selectedFile.getName() + " tới client.");
        }
    }
    private void sendFile(DataOutputStream dos, File file) {
        try {
            dos.writeUTF("FILE_RECEIVE");
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class FileHandler extends Thread {
        private final Socket socket;
        private static final boolean isRunning = true;

        public FileHandler(Socket socket) {
            this.socket = socket;

        }

        @Override
        public void run() {
            String clientAddress = socket.getInetAddress().getHostAddress();
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                fileClients.put(socket, dos);

                while (isRunning && !socket.isClosed()) {
                    try {
                        String type = dis.readUTF();
                        if ("FILE_SEND".equals(type)) {
                            receiveFile(dis);
                        }
                    } catch (EOFException e) {
                        System.out.println("Client " + clientAddress + " đã ngắt kết nối");
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
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileClients.remove(socket);
                appendToLogArea("Client " + clientAddress + " đã ngắt kết nối");
            }
        }

        private void receiveFile(DataInputStream dis) {
            try {
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                String userHome = System.getProperty("user.home");
                String downloadsPath = userHome + File.separator + "Downloads";
                File file = new File(downloadsPath, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    long remaining = fileSize;
                    while (remaining > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
                appendToLogArea("Đã nhận file: " + fileName + " - " + downloadsPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class RemoteClientHandler implements Runnable {
            private final Socket socket;
            private final String requestType;
            private Robot robot;
            private Rectangle screenRect;
            private boolean isRunning = true; // Biến điều khiển luồng
            private static final ExecutorService executorService = Executors.newCachedThreadPool();
            public RemoteClientHandler(Socket socket, String requestType) {
                this.socket = socket;
                this.requestType = requestType;

                try {
                    robot = new Robot();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    screenRect = new Rectangle(screenSize);
                } catch (AWTException e) {
                    System.err.println("Không thể khởi tạo Robot: " + e.getMessage());
                    appendToLogArea("Không thể khởi tạo Robot: " + e.getMessage());
                }
            }

            @Override
            public void run() {
                String clientAddress = socket.getInetAddress().getHostAddress();
                try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                    clients.put(socket, dos);

                    if ("REMOTE_DESKTOP".equalsIgnoreCase(requestType)) {
                        dos.writeInt(screenRect.width);
                        dos.writeInt(screenRect.height);
                        dos.flush();

                        // Bắt đầu luồng gửi ảnh màn hình
                        executorService.submit(this::sendScreenImages);

                        // Vòng lặp xử lý lệnh client
                        while (isRunning && !socket.isClosed()) {
                            try {
                                handleClientCommands(dis);
                            } catch (IOException e) {
                                System.out.println("Client " + clientAddress + " đã ngắt kết nối");


                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Lỗi khi gửi/nhận dữ liệu qua socket: " + e.getMessage());

                } finally {
                    cleanUp(clientAddress);
                    appendToLogArea("Client " + clientAddress + " đã ngắt kết nối RemoteServer");
                }
            }

            private void sendScreenImages() {
                try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                    while (isRunning && !socket.isClosed()) {
                        BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(screenCapture, "jpg", baos);
                        byte[] imageBytes = baos.toByteArray();

                        dos.writeUTF("IMG");
                        dos.writeInt(imageBytes.length);
                        dos.write(imageBytes);
                        dos.flush();
                        Thread.sleep(10); // Điều chỉnh thời gian trễ (delay) tại đây nếu cần thiết
                    }
                } catch (IOException | InterruptedException e) {
                    System.out.println("Client " + socket.getInetAddress().getHostAddress() + " đã ngắt kết nối");
                }
            }

        private void handleClientCommands(DataInputStream dis) throws IOException {
            try {
                String type = dis.readUTF();
                if ("CTL".equals(type)) {
                    String action = dis.readUTF();
                    int x = dis.readInt();
                    int y = dis.readInt();
                    int data = dis.readInt();

                    handleControlAction(action, x, y, data);
                }
            } catch (EOFException | SocketException e) {
                cleanUp(socket.getInetAddress().getHostAddress());
                System.out.println("Client đã ngắt kết nối");

            } catch (InterruptedException e) {
                System.out.println("Lỗi khi xử lý lệnh từ client: " + e.getMessage());
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

        private void cleanUp(String clientAddress) {
            clients.remove(socket);
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Lỗi khi gửi/nhận dữ liệu qua socket: " + e.getMessage());
            }
            System.out.println("Client " + clientAddress + " đã ngắt kết nối");

        }
        public void stop() {
                isRunning = false;
            }
        }
    public class ChatHandler extends Thread {
        private final Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;

        public ChatHandler(Socket socket) {
            this.socket = socket;
            sendButton.addActionListener(e -> sendMessage());
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            chatClients.put(socket, dos);
            String clientAddress = socket.getInetAddress().getHostAddress();
            try {
                while (isRunning && !socket.isClosed()) {
                    try {
                        String message = dis.readUTF();
                        appendToChatArea(message);
                        broadcastMessage(message);
                    } catch (EOFException e) {
                        System.out.println("Client " + clientAddress + " đã ngắt kết nối");
                        appendToLogArea("Client " + clientAddress + " đã ngắt kết nối ChatServer");
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
                chatClients.remove(socket);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String message) {
            Iterator<Map.Entry<Socket, DataOutputStream>> iterator = chatClients.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Socket, DataOutputStream> entry = iterator.next();
                DataOutputStream dos = entry.getValue();
                try {
                    dos.writeUTF(message);
                    dos.flush();
                } catch (SocketException e) {
                    System.out.println("Client " + entry.getKey() + " đã ngắt kết nối");
                    iterator.remove();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendMessage() {
            String message = chatField.getText().trim();
            if (!message.isEmpty()) {
                appendToChatArea("Server: " + message);
                broadcastMessage("Server: " + message);
                chatField.setText("");
            }
        }
    }

}
