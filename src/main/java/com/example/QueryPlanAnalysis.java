package com.example;

import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;
import org.apache.calcite.tools.*;
import org.apache.calcite.sql.SqlNode;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class QueryPlanAnalysis {

    // A user-defined function to parse "MM/dd/yyyy" date strings
    public static class MyToDate {
        public static Date eval(String dateStr) {
            if (dateStr == null) return null;
            dateStr = dateStr.trim();
            if (dateStr.isEmpty()) return null;

            // Single-digit month/day
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr, fmt);
            return Date.valueOf(localDate);
        }
    }

    public static void main(String[] args) {
        // 1) Connect via Calcite model
        Properties props = new Properties();
        props.put("model", "src/main/resources/model.json"); // Adjust path as needed

        try (Connection conn = DriverManager.getConnection("jdbc:calcite:", props)) {

            // 2) Unwrap, register UDF
            CalciteConnection calciteConn = conn.unwrap(CalciteConnection.class);
            calciteConn.getRootSchema().add(
                    "MY_TO_DATE",
                    ScalarFunctionImpl.create(MyToDate.class, "eval")
            );

            // 3) Build config with the SUPERSTORE schema
            FrameworkConfig config = Frameworks.newConfigBuilder()
                    .defaultSchema(calciteConn.getRootSchema().getSubSchema("SUPERSTORE"))
                    .build();

            // 4) The query that uses MY_TO_DATE(...) in a recursive CTE
            String query =
                    "WITH RECURSIVE daily_sales AS (\n" +
                            "  SELECT MY_TO_DATE(\"Order Date\") AS \"Order Date\", \n" +
                            "         SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales\n" +
                            "  FROM superstore\n" +
                            "  GROUP BY \"Order Date\"\n" +
                            "  HAVING \"Order Date\" = (\n" +
                            "    SELECT MIN(MY_TO_DATE(\"Order Date\")) FROM superstore\n" +
                            "  )\n" +
                            "  UNION ALL\n" +
                            "  SELECT s.\"Order Date\", s.daily_sales + ds.daily_sales\n" +
                            "  FROM (\n" +
                            "    SELECT MY_TO_DATE(\"Order Date\") AS \"Order Date\", \n" +
                            "           SUM(CAST(\"Sales\" AS DOUBLE)) AS daily_sales\n" +
                            "    FROM superstore\n" +
                            "    GROUP BY \"Order Date\"\n" +
                            "  ) s\n" +
                            "  JOIN daily_sales ds ON s.\"Order Date\" = ds.\"Order Date\" + INTERVAL '1' DAY\n" +
                            ")\n" +
                            "SELECT \"Order Date\", SUM(daily_sales) AS cumulative_sales\n" +
                            "FROM daily_sales\n" +
                            "GROUP BY \"Order Date\"\n" +
                            "ORDER BY \"Order Date\"";

            // -------------------------------------------------------------------
            // 5) Create a NEW Planner for this single query
            // -------------------------------------------------------------------
            Planner planner = Frameworks.getPlanner(config);

            // 6) parse -> validate -> rel
            //    Use planner.parse(...) instead of SqlParser.create(...).parseQuery()
            SqlNode parsed = planner.parse(query);
            SqlNode validated = planner.validate(parsed);
            RelRoot relRoot = planner.rel(validated);
            RelNode relNode = relRoot.rel;

            // 7) Print the plan
            System.out.println("Query Plan:\n" + RelOptUtil.toString(relNode));

            // 8) Execute the plan
            try (PreparedStatement ps = RelRunners.run(relNode);
                 ResultSet rs = ps.executeQuery()) {

                System.out.println("\nResults:");
                while (rs.next()) {
                    System.out.printf("%s\t%.2f%n",
                            rs.getDate("Order Date"),
                            rs.getDouble("cumulative_sales"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
