BronzeJob - Data Ingestion and Schemas

Overview:
---------
This Spark job (BronzeJob.scala) ingests the data from the raw files and provides summary level information about the 
quality of that data

Inputs:
-------
/opt/spark-data/yellow_tripdata_2025-01.parquet   → Raw trip data  
/opt/spark-data/taxi_zone_lookup.csv              → Taxi zone lookup data

Outputs:
--------
(to stdout)
A sample of the taxi_zone_lookup data
A per column null percentage of the yellow tripdata

Run Instructions:
-----------------
From project root:
sbt "spark/runMain org.cscie88c.spark.BronzeJob"

Or build and run manually:
sbt "spark/assembly"
spark-submit --class org.cscie88c.spark.BronzeJob \
--master local[*] spark/target/scala-2.13/spark-assembly-*.jar

Notes:
------
This Job also includes creating the schema for the files and case classes in 
TaxiZones (for the taxi_zone_lookup.csv file) 
TripData (for the yellow_tripdata_2025-01.parquet file)
