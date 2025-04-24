package com.example;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class WindowFunctions {

    public static class MyToDate {
        public static Date eval(String dateStr) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr, fmt);
            return Date.valueOf(localDate);
        }
    }

    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.put("model", "src/main/resources/model.json");
            Connection conn = DriverManager.getConnection("jdbc:calcite:", props);

            // Register the date-parsing UDF
            CalciteConnection calciteConn = conn.unwrap(CalciteConnection.class);
            calciteConn.getRootSchema().add(
                    "MY_TO_DATE",
                    ScalarFunctionImpl.create(MyToDate.class, "eval")
            );

            // Convert "Order Date" to an actual date in a subselect,
            // then apply the window function for a running total
            String query =
                    "SELECT \"Order Date\", daily_sales,\n" +
                            "       SUM(daily_sales) OVER (ORDER BY \"Order Date\") AS cumulative_sales\n" +
                            "FROM (\n" +
                            "  SELECT MY_TO_DATE(\"Order Date\") AS \"Order Date\", SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales\n" +
                            "  FROM superstore\n" +
                            "  GROUP BY \"Order Date\"\n" +
                            ") t\n" +
                            "ORDER BY \"Order Date\"";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                System.out.println("Date\t\tDaily Sales\tCumulative Sales");
                while (rs.next()) {
                    System.out.printf("%s\t%.2f\t\t%.2f%n",
                            rs.getDate("Order Date"),
                            rs.getDouble("daily_sales"),
                            rs.getDouble("cumulative_sales"));
                }
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
