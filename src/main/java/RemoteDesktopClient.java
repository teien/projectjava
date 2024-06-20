import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopClient {
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private SwingWorker<Void, BufferedImage> worker;

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
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

            System.out.println("Đã kết nối tới server");

            JFrame frame = new JFrame("Remote Desktop Viewer");
            JLabel label = new JLabel();
            frame.add(label);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            addMouseListeners(label, dos);
            addKeyListeners(frame, dos);

            worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
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
                        } catch (IOException e) {
                            e.printStackTrace();
                            break; // Exit the loop on error
                        }
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<BufferedImage> chunks) {
                    BufferedImage image = chunks.get(chunks.size() - 1);
                    label.setIcon(new ImageIcon(image));
                    label.repaint();
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

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Lỗi kết nối tới server: " + e.getMessage());
            e.printStackTrace();
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
                sendMouseEvent("MOVE", e.getPoint(), dos, 0);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), dos, 0);
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
        };
        frame.addKeyListener(keyAdapter);
    }

    private static void sendMouseEvent(String action, Point point, DataOutputStream dos, int button) {
        try {
            dos.writeUTF("CTL");
            dos.writeUTF(action);
            dos.writeInt(point.x);
            dos.writeInt(point.y);
            dos.writeInt(button);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendKeyEvent(String action, int keycode, DataOutputStream dos) {
        try {
            dos.writeUTF("CTL");
            dos.writeUTF(action);
            dos.writeInt(0);  // x
            dos.writeInt(0);  // y
            dos.writeInt(keycode);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
