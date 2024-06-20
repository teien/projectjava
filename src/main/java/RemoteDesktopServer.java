import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private JButton startButton;
    private JButton stopButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopServer().createAndShowGUI());
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Remote Desktop Server");
        frame.setSize(200, 105);
        frame.setLayout(new FlowLayout());
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        frame.add(startButton);
        frame.add(stopButton);
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
                    System.out.println("Server đang chờ kết nối...");

                    while (isRunning) {
                        try {
                            Socket socket = serverSocket.accept();
                            System.out.println("Client đã kết nối");
                            new ClientHandler(socket).start();
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

    private void stopServer() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                System.out.println("Server đã dừng");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ClientHandler extends Thread {
        private final Socket socket;
        private Robot robot;
        private Rectangle screenRect;

        public ClientHandler(Socket socket) {
            this.socket = socket;
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

                // Thread để gửi ảnh màn hình
                new Thread(() -> {
                    try {
                        while (isRunning && !socket.isClosed()) {
                            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

                            // Giảm kích thước ảnh
                            Image scaledImage = screenCapture.getScaledInstance(screenCapture.getWidth() / 2, screenCapture.getHeight() / 2, Image.SCALE_SMOOTH);
                            BufferedImage scaledBufferedImage = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
                            Graphics g = scaledBufferedImage.createGraphics();
                            g.drawImage(scaledImage, 0, 0, null);
                            g.dispose();

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(scaledBufferedImage, "jpg", baos);
                            byte[] imageBytes = baos.toByteArray();

                            dos.writeUTF("IMG");
                            dos.writeInt(imageBytes.length);
                            dos.write(imageBytes);
                            dos.flush();

                            Thread.sleep(200); // Đợi 200ms trước khi chụp lại màn hình
                        }
                    } catch (IOException | InterruptedException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                // Xử lý lệnh điều khiển từ client
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
            }
        }

        private void handleControlAction(String action, int x, int y, int data) {
            switch (action) {
                case "CLICK":
                    robot.mouseMove(x, y);
                    robot.mousePress(InputEvent.getMaskForButton(data));
                    robot.mouseRelease(InputEvent.getMaskForButton(data));
                    break;
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
}
