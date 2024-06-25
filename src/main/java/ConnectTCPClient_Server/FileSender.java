package ConnectTCPClient_Server;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class FileSender extends JFrame {
    private JTextArea chatArea;
    private DataOutputStream fileDos;

    public void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file)) {
                fileDos.writeUTF("FILE_SEND");
                fileDos.writeUTF(file.getName());
                fileDos.writeLong(file.length());

                // Tạo JProgressBar
                JProgressBar progressBar = new JProgressBar(0, (int) file.length());
                progressBar.setStringPainted(true);
                JOptionPane.showMessageDialog(this, progressBar, "Đang gửi file...", JOptionPane.PLAIN_MESSAGE);

                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalBytesRead = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fileDos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    progressBar.setValue(totalBytesRead);
                }
                fileDos.flush();
                JOptionPane.showMessageDialog(this, "File đã được gửi thành công.");
                chatArea.append("File " + file.getName() + " đã được gửi thành công.\n");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Constructor hoặc method khởi tạo giao diện người dùng
    public FileSender(DataOutputStream dos, JTextArea chatArea) {
        this.fileDos = dos;
        this.chatArea = chatArea;
        // Khởi tạo giao diện người dùng tại đây
    }

    public static void main(String[] args) {
        // Tạo DataOutputStream và JTextArea cho ví dụ
        JTextArea chatArea = new JTextArea();
        DataOutputStream dos = new DataOutputStream(System.out); // Chỉ để ví dụ, bạn cần thay bằng stream thực tế
        FileSender sender = new FileSender(dos, chatArea);

        // Hiển thị giao diện và gọi sendFile()
        JFrame frame = new JFrame("File Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JButton sendButton = new JButton("Send File");
        sendButton.addActionListener(e -> sender.sendFile());
        frame.add(sendButton, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
