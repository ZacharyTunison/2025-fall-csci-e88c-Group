## Programming in Scala for Big Data Systems, Fall 2025
Scala Project for Harvard Extension course CSCI E-88C, Fall, 2025. See course details at [Scala for Big Data](https://courses.dce.harvard.edu/?details&srcdb=202601&crn=16769).


This project is a multi-module setup for Scala applications that integrate with big data frameworks like Spark, Beam, and Kafka. It is designed to facilitate development in a structured manner, allowing for modular code organization and easy dependency management.

The project requires Java 17, Scala 2.13 and sbt 1.9.2+ environment to run.

## Project Structure
- **core**: Contains the core library code shared across other modules.
- **cli**: Implements the command-line interface for the application.
- **spark**: Contains Spark-specific code and configurations.
- **build.sbt**: The main build file for the project, defining the modules and their dependencies.
- **README.md**: This file, providing an overview of the project and its structure.

## Build Tool
This project uses [SBT](https://www.scala-sbt.org/) (Scala Build Tool) for building and managing dependencies. SBT allows for incremental compilation and easy management of multi-module projects.


## NYC Taxi Analytics Dashboard - Quick Start

### Prerequisites

- Node.js (v18 or later)
- Docker and Docker Compose
- Java 17
- sbt 1.9.2+
- python

### Installation

1. **Install project dependencies:**
   ```bash
   npm install
   ```

2. **Install Evidence dependencies:**
   ```bash
   cd evidence
   npm install
   cd ..
   ```

### Running the Pipeline

**Option 1: Automated (Recommended)**

Run the complete pipeline with one command:

```bash
npm run pipeline:full
```

This will:
- Start Docker Spark containers
- Process the taxi data (Silver job)
- Calculate KPIs (Gold job)
- Update the dashboard data
- Start the Evidence development server

**Option 2: Manual Steps**

If you prefer to run each step individually:

1. Start Docker containers:
   ```bash
   npm run docker:up
   ```

2. Run data processing:
   ```bash
   npm buildAndCopyJar
   npm run spark:silver
   npm run spark:gold
   ```

3. Update dashboard database:
   ```bash
   npm run update:duckdb
   ```

4. Start the dashboard:
   ```bash
   npm run dev
   ```

### View the Dashboard

Open your browser to:
```
http://localhost:3000
```

### Stop Everything

Stop Docker containers:
```bash
npm run docker:down
```

Stop Evidence (press Ctrl+C in the terminal running the dev server)

### Troubleshooting

**Dashboard shows no data:**
- Make sure Spark jobs completed successfully
- Run `npm run update:duckdb` to refresh the database
- Restart the Evidence server

**Docker errors:**
- Verify Docker is running: `docker ps`
- Check logs: `docker logs spark-master`

**Build errors:**
- Ensure Java 17 is installed: `java -version`
- Clean build: `sbt clean`
npm 


# Doing things the old way
## Running the Spark Job in Docker
1. First, ensure that your Spark application uberjar file is built. You can build the Spark uberjar file by using the following sbt commands:

   ```bash
   sbt 
   compile
   spark/assembly
   exit
   ```

2. Then, copy the JAR file to the `docker/apps/spark` directory:
   ```bash
   cp spark/target/scala-2.13/SparkJob.jar docker/apps/spark
   ```

3. Copy any data files to the `data` directory. These files are required to be in these locations.
``` 
data/yellow_tripdata_2025-01.parquet
data/taxi_zone_lookup.csv
```


4. Next, start the Docker container:
   ```bash
   docker compose -f docker-compose-spark.yml up -d
   ```
5. Finally, submit the Spark jobs: Note that Silver must run before Gold.
   ```bash
   docker exec -it spark-master /bin/bash
   
   /opt/spark/bin/spark-submit --class org.cscie88c.spark.BronzeJob --master spark://spark-master:7077 /opt/spark-apps/SparkJob.jar
    /opt/spark/bin/spark-submit --class org.cscie88c.spark.SilverJob --master spark://spark-master:7077 /opt/spark-apps/SparkJob.jar
    /opt/spark/bin/spark-submit --class org.cscie88c.spark.GoldJob --master spark://spark-master:7077 /opt/spark-apps/SparkJob.jar
   ```
6. To stop the Docker containers,

   Exit the container shell and run:
   ```bash
   docker compose -f docker-compose-spark.yml down
   ```


## Advanced Troubleshooting (Python, DuckDB, and Evidence Setup)

If the pipeline runs but the dashboard shows  
`Unable to load source manifest` or `ModuleNotFoundError: No module named 'duckdb'`,  
follow these steps:

### 1. Install Python and pip inside WSL
If `pip` is missing:
```bash
sudo apt update
sudo apt install python3-pip -y
```

If virtual environment creation fails:
```bash
sudo apt install python3.12-venv -y
```
(Replace `3.12` with your Python version, e.g., `3.10` or `3.11`.)

### 2. Create and activate a virtual environment
From the project root:
```bash
python3 -m venv .venv
source .venv/bin/activate
```
You’ll know it’s active when your prompt looks like:
```
(.venv) mikha@LAPTOP-NQ6Q29UV:~/Projects/2025-fall-csci-e88c-Group$
```

### 3. Install DuckDB Python package
```bash
pip install duckdb
```

If you see the “externally managed environment” error:
```bash
pip install duckdb --break-system-packages
```
(Use only if not working inside a venv.)

### 4. Rebuild the dashboard database
After fixing Python/DuckDB, recreate your database:
```bash
npm run update:dashboard
```

Expected output:
```
Creating table weekly_trip_volume...
Created table weekly_trip_volume
DuckDB database created at: evidence/sources/taxi_analytics/taxi_analytics.duckdb
```

### 5. Regenerate the Evidence source manifest
From the `evidence` subfolder:
```bash
cd evidence
npx evidence sources add sources/taxi_analytics/taxi_analytics.duckdb
```

Expected:
```
✔ Linked DuckDB source taxi_analytics
```

### 6. Launch the dashboard
```bash
npm run dev
```
Then visit:  
[http://localhost:3000]

---

### Common verification commands

| Check | Command | Expected |
|-------|----------|-----------|
| Confirm DuckDB file | `ls evidence/sources/taxi_analytics/` | Shows `taxi_analytics.duckdb` |
| Check manifest | `cat evidence/sources/sources.json` | Contains one `taxi_analytics` entry |
| Verify Docker running | `docker ps` | Shows `spark-master` and `spark-worker` containers |



## License
Copyright 2025, Edward Sumitra

Licensed under the MIT License.



