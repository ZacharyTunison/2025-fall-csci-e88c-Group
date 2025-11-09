package org.cscie88c.spark

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, count, desc, when}

object BronzeJob {
  def main(args: Array[String]): Unit = {
   Utilities.validateArgs("Usage: SparkJob taxiZoneCSVpath TripDataparquetpath",args)

    // All files are valid, proceed with your application...
    println("All files valid. Continuing...")
    val spark = SparkSession.builder()
      .appName("SampleSparkJob")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val df = TaxiZones.zonesFromFile(args(0))(spark)
    val pq = TripData.loadParquetData(args(1))(spark)
    println("Zone sample")
    df.show(10)
    println("Null percentatages by column:" )
    calculateNullPercentages(pq)(spark).show(100, false)
    spark.stop()
  }


  def calculateNullPercentages(df: DataFrame)(spark: SparkSession): DataFrame = {
    // Calculate counts for all columns in a single pass
    val exprs = df.columns.map { colName =>
      (count(when(col(colName).isNull, true)) / count("*") * 100).alias(s"${colName}_null_pct")
    }

    // Run the aggregation
    val nullCounts = df.select(exprs: _*).first()

    // Transform row to column format
    val result = df.columns.map { colName =>
      (colName, nullCounts.getAs[Double](s"${colName}_null_pct"))
    }

    // Convert to DataFrame
    spark.createDataFrame(result)
      .toDF("column_name", "null_percentage")
      .orderBy(desc("null_percentage"))
  }


}
