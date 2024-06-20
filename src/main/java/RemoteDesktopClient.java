import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class RemoteDesktopClient extends JFrame {
    private final Socket socket;
    private BufferedImage image;
    private final ObjectOutputStream eventOutputStream;
    private final BufferedInputStream imageInputStream;

    public RemoteDesktopClient(String serverAddress) throws IOException {
        this.socket = new Socket(serverAddress, 5000);
        this.eventOutputStream = new ObjectOutputStream(socket.getOutputStream());
        this.imageInputStream = new BufferedInputStream(socket.getInputStream());
        initComponents();
        new ScreenReceiver().start();
    }

    private void initComponents() {
        setTitle("Remote Desktop Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, this);
                }
            }
        };
        add(panel);
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                sendEvent("MOUSE_MOVE:" + e.getX() + ":" + e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                sendEvent("MOUSE_CLICK:" + e.getX() + ":" + e.getY());
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                sendEvent("KEY_PRESS:" + e.getKeyCode() + ":0");
            }
        });
    }

    private void sendEvent(String event) {
        try {
            eventOutputStream.writeObject(event);
            eventOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Event send exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class ScreenReceiver extends Thread {
        public void run() {
            try {
                while (true) {
                    image = ImageIO.read(imageInputStream);
                    if (image != null) {
                        repaint();
                    }
                }
            } catch (IOException e) {
                System.err.println("ScreenReceiver exception: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String ip = JOptionPane.showInputDialog("Enter server IP address:");
                new RemoteDesktopClient(ip).setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
