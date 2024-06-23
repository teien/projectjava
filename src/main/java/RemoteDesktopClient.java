import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import javax.imageio.ImageIO;
import javax.swing.*;


public class RemoteDesktopClient extends JFrame {
    private Socket socket;
    private DataInputStream dis;
    private JFrame frame;
    private JLabel label;
    private BufferedImage currentImage;
    private int serverScreenWidth;
    private int serverScreenHeight;
    private final JTextField Ip4Address ;
    private DataOutputStream dos;


    //TCP Connection
    public static void main(String[] args) {
        new RemoteDesktopClient().setVisible(true);

    }
    public RemoteDesktopClient() {
        // Tao JFrame quan ly client

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setSize(400, 200);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel ipPanel = new JPanel(new FlowLayout());
        Ip4Address = new JTextField(15);
        JLabel ipLabel = new JLabel("IP Address: ");
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            try {
                ConnectServer();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        ipPanel.add(ipLabel);
        ipPanel.add(Ip4Address);
        ipPanel.add(connectButton);


        JButton remoteButton = new JButton("Remote");
        remoteButton.addActionListener(e -> {
            try {
                Remote();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });


        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField chatInput = new JTextField();
        JButton sendChatButton = new JButton("Send Chat");
        sendChatButton.addActionListener(e -> sendChatMessage());
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendChatButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(e -> sendFile());
        JButton videoCallButton = new JButton("Video Call");
        videoCallButton.addActionListener(e -> startVideoCall());
        JButton callButton = new JButton("Call");
        callButton.addActionListener(e -> startCall());
        JButton audioButton = new JButton("Send Audio");
        audioButton.addActionListener(e -> sendAudio());
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(ipPanel, BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.add(remoteButton);
        buttonPanel.add(sendFileButton);
        buttonPanel.add(videoCallButton);
        buttonPanel.add(callButton);
        buttonPanel.add(audioButton);


        add(panel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

    }

    private void ConnectServer() throws IOException {
        String ip = Ip4Address.getText();
        socket = new Socket(ip, 12345);
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());
        serverScreenWidth = dis.readInt();
        serverScreenHeight = dis.readInt();
        System.out.println("Đã kết nối tới server");
    }

    private void Remote() throws IOException {

        frame = new JFrame("Remote Desktop Viewer");
        label = new JLabel();
        frame.setSize(800, 600);
        frame.add(label);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

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
                            break; // Thoát vòng lặp khi kết nối bị đóng
                        } catch (IOException e) {
                            e.printStackTrace();
                            break; // Thoát vòng lặp khi gặp lỗi khác
                        }
                    }
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<BufferedImage> chunks) {
                currentImage = chunks.get(chunks.size() - 1);
                updateImage();
            }

            @Override
            protected void done() {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateImage();
            }
        });

    }

    private void sendAudio() {
    }

    private void startCall() {
    }

    private void startVideoCall() {
    }

    private void sendFile() {
    }

    private void sendChatMessage() {
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
           /* @Override
            public void mouseClicked(MouseEvent e) {
                sendMouseEvent("CLICK", e.getPoint(), dos, e.getButton());
                System.out.println("Mouse clicked");
            }*/

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
}
