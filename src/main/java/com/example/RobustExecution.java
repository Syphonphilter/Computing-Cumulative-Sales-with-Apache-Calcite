package com.example;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.*;

// NOTE
// This class is in preview and not fully implemented
//
public class RobustExecution {

    private static final Logger LOGGER = Logger.getLogger(RobustExecution.class.getName());

    // UDF
    public static class MyToDate {
        public static Date eval(String dateStr) {
            // Note the pattern "M/d/yyyy" allows single-digit months/days
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr, fmt);
            return Date.valueOf(localDate);
        }
    }

    public static void main(String[] args) {
        setupLogging();

        try {
            Properties props = new Properties();
            props.put("model", "src/main/resources/model.json");
            Connection conn = DriverManager.getConnection("jdbc:calcite:", props);
            LOGGER.info("Connected to Calcite database.");

            // Register UDF
            CalciteConnection calciteConn = conn.unwrap(CalciteConnection.class);
            calciteConn.getRootSchema().add(
                    "MY_TO_DATE",
                    ScalarFunctionImpl.create(MyToDate.class, "eval")
            );

            // Updated query with MY_TO_DATE
            String query =
                    "WITH RECURSIVE daily_sales AS (" +
                            "  SELECT MY_TO_DATE(\"Order Date\") AS \"Order Date\", SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales" +
                            "  FROM superstore" +
                            "  GROUP BY \"Order Date\"" +
                            "  HAVING \"Order Date\" = (SELECT MIN(MY_TO_DATE(\"Order Date\")) FROM superstore)" +
                            "  UNION ALL" +
                            "  SELECT s.\"Order Date\", s.daily_sales + ds.daily_sales" +
                            "  FROM (" +
                            "    SELECT MY_TO_DATE(\"Order Date\") AS \"Order Date\", SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales" +
                            "    FROM superstore" +
                            "    GROUP BY \"Order Date\"" +
                            "  ) s" +
                            "  JOIN daily_sales ds ON s.\"Order Date\" = ds.\"Order Date\" + INTERVAL '1' DAY" +
                            ")" +
                            "SELECT \"Order Date\", SUM(daily_sales) AS cumulative_sales " +
                            "FROM daily_sales " +
                            "GROUP BY \"Order Date\" " +
                            "ORDER BY \"Order Date\"";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                LOGGER.info("Query executed successfully.");
                System.out.println("Date\t\tCumulative Sales");
                while (rs.next()) {
                    System.out.printf("%s\t%.2f%n",
                            rs.getDate("Order Date"),
                            rs.getDouble("cumulative_sales"));
                }
            } catch (SQLException e) {
                LOGGER.severe("Query execution failed: " + e.getMessage());
                System.err.println("An error occurred while executing the query. Check logs for details.");
            }

            conn.close();
        } catch (SQLException e) {
            LOGGER.severe("Database connection failed: " + e.getMessage());
            System.err.println("Failed to connect to the database.");
        }
    }

    private static void setupLogging() {
        try {
            FileHandler handler = new FileHandler("execution.log", true);
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.setLevel(Level.INFO);
        } catch (Exception e) {
            LOGGER.warning("Failed to set up logging: " + e.getMessage());
        }
    }
}
