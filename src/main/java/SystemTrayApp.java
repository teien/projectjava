import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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
    private JFrame frame;

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

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updateTrayIcon, 0, 1, TimeUnit.SECONDS);

        // Add a global mouse listener to hide the popup menu
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (popupMenu.isVisible() && me.getID() == MouseEvent.MOUSE_PRESSED) {
                    Point mousePoint = me.getLocationOnScreen();
                    Rectangle popupBounds = popupMenu.getBounds();
                    Point popupLocation = popupMenu.getLocationOnScreen();
                    popupBounds.setLocation(popupLocation);

                    if (!popupBounds.contains(mousePoint)) {
                        popupMenu.setVisible(false);
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private Image loadIconImage() {
        try {
            return Toolkit.getDefaultToolkit().getImage(SystemTrayApp.class.getResource("/icon.png"));
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
            SettingsDialog settingsDialog = new SettingsDialog(frame);
            settingsDialog.setVisible(true);
        });
        exitItem.addActionListener(e -> System.exit(0));

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

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


        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (popupMenu.isVisible()) {
                    popupMenu.setVisible(false);
                }
            }
        });
    }

    private void updateTrayIcon() {
        SwingUtilities.invokeLater(() -> trayIcon.setToolTip("Updated: " + System.currentTimeMillis()));
    }
}
