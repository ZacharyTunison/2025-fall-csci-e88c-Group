#!/bin/bash
#This is the 'Application Id', when you go to EMR Studio and create an application
APP_ID="00g0vpa2ivkvlp0d"
#This is the role that the EMR app runs as, see https://us-east-1.console.aws.amazon.com/iam/home?region=us-east-1#/roles 
EMR_ROLE_ID="arn:aws:iam::314892180401:role/service-role/AmazonEMR-ExecutionRole-1762641002417"
#S3 bucket, without the prefix, see https://us-east-1.console.aws.amazon.com/s3/home?region=us-east-1
BUCKET="taxidatabucket-ztharvard"