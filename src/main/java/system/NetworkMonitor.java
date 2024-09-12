package system;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONObject;
import oshi.hardware.NetworkIF;
import settings.SettingsLogger;

import javax.swing.*;
import java.awt.*;

public class NetworkMonitor extends JPanel {

    private final XYSeries downloadSeries;
    private long lastDownloadBytes;
    private static NetworkIF networkIF;
    private long startTime;
    private boolean firstUpdateDownload = true;
    private boolean firstUpdateUpload = true;
    private XYPlot downloadPlot;
    private XYPlot uploadPlot;
    private final XYSeries uploadSeries;
    private long lastUploadBytes;

    public NetworkMonitor(NetworkIF networkIF, Boolean isDownload) {
        NetworkMonitor.networkIF = networkIF;
        this.startTime = System.currentTimeMillis();
        downloadSeries = new XYSeries("Download Speed");
        uploadSeries = new XYSeries("Upload Speed");
        createChart(new XYSeriesCollection(isDownload ? downloadSeries : uploadSeries), isDownload ? Color.GREEN : Color.RED, isDownload);
    }

    public static void setNetworkIF(NetworkIF networkIF) {
        NetworkMonitor.networkIF = networkIF;
    }

    public static NetworkIF getNetworkIF() {
        return networkIF;
    }

    public void resetChart() {
        downloadSeries.clear();
        uploadSeries.clear();
        firstUpdateDownload = true;
        firstUpdateUpload = true;
        lastDownloadBytes = 0;
        lastUploadBytes = 0;
        startTime = System.currentTimeMillis();
    }

    public void startDownload() {
        updateDownloadChart();
    }

    public void startUpload() {
        updateUploadChart();
    }

    public void createChart(XYSeriesCollection dataset, Color color, boolean isDownload) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false
        );
        customizeChart(chart, color);
        ChartPanel panel = new ChartPanel(chart);
        setLayout(new GridLayout(1, 1));
        JSONObject settings = SettingsLogger.loadSettings();
        int wc = settings.getJSONObject("Chart").getInt("chartWidth");
        panel.setPreferredSize(new Dimension(wc, 50));
        setPreferredSize(new Dimension(wc, 50));
        panel.setBackground(new Color(0, 0,0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        add(panel);
        setBackground(new Color(0, 0, 0, 0));
        if (isDownload) {
            this.downloadPlot = chart.getXYPlot();
            if (networkIF != null) {
                networkIF.updateAttributes();
                lastDownloadBytes = networkIF.getBytesRecv();
            }
        } else {
            this.uploadPlot = chart.getXYPlot();
            if (networkIF != null) {
                networkIF.updateAttributes();
                lastUploadBytes = networkIF.getBytesSent();
            }
        }
    }

    private void customizeChart(JFreeChart chart, Color color) {
        chart.setTitle((String) null);
        chart.setAntiAlias(true);
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));

        XYPlot plot = chart.getXYPlot();
        plot.setOutlinePaint(Color.WHITE);
        plot.setBackgroundPaint(new Color(0, 0, 0, 0));
        plot.setDomainGridlinePaint(new Color(0, 0, 0, 0));
        plot.setRangeGridlinePaint(new Color(0, 0, 0, 0));

        XYAreaRenderer renderer = new XYAreaRenderer();
        plot.setRenderer(renderer);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesFillPaint(0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRange(true);
        yAxis.setTickUnit(new NumberTickUnit(1));
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarksVisible(false);
        yAxis.setAxisLinePaint(new Color(0, 0, 0, 0));

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(0.0, 60.0);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarksVisible(false);
        xAxis.setAxisLinePaint(new Color(0, 0, 0, 0));
    }

    void updateDownloadChart() {
        if (networkIF != null) {
            networkIF.updateAttributes();
            long downloadBytes = networkIF.getBytesRecv();

            if (firstUpdateDownload) {
                firstUpdateDownload = false;
                lastDownloadBytes = downloadBytes;
                return;
            }

            double downloadSpeedMbps = (double) (downloadBytes - lastDownloadBytes) * 8 / 1024 / 1024;
            lastDownloadBytes = downloadBytes;
            double currentTime = (System.currentTimeMillis() - startTime) / 1000.0;

            downloadSeries.add(currentTime, downloadSpeedMbps);

            while (downloadSeries.getItemCount() > 0 && downloadSeries.getMaxX() - downloadSeries.getMinX() > 60) {
                downloadSeries.remove(0);
            }

            NumberAxis xAxis = (NumberAxis) downloadPlot.getDomainAxis();
            xAxis.setRange(currentTime - 60, currentTime);

            downloadPlot.setNotify(true);
        }
    }

    void updateUploadChart() {
        if (networkIF != null) {
            networkIF.updateAttributes();
            long uploadBytes = networkIF.getBytesSent();

            if (firstUpdateUpload) {
                firstUpdateUpload = false;
                lastUploadBytes = uploadBytes;
                return;
            }

            double uploadSpeedMbps = (double) (uploadBytes - lastUploadBytes) * 8 / 1024 / 1024;
            lastUploadBytes = uploadBytes;
            double currentTime = (System.currentTimeMillis() - startTime) / 1000.0;

            uploadSeries.add(currentTime, uploadSpeedMbps);

            while (uploadSeries.getItemCount() > 0 && uploadSeries.getMaxX() - uploadSeries.getMinX() > 60) {
                uploadSeries.remove(0);
            }

            NumberAxis xAxis = (NumberAxis) uploadPlot.getDomainAxis();
            xAxis.setRange(currentTime - 60, currentTime);

            uploadPlot.setNotify(true);
        }
    }
}
