import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopClient {
    public static void main(String[] args) {
        new RemoteDesktopClient().start();
    }

    public void start() {
        try {
            String ip = JOptionPane.showInputDialog("Enter the server IP address");
            Socket screenSocket = new Socket(ip, 12345);
            Socket controlSocket = new Socket(ip, 12346);
            System.out.println("Connected to the server");

            JFrame frame = new JFrame("Remote Desktop Viewer");
            JLabel label = new JLabel();
            frame.add(label);
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            DataOutputStream dos = new DataOutputStream(controlSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(screenSocket.getInputStream());

            label.addMouseListener(new MouseAdapter() {
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
            });

            label.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    sendMouseEvent("MOVE", e.getPoint(), dos, 0);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    sendMouseEvent("MOVE", e.getPoint(), dos, 0);
                }
            });

            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    sendKeyEvent("PRESS", e.getKeyCode(), dos);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    sendKeyEvent("RELEASE", e.getKeyCode(), dos);
                }
            });

            // Using SwingWorker to keep the UI responsive
            SwingWorker<Void, BufferedImage> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    while (true) {
                        byte[] imageBytes = (byte[]) ois.readObject();
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                        BufferedImage image = ImageIO.read(bais);
                        publish(image);
                    }
                }

                @Override
                protected void process(java.util.List<BufferedImage> chunks) {
                    BufferedImage image = chunks.get(chunks.size() - 1);
                    label.setIcon(new ImageIcon(image));
                    label.repaint();
                }
            };
            worker.execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMouseEvent(String action, Point point, DataOutputStream dos, int button) {
        try {
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
