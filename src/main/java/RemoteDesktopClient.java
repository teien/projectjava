import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopClient {
    private Socket socket;
    private DataInputStream dis;
    private JFrame frame;
    private JLabel label;
    private BufferedImage currentImage;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RemoteDesktopClient().start());
    }

    public void start() {
        String ip = JOptionPane.showInputDialog("Nhập IP của server");
        if (ip == null || ip.isEmpty()) {
            JOptionPane.showMessageDialog(null, "IP không hợp lệ");
            return;
        }

        try {
            socket = new Socket(ip, 12345);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

            System.out.println("Đã kết nối tới server");

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

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Lỗi kết nối tới server: " + e.getMessage());
            e.printStackTrace();
        }
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
            public void mouseClicked(MouseEvent e) {
                sendMouseEvent("CLICK", e.getPoint(), dos, e.getButton());
            }

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
                    dos.writeUTF("CTL");
                    dos.writeUTF(action);
                    dos.writeInt(point.x);
                    dos.writeInt(point.y);
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
