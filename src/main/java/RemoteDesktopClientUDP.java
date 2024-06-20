import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopClientUDP {
    private DatagramSocket socket;
    private JFrame frame;
    private JLabel label;
    private BufferedImage currentImage;
    private int serverScreenWidth;
    private int serverScreenHeight;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopClientUDP().start());
    }

    public void start() {
        String ip = JOptionPane.showInputDialog("Nhập IP của server");
        if (ip == null || ip.isEmpty()) {
            JOptionPane.showMessageDialog(null, "IP không hợp lệ");
            return;
        }

        try {
            socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(ip);

            frame = new JFrame("Remote Desktop Viewer");
            label = new JLabel();
            frame.setSize(800, 600);
            frame.add(label);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    socket.close();
                }
            });

            frame.setVisible(true);

            addMouseListeners(label, serverAddress);
            addKeyListeners(frame, serverAddress);

            new Thread(() -> {
                byte[] buffer = new byte[65536];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    try {
                        socket.receive(packet);
                        ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                        BufferedImage image = ImageIO.read(bais);
                        if (image != null) {
                            currentImage = image;
                            updateImage();
                        }

                    } catch (SocketException e) {
                        System.out.println("Đã ngắt kết nối");
                        break;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateImage();
                }
            });

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Lỗi kết nối tới server: " + e.getMessage());
            e.printStackTrace();
        }
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

    private void addMouseListeners(JLabel label, InetAddress serverAddress) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendMouseEvent("CLICK", e.getPoint(), serverAddress, e.getButton());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent("MOUSE_PRESS", e.getPoint(), serverAddress, e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent("MOUSE_RELEASE", e.getPoint(), serverAddress, e.getButton());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), serverAddress, -1);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), serverAddress, -1);
            }

            private void sendMouseEvent(String action, Point point, InetAddress serverAddress, int button) {
                try {
                    Point adjustedPoint = adjustMouseCoordinates(point);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeUTF("CTL");
                    dos.writeUTF(action);
                    dos.writeInt(adjustedPoint.x);
                    dos.writeInt(adjustedPoint.y);
                    dos.writeInt(button);
                    byte[] data = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 12345);
                    socket.send(packet);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };

        label.addMouseListener(mouseAdapter);
        label.addMouseMotionListener(mouseAdapter);
    }

    private void addKeyListeners(JFrame frame, InetAddress serverAddress) {
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent("PRESS", e.getKeyCode(), serverAddress);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent("RELEASE", e.getKeyCode(), serverAddress);
            }

            private void sendKeyEvent(String action, int keyCode, InetAddress serverAddress) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeUTF("CTL");
                    dos.writeUTF(action);
                    dos.writeInt(-1);
                    dos.writeInt(-1);
                    dos.writeInt(keyCode);
                    byte[] data = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 12345);
                    socket.send(packet);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };

        frame.addKeyListener(keyAdapter);
    }
}
