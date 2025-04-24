# Computing Cumulative Sales With Apache Calcite

## Overview
This project develops a system for analyzing the **Superstore** dataset, focusing on cumulative sales computation, query execution analysis, and sales‑trend visualization. The system leverages **Apache Calcite** for query processing, implements recursive CTEs and window functions for time‑series analysis, and visualizes sales trends using **JFreeChart**.

### Key components
| Component | Purpose |
|-----------|---------|
| **BaseCumulativeSales** | Computes cumulative sales across *all* categories using a recursive CTE. |
| **InteractiveCLI** | Allows users to compute cumulative sales for a specific category via a command‑line interface. |
| **QueryPlanAnalysis** | Analyzes the logical query plan of the recursive CTE using Apache Calcite. |
| **WindowFunctionAnalysis** | Computes cumulative sales using a SQL window function. |
| **Sales Trend Chart** | Visualizes daily sales and a 7‑day moving average in a `JFrame` using JFreeChart. |

The project was developed collaboratively by **Abdulkadir Richard Bala** and **Harry Okoro**. The results are documented in a paper submitted to an IEEE conference.

---

## Prerequisites
* **Java** — JDK 11 or higher
* **Maven** — for dependency management
* **IntelliJ IDEA** (recommended) or any Java IDE
* **Apache Calcite** (pulled via Maven)
* **JFreeChart** (pulled via Maven)
* **Superstore Dataset** — CSV file `superstore.csv`

---

## Setup Instructions

### 1 · Clone the repository
```bash
git clone <repository-url>
cd superstore-sales-analysis
```

### 2 · Place the dataset
* Download **superstore.csv** and place it in the `data/` directory.
* Ensure the `model.json` path points to the correct location.

### 3 · Install dependencies
Open the project in IntelliJ IDEA (or your preferred IDE) and run:
```bash
mvn install
```

### 4 · Configure `model.json`
```json
{
  "version": "1.0",
  "defaultSchema": "SUPERSTORE",
  "schemas": [
    {
      "name": "SUPERSTORE",
      "type": "custom",
      "factory": "org.apache.calcite.adapter.csv.CsvSchemaFactory",
      "operand": {
        "directory": "data"
      }
    }
  ]
}
```

### 5 · Build the project
```bash
mvn clean package
```

---

## Usage Instructions

### First Change Working Directory


### BaseCumulativeSales
Compute cumulative sales across *all* categories:
```bash
cd ../
mvn exec:java@base-sales
```

### InteractiveCLI
Interactive cumulative sales for a chosen category:
```bash
cd ../
mvn exec:java@interactive-cli
```

### QueryPlanAnalysis
Show the logical query plan for the recursive CTE:
```bash
cd ../
mvn exec:java@query-plan
```

### WindowFunctionAnalysis
Compute cumulative sales via a window function:
```bash
cd ../
mvn exec:java@window-fn
```

### Sales Trend Chart
Render the daily‑sales chart in a `JFrame`:
```bash
cd ../
mvn exec:java@trend-analysis
```

---

## Project Structure
```
├── data/
│   └── superstore.csv
├── model.json
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    ├── BaseCumulativeSales.java
                    ├── InteractiveCLI.java
                    ├── QueryPlanAnalysis.java
                    ├── RobustExecution.java
                    └── TrendAnalysis.java
                    └── WindowFunctions.java
```

---

## Contributors
* **Abdulkadir Richard Bala** — BaseCumulativeSales, InteractiveCLI, QueryPlanAnalysis, WindowFunctionAnalysis, Calcite schema setup.
* **Harry Okoro** — Environment setup, data sourcing, literature review, experimental setup, data analysis.

---

## Acknowledgments
* Readme content refined with **Grammarly AI** and clarified using **Grok AI**.
* Developed as part of a course assignment at the **University of New Brunswick**.

---

## License
This project is released under the **MIT License**. See the `LICENSE` file for details.

