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
            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> SettingsPanel.applySettings());
            add(applyButton, BorderLayout.SOUTH);
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
