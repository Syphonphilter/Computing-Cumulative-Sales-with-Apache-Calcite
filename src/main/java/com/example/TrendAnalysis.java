package com.example;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class TrendAnalysis extends JFrame {

    public static class MyToDate {
        public static Date eval(String dateStr) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr.trim(), fmt);
            return Date.valueOf(localDate);
        }
    }

    public TrendAnalysis() {
        setTitle("Sales Line Chart");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        TimeSeries dailySeries = new TimeSeries("Daily Sales");
        TimeSeries avgSeries = new TimeSeries("7-Day Avg");

        try {
            Properties props = new Properties();
            props.put("model", "src/main/resources/model.json");
            Connection conn = DriverManager.getConnection("jdbc:calcite:", props);

            CalciteConnection calciteConn = conn.unwrap(CalciteConnection.class);
            calciteConn.getRootSchema().add(
                    "MY_TO_DATE",
                    ScalarFunctionImpl.create(MyToDate.class, "eval")
            );

            String query =
                    "WITH daily AS (\n" +
                            "  SELECT MY_TO_DATE(\"Order Date\") AS \"Order Date\",\n" +
                            "         SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales\n" +
                            "  FROM superstore\n" +
                            "  GROUP BY \"Order Date\"\n" +
                            ")\n" +
                            "SELECT \"Order Date\", daily_sales,\n" +
                            "  AVG(daily_sales) OVER (\n" +
                            "    ORDER BY \"Order Date\"\n" +
                            "    ROWS BETWEEN 6 PRECEDING AND CURRENT ROW\n" +
                            "  ) AS moving_avg\n" +
                            "FROM daily\n" +
                            "ORDER BY \"Order Date\"";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Date date = rs.getDate("Order Date");
                    double sales = rs.getDouble("daily_sales");
                    double avg = rs.getDouble("moving_avg");

                    Day d = new Day(date);
                    dailySeries.addOrUpdate(d, sales);
                    avgSeries.addOrUpdate(d, avg);
                }
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(dailySeries);
        dataset.addSeries(avgSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Sales Trend",
                "Date",
                "Sales",
                dataset,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Series 0: Daily Sales
        renderer.setSeriesPaint(0, new Color(40, 110, 255));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-2, -2, 4, 4));

        // Series 1: Moving Average
        renderer.setSeriesPaint(1, new Color(255, 100, 0));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-2, -2, 4, 4));

        plot.setRenderer(renderer);

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrendAnalysis().setVisible(true));
    }
}
