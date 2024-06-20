import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;


public class SystemTrayApp {

    public static class SettingsDialog extends JDialog {

        public SettingsDialog(JFrame parent) {
            super(parent, "Settings", true);
            setSize(800, 450);
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            SettingsPanel settingsPanel = new SettingsPanel();
            add(settingsPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JButton applyButton = new JButton("Apply");
            JButton cancelButton = new JButton("Cancel");
            JButton okButton = new JButton("OK");

            applyButton.addActionListener(e -> SettingsPanel.applySettings());
            cancelButton.addActionListener(e -> dispose());
            okButton.addActionListener(e -> {
                SettingsPanel.applySettings();
                dispose();
            });

            buttonPanel.add(okButton);
            buttonPanel.add(applyButton);
            buttonPanel.add(cancelButton);
            add(buttonPanel, BorderLayout.SOUTH);
            SettingsPanel.loadSettings();
        }
    }

    private JPopupMenu popupMenu;
    private JFrame frame;

    public SystemTrayApp() {
        if (!SystemTray.isSupported()) {
            System.out.println("System Tray is not supported");
            return;
        }

        Image image = loadIconImage();
        TrayIcon trayIcon = new TrayIcon(image, "System Monitor");
        trayIcon.setImageAutoSize(true);
        popupMenu = createPopupMenu();
        createInvisibleFrame();

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    popupMenu.setLocation(e.getXOnScreen(), e.getYOnScreen());
                    popupMenu.setInvoker(frame);
                    popupMenu.setVisible(true);
                });
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Unable to add icon to tray");
            e.printStackTrace();
        }
    }

    private Image loadIconImage() {
        try {
            return Toolkit.getDefaultToolkit().getImage(SystemTrayApp.class.getResource("/icon.png"));
        } catch (Exception e) {
            System.out.println("Icon not found, using default");
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.BLUE);
            g2.fillRect(0, 0, 16, 16);
            g2.dispose();
            return image;
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.setFont(new Font("Arial", Font.PLAIN, 12));

        JMenu refreshMenu = new JMenu("Refresh");
        JMenu remoteMenu = new JMenu("Remote");
        refreshMenu.setFont(new Font("Arial", Font.PLAIN, 12));
        remoteMenu.setFont(new Font("Arial", Font.PLAIN, 12));
        JMenuItem updateWeather = new JMenuItem("Weather");
        updateWeather.setFont(new Font("Arial", Font.PLAIN, 11));
        JMenuItem updateNetwork = new JMenuItem("Network");
        updateNetwork.setFont(new Font("Arial", Font.PLAIN, 11));
        JMenuItem remoteServer = new JMenuItem("Server");
        remoteServer.setFont(new Font("Arial", Font.PLAIN, 11));
        JMenuItem remoteClient = new JMenuItem("Client");
        remoteClient.setFont(new Font("Arial", Font.PLAIN, 11));
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setFont(new Font("Arial", Font.PLAIN, 12));

        menu.add(settingsItem);
        menu.addSeparator();
        menu.add(refreshMenu);
        menu.addSeparator();
        menu.add(remoteMenu);
        menu.addSeparator();
        menu.add(exitItem);

        refreshMenu.add(updateWeather);
        refreshMenu.add(updateNetwork);

        remoteMenu.add(remoteServer);
        remoteMenu.add(remoteClient);

        settingsItem.addActionListener(e -> {
            SettingsDialog settingsDialog = new SettingsDialog(frame);
            settingsDialog.setVisible(true);
        });
        updateWeather.addActionListener(e -> SystemMonitorUI.updateWeatherInfo());
        updateNetwork.addActionListener(e -> SystemMonitorUI.initializeSystemInfo());
        remoteServer.addActionListener(e -> new Thread(() -> RemoteDesktopServer.main(null)).start());
        remoteClient.addActionListener(e -> new Thread(() -> RemoteDesktopClient.main(null)).start());

        exitItem.addActionListener(e -> System.exit(0));

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                frame.toFront();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                menu.setVisible(false);
            }
        });

        return menu;
    }

    private void createInvisibleFrame() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setSize(0, 0);
        frame.setLocationRelativeTo(null);
        frame.setType(Window.Type.UTILITY);
        frame.setVisible(true);
    }
}
