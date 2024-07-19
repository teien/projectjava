package system;

import system.ConnectTCPClient_Server.RemoteDesktopClient;
import system.ConnectTCPClient_Server.RemoteDesktopServer;
import settings.SettingsPanel;
import main.SystemMonitorUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;


public class SystemTrayApp {

    public static class SettingsDialog extends JDialog {

        public SettingsDialog(JFrame parent) {
            super(parent, "Settings", true);
            setSize(730, 350);
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
        JMenuItem refreshMenu = new JMenuItem("Refresh");
        JMenu remoteMenu = new JMenu("Remote");
        JMenuItem updateWeather = new JMenuItem("Weather");
        JMenuItem updateNetwork = new JMenuItem("Network");
        JMenuItem remoteServer = new JMenuItem("Server");
        JMenuItem remoteClient = new JMenuItem("Client");
        JMenuItem restartItem = new JMenuItem("Restart");
        JMenuItem exitItem = new JMenuItem("Exit");
        menu.setBorder(new LineBorder(Color.DARK_GRAY, 1));
        setFont(settingsItem, updateWeather, updateNetwork, remoteServer, remoteClient, restartItem, exitItem,refreshMenu,remoteMenu);


        menu.add(settingsItem);
        menu.addSeparator();
        menu.add(refreshMenu);
        menu.addSeparator();
        menu.add(remoteMenu);
        menu.addSeparator();
        menu.add(restartItem);
        menu.addSeparator();
        menu.add(exitItem);


       /* refreshMenu.add(updateWeather);
        refreshMenu.addSeparator();
        refreshMenu.add(updateNetwork);*/

        remoteMenu.add(remoteServer);
        remoteMenu.addSeparator();
        remoteMenu.add(remoteClient);


        settingsItem.addActionListener(e -> {
            SettingsDialog settingsDialog = new SettingsDialog(frame);
            settingsDialog.setVisible(true);
        });

        /*updateWeather.addActionListener(e -> new Thread(() -> {
            try {
                SystemMonitorUI.updateWeatherInfo();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).start());*/
        /*updateNetwork.addActionListener(e -> new Thread( () -> {
            try {
                SystemMonitorUI.initializeSystemInfo();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).start());*/
        refreshMenu.addActionListener(e -> new Thread(() -> {
            try {
                SystemMonitorUI.initializeSystemInfo();
                SystemMonitorUI.updateWeatherInfo();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).start());
        remoteServer.addActionListener(e -> new Thread(() -> {
            try {
                 new RemoteDesktopServer().createAndShowGUI();

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).start());
        remoteClient.addActionListener(e -> new Thread(() -> {
            try {
                RemoteDesktopClient client = new RemoteDesktopClient();
                client.setVisible(true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).start());




        restartItem.addActionListener(e -> {
            SystemMonitorUI.restart();
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
    private void setFont(JMenuItem ...items) {
        for (JMenuItem item : items) {
            item.setFont(new Font("JetBrains Mono Medium", Font.PLAIN, 10));
        }
    }
    private void createInvisibleFrame() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Change to HIDE_ON_CLOSE
        frame.setUndecorated(true);
        frame.setSize(0, 0);
        frame.setLocationRelativeTo(null);
        frame.setType(Window.Type.UTILITY);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
                popupMenu.setVisible(false);
            }
        });
        frame.setVisible(true);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SystemTrayApp::new);
    }
}
