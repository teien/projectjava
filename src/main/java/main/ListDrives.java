package main;

import java.io.File;

public class ListDrives {
    public static void main(String[] args) {
        // Lấy danh sách các ổ đĩa
        File[] drives = File.listRoots();

        // Kiểm tra và in danh sách các ổ đĩa
        if (drives != null && drives.length > 0) {
            for (File drive : drives) {
                if (drive.getTotalSpace() > 0) {
                    System.out.println("Ổ đĩa: " + drive);
                    System.out.println("Dung lượng tổng: " + drive.getTotalSpace() + " bytes");
                    System.out.println("Dung lượng khả dụng: " + drive.getUsableSpace() + " bytes");
                    System.out.println("Dung lượng đã sử dụng: " + (drive.getTotalSpace() - drive.getUsableSpace()) + " bytes");
                    System.out.println();
                }

            }
        } else {
            System.out.println("Không có ổ đĩa nào được tìm thấy.");
        }
    }
}
