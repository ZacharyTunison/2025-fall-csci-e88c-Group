## Programming in Scala for Big Data Systems, Fall 2025 Group Project - Group 6
Scala Project for Harvard Extension course CSCI E-88C, Fall, 2025. 

This Repository holds the code for Group 6's implementation of the group project.

The main branch contains the most proper versions of things

The AWS branch contains the changes needed to make things run on AWS, which uses Scala 2.12. See "For AWS:" below 

For compilation:
```
sbt
compile
spark/assembly
exit
```

Required files:
```
data/taxi_zone_lookup.csv
data/yellow_tripdata_2025-01.parquet
```

To copy the megajar file to the proper location (main branch)

```
cp spark/target/scala-2.13/SparkJob.jar docker/apps/spark
```

Ensure Docker Desktop is running, and start the spark containers:

```
docker compose -f docker-compose-spark.yml up -d
```

Enter the Docker shell:
```
docker exec -it spark-master /bin/bash
```

Run the jobs. Note that Silver must run before Gold.

```
/opt/spark/bin/spark-submit --class org.cscie88c.spark.BronzeJob --master spark://spark-master:7077 /opt/spark-apps/SparkJob.jar
/opt/spark/bin/spark-submit --class org.cscie88c.spark.SilverJob --master spark://spark-master:7077 /opt/spark-apps/SparkJob.jar
/opt/spark/bin/spark-submit --class org.cscie88c.spark.GoldJob --master spark://spark-master:7077 /opt/spark-apps/SparkJob.jar
```

For AWS:
--------

switch to the AWS branch (git checkout AWS), which compiles to scala 2.12 and requires some code changes to allow passing in file paths.
Key changes: 
Bronze, Silver and Gold jobs changed to allow passing in needed paths. 
Removed reject output from silver for simplicity
Scala 2.12 compatibility - downgraded some dependencies, eliminated others
Changed docker container to match AWS as close as possible:  apache/spark:3.5.7-scala2.12-java11-ubuntu


Configure ./scripts/user_conf.sh with your appId, EMR_Role_ID, and S3 Bucket name. 
AWS cli required (or it can be done in EMR studio as well)

Upload the mega jar to $BUCKET/apps/SparkJob.jar (replacing <bucket> with your bucket prefix, in my case "s3://taxidatabucket-ztharvard")

 ```aws s3 cp --sse AES256 spark/target/scala-2.12/SparkJob.jar <bucket>/apps/SparkJob.jar```

Copy your data files (again replacing <bucket> with your bucket prefix)

```
aws s3 cp -sse AES256 data/taxi_zone_lookup.csv <bucket>/data/taxi_zone_lookup.csv
aws s3 cp -sse AES256 data/yellow_tripdata_2025-01.parquet <bucket>/data/taxi_zone_lookup.csv
```

To run each stage, use the shell scripts provided: 
```
./runEmrBronze.sh
./runEmrSilver.sh
./runEmrGold.sh
```


SILVERJOB
---------


Overview:
---------
This Spark job (SilverJob.scala) performs cleaning and transformation of NYC Yellow Taxi trip data.
It applies sequential data quality (DQ) rules, removes invalid and null records, standardizes timestamps
to EST, joins with Taxi Zone Lookup data, and adds weekly bucketing for aggregation readiness.
The job logs detailed metrics, writes both cleaned and conformed outputs, and saves sample CSVs and reject files.

Inputs:
-------
/opt/spark-data/yellow_tripdata_2025-01.parquet   → Raw trip data  
/opt/spark-data/taxi_zone_lookup.csv              → Taxi zone lookup data

Outputs:
--------
/opt/spark-data/silver/trips/                     → Cleaned Parquet (post-DQ)  
/opt/spark-data/silver/trips_conformed/           → Conformed Parquet (joined + EST + weekly)  
/opt/spark-data/silver/rejects_<rule_name>/       → Rejected rows (CSV)  
/opt/spark-data/silver/clean_sample_10k/          → 10K-row cleaned sample (CSV)  
/opt/spark-data/silver/conformed_sample_10k/      → 10K-row conformed sample (CSV)
SilverJob - Trip Data Cleaning

Overview:
---------
This Spark job (SilverJob.scala) cleans NYC Yellow Taxi trip data by applying
multiple data quality (DQ) rules sequentially. It removes invalid or null records,
logs metrics for each step, and writes both cleaned data and rejected rows.

Inputs:
-------
/opt/spark-data/yellow_tripdata_2025-01.parquet   → Raw trip data
/opt/spark-data/taxi_zone_lookup.csv              → Zone info (not used yet)

Outputs:
--------
/opt/spark-data/silver/trips/                     → Final cleaned Parquet
/opt/spark-data/silver/rejects_<rule_name>/       → Rejected rows (CSV)
/opt/spark-data/silver/clean_sample_500k/         → 500K-row sample (CSV)



Applied DQ Rules (in order):
----------------------------
1. no_null_values              → remove any rows with nulls
2. passenger_count_valid       → passenger_count ∈ [1, 8]
3. total_amount_nonnegative    → total_amount ≥ 0
4. chronological_order_valid   → dropoff ≥ pickup
5. duration_reasonable         → trip_duration_min ∈ [1, 180]
6. distance_reasonable         → trip_distance ∈ [0.1, 100]
7. payment_type_valid          → payment_type ∈ [1, 6]
8. ratecode_valid              → RatecodeID ∈ [1, 6]
9. pickup_dropoff_not_null     → ensure timestamps exist
10. no_negative_values         → remove any negative numeric fields

Each rule prints metrics and writes rejects:
=== METRICS: rule_name ===
Total before:  3,475,226
Kept rows:     3,100,000
Rejected rows:   375,226
Rejects written to: /opt/spark-data/silver/rejects_rule_name/

Final Outputs:
--------------
- Cleaned data:  /opt/spark-data/silver/trips/clean_trips_2025_01.parquet
- Sample CSV:    /opt/spark-data/silver/clean_sample_500k/
- Reject files:  /opt/spark-data/silver/rejects_<rule_name>/

Verification Tips:
------------------
- Check Spark logs for metrics at each stage.
- Validate record counts: kept + rejected = total before.
- To preview data:
  spark.read.parquet("/opt/spark-data/silver/trips").show(20, false)
- To inspect rejects:
  head /opt/spark-data/silver/rejects_duration_reasonable/part-*.csv

Notes:
------
- Each run overwrites previous results (repeatable).
- All output paths configurable via SilverRoot variable.
- Modify or add rules using the helper:
  applyDQRule(currentDF, "rule_name", <condition>)

