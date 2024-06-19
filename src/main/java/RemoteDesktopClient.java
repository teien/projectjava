import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;

public class RemoteDesktopClient {
    private JFrame frame;
    private JLabel label;
    private DataOutputStream dos;
    private ObjectInputStream ois;

    public static void main(String[] args) {
        new RemoteDesktopClient().start();
    }

    public void start() {
        try {
            initializeSockets();
            initializeUI();
            startScreenReceiver();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeSockets() throws IOException {
        try {
            String ip = JOptionPane.showInputDialog("Enter the server IP address");
            Socket screenSocket = new Socket(ip, 12345);
            Socket controlSocket = new Socket(ip, 12346);
            System.out.println("Connected to the server");

            dos = new DataOutputStream(controlSocket.getOutputStream());
            ois = new ObjectInputStream(screenSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Failed to connect to the server: " + e.getMessage());
            throw e;
        }
    }

    private void initializeUI() {
        frame = new JFrame("Remote Desktop Viewer");
        label = new JLabel();
        frame.add(label);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);

        label.addMouseListener(createMouseListener());
        label.addMouseMotionListener(createMouseMotionListener());
        frame.addKeyListener(createKeyListener());

        System.out.println("UI initialized and frame set visible");
    }

    private void startScreenReceiver() {
        SwingWorker<Void, BufferedImage> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    try {
                        byte[] imageBytes = (byte[]) ois.readObject();
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                        BufferedImage image = ImageIO.read(bais);
                        publish(image);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        break;
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
        };
        worker.execute();
    }

    private MouseListener createMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendMouseEvent("CLICK", e.getPoint(), e.getButton());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent("MOUSE_PRESS", e.getPoint(), e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent("MOUSE_RELEASE", e.getPoint(), e.getButton());
            }
        };
    }

    private MouseMotionListener createMouseMotionListener() {
        return new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), 0);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent("MOVE", e.getPoint(), 0);
            }
        };
    }

    private KeyListener createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent("PRESS", e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent("RELEASE", e.getKeyCode());
            }
        };
    }

    private void sendMouseEvent(String action, Point point, int button) {
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

    private void sendKeyEvent(String action, int keycode) {
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
