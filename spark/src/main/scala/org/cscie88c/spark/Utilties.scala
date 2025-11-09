package org.cscie88c.spark

import org.apache.spark.sql.{Column, DataFrame}

import java.nio.file.{Files, Paths}

object Utilities {
  def parquetOutput(dataFrame: DataFrame, fileName: String): Unit = {
    dataFrame
      .coalesce(1)
      .write
      .mode("overwrite")
      .option("compression", "snappy")
      .parquet(fileName)
  }
  def csvOutput(dataFrame: DataFrame, fileName: String, rowLimit: Int, sortExpr: Column): Unit = {
    dataFrame
      .orderBy(sortExpr)
      .limit(rowLimit)
      .coalesce(1)
      .write
      .mode("overwrite")
      .option("header", "true")
      .option("quoteAll", "true")
      .csv(fileName)
  }

  def validateFiles(filePaths: Array[String]): (Boolean, Map[String, String]) = {
    val results = filePaths.map { path =>
      val p = Paths.get(path)
      val errorOpt = if (!Files.exists(p)) {
        Some("File does not exist")
      } else if (!Files.isRegularFile(p)) {
        Some("Path exists but is not a regular file")
      } else if (!Files.isReadable(p)) {
        Some("File exists but is not readable")
      } else {
        None
      }

      (path, errorOpt)
    }
    val invalidFiles = results.flatMap { case (path, errorOpt) =>
      errorOpt match {
        case Some(error) => Some(path -> error)
        case None => None
      }
    }.toMap

    (invalidFiles.isEmpty, invalidFiles)

  }
  def validateArgs(usage: String, args: Array[String]) : Boolean = {
    if (args.isEmpty) {
      println("Error: No file paths provided")
      sys.exit(1)
    }
    if (args.length < 2) {
      println(usage)
      sys.exit(1)
    }
    val (allValid, invalidFiles) = validateFiles(args)

    if (!allValid) {
      println("Invalid files found:")
      invalidFiles.foreach { case (path, error) =>
        println(s"$path: $error")
      }
      sys.exit(1)
    }
    true
  }
}
