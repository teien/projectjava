import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

class NetworkPanel extends JPanel {
    private static final int MAX_HISTORY = 50;
    private Queue<Integer> downloadSpeeds = new LinkedList<>();
    private Queue<Integer> uploadSpeeds = new LinkedList<>();
    private Random random = new Random();

    public NetworkPanel() {
        for (int i = 0; i < MAX_HISTORY; i++) {
            downloadSpeeds.add(0);
            uploadSpeeds.add(0);
        }
    }

    public void updateSpeeds() {
        if (downloadSpeeds.size() >= MAX_HISTORY) {
            downloadSpeeds.poll();
        }
        if (uploadSpeeds.size() >= MAX_HISTORY) {
            uploadSpeeds.poll();
        }

        downloadSpeeds.add(random.nextInt(100)); // Simulating download speed
        uploadSpeeds.add(random.nextInt(50));    // Simulating upload speed

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGraph(g, downloadSpeeds, Color.GREEN, 50, "Download");
        drawGraph(g, uploadSpeeds, Color.RED, 150, "Upload");
    }

    private void drawGraph(Graphics g, Queue<Integer> speeds, Color color, int yOffset, String label) {
        int width = getWidth() - 20;
        int height = 50;

        g.setColor(Color.BLACK);
        g.drawString(label, 10, yOffset - 10);
        g.drawRect(10, yOffset - height, width, height);

        int prevX = 10;
        int prevY = yOffset - speeds.peek();

        int i = 0;
        for (int speed : speeds) {
            int newX = 10 + (width * i / MAX_HISTORY);
            int newY = yOffset - speed;

            g.setColor(color);
            g.drawLine(prevX, prevY, newX, newY);

            prevX = newX;
            prevY = newY;
            i++;
        }
    }
}
