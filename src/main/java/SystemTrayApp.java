import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.Time;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemTrayApp {
    public static class SettingsDialog extends JDialog {

        public SettingsDialog(JFrame parent) {
            super(parent, "Settings", true);
            setSize(800, 450);
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            SettingsPanel settingsPanel = new SettingsPanel();
            add(settingsPanel, BorderLayout.CENTER);
            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> {
                SettingsPanel.applySettings();
            });
            add(applyButton, BorderLayout.SOUTH);
            SettingsPanel.loadSettings();
        }
    }

    private TrayIcon trayIcon;
    private JPopupMenu popupMenu;

    public SystemTrayApp() {
        if (!SystemTray.isSupported()) {
            System.out.println("System Tray is not supported");
            return;
        }


        Image image = loadIconImage();
        trayIcon = new TrayIcon(image, "System Monitor");
        trayIcon.setImageAutoSize(true);

        popupMenu = createPopupMenu();

        createInvisibleFrame();

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopupMenu(e);
            }
            private void maybeShowPopupMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Unable to add icon to tray");
            e.printStackTrace();
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updateTrayIcon, 0, 1, TimeUnit.SECONDS);

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent mouseEvent) {
                maybeHidePopupMenu(mouseEvent);
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private Image loadIconImage() {
        try {
            return Toolkit.getDefaultToolkit().getImage(SystemTrayApp.class.getResource("icon.png"));
        } catch (Exception e) {
            System.out.println("Icon not found, using default");
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem settingsItem = new JMenuItem("Settings");
        JMenuItem exitItem = new JMenuItem("Exit");

        menu.add(settingsItem);
        menu.addSeparator();
        menu.add(exitItem);

        settingsItem.addActionListener(e -> {
            JFrame frame = new JFrame();
            SettingsDialog settingsDialog = new SettingsDialog(frame);
            settingsDialog.setVisible(true);

        });
        exitItem.addActionListener(e -> System.exit(0));
        return menu;
    }

    private void createInvisibleFrame() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setSize(0, 0);
        frame.setLocationRelativeTo(null);
    }

    private void updateTrayIcon() {
        SwingUtilities.invokeLater(() -> trayIcon.setToolTip("Updated: " + System.currentTimeMillis()));
    }

    private void maybeHidePopupMenu(MouseEvent e) {
        if (popupMenu.isVisible()) {
            Point mousePoint = e.getLocationOnScreen();
            Rectangle popupBounds = popupMenu.getBounds();
            Point popupLocation = popupMenu.getLocationOnScreen();
            popupBounds.setLocation(popupLocation);

            if (!popupBounds.contains(mousePoint)) {
                popupMenu.setVisible(false);
            }
        }
    }
}
