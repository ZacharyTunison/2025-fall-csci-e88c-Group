GoldJob – KPI Aggregation & Metrics

This Spark job (GoldJob.scala) performs KPI aggregation on the conformed Silver layer data. It computes multiple weekly-level metrics including trip volume, average duration and distance, total revenue, and percentage of peak-hour and night trips. Each run writes its results to a timestamped folder (run_YYYYMMDD_HHmmss), enabling comparison across runs through the GoldValidationJob.

Inputs:
- /opt/spark-data/silver/trips_conformed/ → Conformed Silver data (cleaned, standardized, joined)

Outputs:
The job creates a new subfolder under /opt/spark-data/gold/kpis/ for each run:
/opt/spark-data/gold/kpis/run_<timestamp>/

Each run includes:
- weekly_trip_volume/: Trips aggregated by borough and week  
- weekly_trips_revenue/: Weekly total trips and revenue  
- time_vs_distance/: Average duration and distance per week  
- kpi_summary/: Summary KPIs (e.g., avg revenue per mile, night trip %, peak hour trip %)

Sample output:
=== GOLD JOB: READING SILVER CONFORMED DATA ===
Loaded 2,757,576 rows from Silver.

+-----------------+--------------+-----------+
|pickup_week_start|pickup_borough|trip_volume|
+-----------------+--------------+-----------+
|2024-12-30       |Bronx         |161        |
|2024-12-30       |Brooklyn      |1580       |
|2024-12-30       |Manhattan     |341228     |
...

Peak Hour Trip Percentage: 21.05%
Avg Revenue per Mile: $8.83
Night Trip Percentage: 9.47%

=== SAVING GOLD OUTPUTS ===
Saved: /opt/spark-data/gold/kpis/run_20251106_003747/weekly_trip_volume
Saved: /opt/spark-data/gold/kpis/run_20251106_003747/kpi_summary

Run Instructions:
From project root:
sbt "spark/runMain org.cscie88c.spark.GoldKPIJob"

Or build & run manually:
sbt "spark/assembly"
spark-submit --class org.cscie88c.spark.GoldKPIJob --master local[*] spark/target/scala-2.13/spark-assembly-*.jar

Expected runtime: ~1–2 minutes.

Key Metrics Computed:
- Weekly Trip Volume – total trips per borough and week  
- Weekly Revenue – total revenue per week  
- Average Duration & Distance – mean trip duration and distance per week  
- Peak Hour Trip % – share of trips between 7–9 AM and 4–7 PM  
- Night Trip % – share of trips between 10 PM–5 AM  
- Revenue per Mile – total revenue divided by total miles  

Notes:
- Each Gold run is timestamped for validation.
- Downstream job GoldValidationJob.scala compares the two most recent runs and flags anomalies >25% in weekly trip volume.
- Output locations are configurable in the code:
  val GoldRoot = "/opt/spark-data/gold/kpis"
  val SilverRoot = "/opt/spark-data/silver/trips_conformed"

## GoldValidationJob – KPI Change Detection

This Spark job (GoldValidationJob.scala) validates consistency between the two most recent Gold KPI runs. It automatically loads the two latest timestamped runs from /opt/spark-data/gold/kpis/, joins their weekly metrics, and computes the percentage change in trip volume per borough and week. The validation flags unexpected data drift or ETL issues between consecutive Gold outputs.

Inputs:
- data/gold/kpis/run_<timestamp>/weekly_trip_volume from the two most recent runs  

The job determines the runs dynamically:
    Using CURRENT run:  /opt/spark-data/gold/kpis/run_20251106_003747
    Using PREVIOUS run: /opt/spark-data/gold/kpis/run_20251106_003658

Processing Logic:
1. Reads both KPI Parquet datasets (weekly_trip_volume)  
2. Joins them by:
   Seq("pickup_borough", "pickup_week_start")
3. Computes relative change:
   (trip_volume_current - trip_volume_previous) / trip_volume_previous
4. Filters anomalies where |change| > 25%  
5. Writes results to:
   /opt/spark-data/gold/validation/run_<timestamp>/

Output:
Each validation produces a timestamped directory containing a single CSV file of anomalies.

Example output:
    Using CURRENT run:  /opt/spark-data/gold/kpis/run_20251106_003747
    Using PREVIOUS run: /opt/spark-data/gold/kpis/run_20251106_003658
    Validation complete. Output written to:
   /opt/spark-data/gold/validation/run_1762390209489

Sample CSV:
| pickup_week_start | pickup_borough | trip_volume_prev | trip_volume_curr | change_pct |
|-------------------|----------------|------------------|------------------|-------------|
| 2024-12-30        | Manhattan      | 341228           | 419205           | 0.228       |

Run Instructions:
From project root:
sbt "spark/runMain org.cscie88c.spark.GoldValidationJob"

Or manually:
spark-submit --class org.cscie88c.spark.GoldValidationJob --master local[*] spark/target/scala-2.13/spark-assembly-*.jar

Notes:
- The validation step is non-destructive — it only reads and compares.
- Results are saved in a new folder under /opt/spark-data/gold/validation/ for auditing.
- Threshold for anomaly detection is currently hardcoded to 25% change in trip volume.
