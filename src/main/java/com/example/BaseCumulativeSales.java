package com.example;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class BaseCumulativeSales {

    /**
     * UDF that parses a "MM/dd/yyyy" string (e.g. "11/8/2016") into a java.sql.Date.
     */
    public static class MyToDate {
        public static Date eval(String dateStr) {
            // Note the pattern "M/d/yyyy" allows single-digit months/days
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr, fmt);
            return Date.valueOf(localDate);
        }
    }

    public static void main(String[] args) {
        try {
            // 1) Load Calcite with our model.json
            Properties props = new Properties();
            props.put("model", "src/main/resources/model.json"); // Adjust path if needed
            Connection conn = DriverManager.getConnection("jdbc:calcite:", props);

            // 2) Unwrap the CalciteConnection and register our UDF
            CalciteConnection calciteConn = conn.unwrap(CalciteConnection.class);
            // "MY_TO_DATE" is how we'll call it in SQL
            calciteConn.getRootSchema().add(
                    "MY_TO_DATE",
                    ScalarFunctionImpl.create(MyToDate.class, "eval")
            );

            // 3) Define the recursive CTE query using MY_TO_DATE(...) to parse the date
            String query = ""
                    + "WITH RECURSIVE daily_sales AS ("
                    + "  SELECT"
                    + "    MY_TO_DATE(\"Order Date\") AS dt,"
                    + "    SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales"
                    + "  FROM superstore"
                    + "  GROUP BY \"Order Date\""
                    + "  HAVING MY_TO_DATE(\"Order Date\") = ("
                    + "    SELECT MIN(MY_TO_DATE(\"Order Date\"))"
                    + "    FROM superstore"
                    + "  )"
                    + "  UNION ALL"
                    + "  SELECT"
                    + "    s.dt,"
                    + "    s.daily_sales + ds.daily_sales"
                    + "  FROM ("
                    + "    SELECT"
                    + "      MY_TO_DATE(\"Order Date\") AS dt,"
                    + "      SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales"
                    + "    FROM superstore"
                    + "    GROUP BY \"Order Date\""
                    + "  ) s"
                    + "  JOIN daily_sales ds"
                    + "    ON s.dt = ds.dt + INTERVAL '1' DAY"
                    + ")"
                    + "SELECT dt AS \"Order Date\","
                    + "       SUM(daily_sales) AS cumulative_sales "
                    + "FROM daily_sales "
                    + "GROUP BY dt "
                    + "ORDER BY dt";

            // 4) Execute the query
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                System.out.println("Date\t\tCumulative Sales");
                while (rs.next()) {
                    System.out.printf("%s\t%.2f%n",
                            rs.getDate("Order Date"),
                            rs.getDouble("cumulative_sales"));
                }
            }

            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
