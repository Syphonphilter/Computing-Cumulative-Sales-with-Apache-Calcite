package com.example;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Scanner;

public class InteractiveCLI {

    public static class MyToDate {
        public static Date eval(String dateStr) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
                LocalDate localDate = LocalDate.parse(dateStr, fmt);
                return Date.valueOf(localDate);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse date: " + dateStr, e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.put("model", "src/main/resources/model.json");
            Connection conn = DriverManager.getConnection("jdbc:calcite:", props);

            CalciteConnection calciteConn = conn.unwrap(CalciteConnection.class);
            calciteConn.getRootSchema().add(
                    "MY_TO_DATE",
                    ScalarFunctionImpl.create(MyToDate.class, "eval")
            );

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Enter a category (e.g., Furniture) or 'all' for all categories:");
                String category = scanner.nextLine();

                String query =
                        "WITH RECURSIVE date_range AS (" +
                                "  SELECT MIN(MY_TO_DATE(\"Order Date\")) AS \"Order Date\" " +
                                "  FROM superstore " +
                                (category.equalsIgnoreCase("all") ? "" : "  WHERE \"Category\" = ? ") +
                                "  UNION ALL " +
                                "  SELECT \"Order Date\" + INTERVAL '1' DAY " +
                                "  FROM date_range " +
                                "  WHERE \"Order Date\" < (SELECT MAX(MY_TO_DATE(\"Order Date\")) FROM superstore" +
                                (category.equalsIgnoreCase("all") ? "" : " WHERE \"Category\" = ? ") + ")" +
                                "), " +
                                "daily_sales AS (" +
                                "  SELECT d.\"Order Date\", " +
                                "         COALESCE(SUM(CAST(s.\"Sales\" AS DOUBLE)), 0) AS daily_sales " +
                                "  FROM date_range d " +
                                "  LEFT JOIN superstore s ON MY_TO_DATE(s.\"Order Date\") = d.\"Order Date\" " +
                                (category.equalsIgnoreCase("all") ? "" : "  AND s.\"Category\" = ? ") +
                                "  GROUP BY d.\"Order Date\" " +
                                "), " +
                                "cumulative_sales AS (" +
                                "  SELECT \"Order Date\", daily_sales, " +
                                "         SUM(daily_sales) OVER (ORDER BY \"Order Date\") AS cumulative_sales " +
                                "  FROM daily_sales " +
                                ") " +
                                "SELECT \"Order Date\", cumulative_sales " +
                                "FROM cumulative_sales " +
                                "ORDER BY \"Order Date\"";

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    if (!category.equalsIgnoreCase("all")) {
                        stmt.setString(1, category);
                        stmt.setString(2, category);
                        stmt.setString(3, category);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        System.out.println("Date\t\tCumulative Sales");
                        while (rs.next()) {
                            System.out.printf("%s\t%.2f%n",
                                    rs.getDate("Order Date"),
                                    rs.getDouble("cumulative_sales"));
                        }
                    }
                }
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}