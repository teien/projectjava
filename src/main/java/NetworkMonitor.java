import org.jetbrains.annotations.NotNull;
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
import javax.swing.*;
import java.awt.*;


public class NetworkMonitor extends JPanel {

    private final XYSeries downloadSeries;
    private long lastDownloadBytes;
    private static NetworkIF networkIF;
    private final long startTime;
    private boolean firstUpdateDownload = true;
    private boolean firstUpdateUpload = true;
    private XYPlot downloadPlot;
    private XYPlot uploadPlot;
    private final XYSeries uploadSeries;
    private long lastUploadBytes;

    public NetworkMonitor(NetworkIF networkIF,Boolean isDownload) {
        NetworkMonitor.networkIF = networkIF;
        this.startTime = System.currentTimeMillis();
        //


        // Create dataset
        XYSeriesCollection datasetDownload = new XYSeriesCollection();
        XYSeriesCollection datasetUpload = new XYSeriesCollection();
        downloadSeries = new XYSeries("Download Speed");
        datasetDownload.addSeries(downloadSeries);
        uploadSeries = new XYSeries("Upload Speed");
        datasetUpload.addSeries(uploadSeries);
        if (isDownload) {
            createChart(datasetDownload, Color.GREEN, true);
        } else {
            createChart(datasetUpload, Color.RED, false);
        }
    }

    public void startDownload() {
        updateDownloadChart();
    }
    public void startUpload() {
        updateUploadChart();
    }
    public void createChart(XYSeriesCollection dataset, Color color, boolean isDownload) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false
        );
        chart.setTitle((String) null);
        chart.setAntiAlias(true);
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(new Color(0, 0, 0, 0)); // Set chart background to transparent


        XYPlot plot = getXyPlot(color, chart);

        // Customize Y axis
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRange(true);
        yAxis.setTickUnit(new NumberTickUnit(1));
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarksVisible(false);
        yAxis.setAxisLinePaint(new Color(0, 0, 0, 0));

        // Customize X axis
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(0.0, 60.0);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarksVisible(false);
        xAxis.setAxisLinePaint(new Color(0, 0, 0, 0));

        // Create Panel
        ChartPanel panel = new ChartPanel(chart);
        JSONObject settings = SettingsLogger.loadSettings();
        int w = settings.getJSONObject("Screen").getInt("width");
        panel.setPreferredSize(new Dimension(214, 60)); // Set preferred size for the chart panel
        panel.setBackground(new Color(0, 0, 0, 0)); // Set panel background to transparent
        panel.setOpaque(false); // Ensure panel is non-opaque
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        add(panel);
        setBackground(new Color(0, 0, 0, 0)); // Set JPanel background to transparent
        setOpaque(false); // Ensure JPanel is non-opaque

        if (isDownload) {
            this.downloadPlot = plot;
            if (networkIF != null) {
                networkIF.updateAttributes();
                lastDownloadBytes = networkIF.getBytesRecv();
            }
        } else {
            this.uploadPlot = plot;
            if (networkIF != null) {
                networkIF.updateAttributes();
                lastUploadBytes = networkIF.getBytesSent();
            }
        }
    }

    private static @NotNull XYPlot getXyPlot(Color color, JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        plot.setOutlinePaint(Color.WHITE);
        plot.setBackgroundPaint(Color.WHITE);
        XYAreaRenderer renderer = new XYAreaRenderer();
        plot.setRenderer(renderer);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesFillPaint(0, new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));

        // Set plot background to transparent
        plot.setBackgroundPaint(new Color(0, 0, 0, 0));
        plot.setDomainGridlinePaint(new Color(0, 0, 0, 0));
        plot.setRangeGridlinePaint(new Color(0, 0, 0, 0));
        return plot;
    }

    private void updateDownloadChart() {
        if (networkIF != null) {
            networkIF.updateAttributes();
            long downloadBytes = networkIF.getBytesRecv();

            if (firstUpdateDownload) {
                firstUpdateDownload = false;
                lastDownloadBytes = downloadBytes;
                return; // Skip the first update to avoid incorrect high value
            }

            double downloadSpeedMbps = (double) (downloadBytes - lastDownloadBytes) * 8 / 1024 / 1024;
            lastDownloadBytes = downloadBytes;
            double currentTime = (System.currentTimeMillis() - startTime) / 1000.0;

            // Add new data point
            downloadSeries.add(currentTime, downloadSpeedMbps);

            // Remove old data points if they exceed 60 seconds
            while (downloadSeries.getItemCount() > 0 && downloadSeries.getMaxX() - downloadSeries.getMinX() > 60) {
                downloadSeries.remove(0);
            }

            // Update X axis range
            NumberAxis xAxis = (NumberAxis) downloadPlot.getDomainAxis();
            xAxis.setRange(currentTime - 60, currentTime);

            // Refresh the chart
            downloadPlot.setNotify(true);
        }
    }

    private void updateUploadChart() {
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

            // Add new data point
            uploadSeries.add(currentTime, uploadSpeedMbps);

            // Remove old data points if they exceed 60 seconds
            while (uploadSeries.getItemCount() > 0 && uploadSeries.getMaxX() - uploadSeries.getMinX() > 60) {
                uploadSeries.remove(0);
            }

            // Update X axis range
            NumberAxis xAxis = (NumberAxis) uploadPlot.getDomainAxis();
            xAxis.setRange(currentTime - 60, currentTime);

            // Refresh the chart
            uploadPlot.setNotify(true);
        }
    }


}
